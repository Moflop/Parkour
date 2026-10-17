package mod.arcomit.parkour.core.client.animation.firstperson;

/** Tick-driven blending, independent of rendering frequency and perspective. */
public final class FirstPersonMotion {
	private static final int LOWERING_TICKS = 5;
	private static final int JUMP_LOWERING_TICKS = 3;
	private static final int WALL_RUN_SLIDE_SETTLE_TICKS = 4;
	private static final float FULL_CYCLE = (float) (Math.PI * 2);
	private static final float WALL_RUN_BACK_SWING_PHASE = (float) (Math.PI * 1.5);
	private static final FirstPersonPose LOWERED = FirstPersonPose.sample(FirstPersonAction.NONE, 0, 0, 1);
	private FirstPersonAction action = FirstPersonAction.NONE;
	private FirstPersonAction clip = FirstPersonAction.NONE;
	private int remaining, age, side = 1;
	private float phase, weight, previousWeight;
	private float wallRunPhaseStep;
	private int wallRunExitSide, wallRunExitSettleTick;
	private float wallRunExitEndPhase;
	private FirstPersonPose pose = LOWERED;
	private FirstPersonPose previous = pose;
	private FirstPersonPose loweringFrom = pose;
	private float loweringWeight;
	private int loweringTicks = LOWERING_TICKS;

	public void play(FirstPersonAction next, int side) {
		if (next.duration <= 0) throw new IllegalArgumentException("Expected a timed action");
		clip = next;
		remaining = next.duration;
		age = 0;
		this.side = side;
	}

	public void tick(FirstPersonAction state, int stateSide, float distance) {
		previous = pose;
		previousWeight = weight;
		// Catching a ledge or wall takes ownership immediately after a jump.
		if (state == FirstPersonAction.ARMHANG || state == FirstPersonAction.WALL_RUN
				|| state == FirstPersonAction.WALL_CLIMB || state == FirstPersonAction.WALL_SLIDE
				|| state == FirstPersonAction.CRAWL || state == FirstPersonAction.SLIDE) remaining = 0;
		FirstPersonAction next = remaining > 0 ? clip : state;
		if (action == FirstPersonAction.SWIM_BOOST && next == FirstPersonAction.NONE) {
			// Both hands have already left the view. Release directly instead of
			// blending them back across the screen toward the lowered pose.
			reset();
			return;
		}
		if (next != action) {
			age = 0;
			wallRunExitSide = 0;
			wallRunExitSettleTick = 0;
			if (action == FirstPersonAction.WALL_RUN && next == FirstPersonAction.WALL_SLIDE
					&& (stateSide == side || stateSide == 0)) {
				// Remember which arm was free and keep its phase moving forward until
				// the next rear turning point, even if sliding has stopped forward travel.
				wallRunExitSide = side;
				phase %= FULL_CYCLE;
				wallRunExitEndPhase = WALL_RUN_BACK_SWING_PHASE;
				if (phase > wallRunExitEndPhase) wallRunExitEndPhase += FULL_CYCLE;
			}
			if (next == FirstPersonAction.WALL_RUN) {
				phase = 0;
				wallRunPhaseStep = 0;
			}
			if (next == FirstPersonAction.NONE) {
				loweringFrom = pose;
				loweringWeight = weight;
				// These two jump gestures should release the view sooner. Capture the
				// outgoing action so later transitions keep their normal exit timing.
				loweringTicks = action == FirstPersonAction.SUPPORT_JUMP_UP
						|| action == FirstPersonAction.VAULT_JUMP ? JUMP_LOWERING_TICKS : LOWERING_TICKS;
			}
		}
		action = next;
		if (remaining > 0) remaining--;
		else side = stateSide;
		// A camera turn can make the old free arm the new support arm.
		if (wallRunExitSide != 0 && side != 0 && side != wallRunExitSide) wallRunExitSide = 0;
		if (action == FirstPersonAction.WALL_RUN) {
			// Smooth speed changes and limit cadence to about 1.75 full swings per second.
			float targetStep = Math.min(Math.abs(distance) * 1.9f, .55f);
			wallRunPhaseStep += (targetStep - wallRunPhaseStep) * .25f;
			phase += wallRunPhaseStep;
		} else if (wallRunExitSide != 0) {
			phase = Math.min(phase + Math.max(wallRunPhaseStep, .25f), wallRunExitEndPhase);
		} else {
			phase += Math.min(Math.abs(distance), .65f) * 4;
		}
		if (action == FirstPersonAction.NONE) {
			// Finish below the camera before releasing the vanilla hand pass. Capture
			// the last pose once so the return eases out instead of snapping at zero weight.
			float progress = Math.min(++age / (float) loweringTicks, 1);
			float eased = progress * progress * (3 - 2 * progress);
			weight = loweringWeight * (1 - progress);
			pose = progress == 1 ? LOWERED : loweringFrom.mix(LOWERED, eased);
		} else {
			weight = Math.min(weight + .4f, 1);
			var target = FirstPersonPose.sample(action, age++, phase, side);
			// Finish the outward stroke before relinquishing the hand pass next tick.
			pose = action == FirstPersonAction.SWIM_BOOST && remaining == 0
					? target : pose.mix(target, .5f);
			if (wallRunExitSide != 0) pose = finishWallRunBackswing(target);
		}
	}

	private FirstPersonPose finishWallRunBackswing(FirstPersonPose slideTarget) {
		boolean freeLeft = wallRunExitSide > 0;
		var runTarget = FirstPersonPose.sample(FirstPersonAction.WALL_RUN, 0, phase, wallRunExitSide);
		var freeArm = freeLeft ? runTarget.left() : runTarget.right();
		if (phase < wallRunExitEndPhase) {
			var previousArm = freeLeft ? previous.left() : previous.right();
			freeArm = previousArm.mix(freeArm, .5f);
		} else if (wallRunExitSettleTick == 0) {
			// Reach the rear endpoint before starting the return to the low slide pose.
			wallRunExitSettleTick = 1;
		} else {
			float progress = Math.min(wallRunExitSettleTick++ / (float) WALL_RUN_SLIDE_SETTLE_TICKS, 1);
			float eased = progress * progress * (3 - 2 * progress);
			freeArm = freeArm.mix(freeLeft ? slideTarget.left() : slideTarget.right(), eased);
			if (progress == 1) wallRunExitSide = 0;
		}
		// Only the free arm completes its swing; the support arm follows wall slide immediately.
		return freeLeft ? new FirstPersonPose(freeArm, pose.right()) : new FirstPersonPose(pose.left(), freeArm);
	}

	public FirstPersonAction action() { return action; }
	/** Includes queued clips and the final interpolated fade-out frame. */
	public boolean isActive() { return remaining > 0 || weight > 0 || previousWeight > 0; }
	public float weight(float partial) { return previousWeight + (weight - previousWeight) * partial; }
	public FirstPersonPose pose(float partial) { return previous.mix(pose, partial); }
	public void reset() {
		action = clip = FirstPersonAction.NONE;
		remaining = age = 0;
		loweringTicks = LOWERING_TICKS;
		wallRunExitSide = wallRunExitSettleTick = 0;
		wallRunExitEndPhase = 0;
		phase = weight = previousWeight = loweringWeight = wallRunPhaseStep = 0;
		previous = pose = loweringFrom = LOWERED;
	}
}
