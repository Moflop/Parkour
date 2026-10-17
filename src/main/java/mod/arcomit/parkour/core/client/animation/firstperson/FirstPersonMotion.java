package mod.arcomit.parkour.core.client.animation.firstperson;

/** Tick-driven blending, independent of rendering frequency and perspective. */
public final class FirstPersonMotion {
	private FirstPersonAction action = FirstPersonAction.NONE;
	private FirstPersonAction clip = FirstPersonAction.NONE;
	private int remaining, age, side = 1;
	private float phase, weight, previousWeight;
	private FirstPersonPose pose = FirstPersonPose.sample(FirstPersonAction.NONE, 0, 0, 1);
	private FirstPersonPose previous = pose;

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
		if (next != action) age = 0;
		action = next;
		if (remaining > 0) remaining--;
		else side = stateSide;
		phase += Math.min(Math.abs(distance), .65f) * 4;
		weight = Math.clamp(weight + (action == FirstPersonAction.NONE ? -.2f : .4f), 0, 1);
		pose = pose.mix(FirstPersonPose.sample(action, age++, phase, side), .5f);
		if (weight == 0) pose = FirstPersonPose.sample(FirstPersonAction.NONE, 0, 0, 1);
	}

	public FirstPersonAction action() { return action; }
	/** Includes queued clips and the final interpolated fade-out frame. */
	public boolean isActive() { return remaining > 0 || weight > 0 || previousWeight > 0; }
	public float weight(float partial) { return previousWeight + (weight - previousWeight) * partial; }
	public FirstPersonPose pose(float partial) { return previous.mix(pose, partial); }
	public void reset() {
		action = clip = FirstPersonAction.NONE;
		remaining = age = 0;
		phase = weight = previousWeight = 0;
		previous = pose = FirstPersonPose.sample(FirstPersonAction.NONE, 0, 0, 1);
	}
}
