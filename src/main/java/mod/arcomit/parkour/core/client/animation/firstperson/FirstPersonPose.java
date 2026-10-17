package mod.arcomit.parkour.core.client.animation.firstperson;

/** Camera-space shoulder and reach direction, in blocks. No world/player transforms are changed. */
public record FirstPersonPose(Arm left, Arm right) {
	public record Arm(float x, float y, float z, float dx, float dy, float dz) {
		public Arm mix(Arm to, float t) {
			return new Arm(lerp(x, to.x, t), lerp(y, to.y, t), lerp(z, to.z, t),
					lerp(dx, to.dx, t), lerp(dy, to.dy, t), lerp(dz, to.dz, t));
		}

	}

	public FirstPersonPose mix(FirstPersonPose to, float t) {
		return new FirstPersonPose(left.mix(to.left, t), right.mix(to.right, t));
	}

	public static FirstPersonPose sample(FirstPersonAction action, float age, float phase, int side) {
		return new FirstPersonPose(arm(action, age, phase, side, -1), arm(action, age, phase, side, 1));
	}

	private static Arm arm(FirstPersonAction action, float age, float phase, int side, int hand) {
		float cycle = (float) Math.sin(phase + (hand < 0 ? Math.PI : 0));
		float progress = action.duration == 0 ? 0 : Math.clamp(age / action.duration, 0, 1);
		float stroke = (float) Math.sin(progress * Math.PI);
		float x = hand * .42f, y = -.43f, z = .16f;
		float reachX = hand * .3f, reachY = -.18f, reachZ = -.65f;
		switch (action) {
			case NONE -> { y = -1.25f; reachY = -1.1f; }
			case CRAWL -> { y = -.48f; reachY = -.32f + cycle * .08f; reachZ += cycle * .18f; }
			case SLIDE -> {
				reachX = hand * .52f; reachY = hand == side ? -.5f : -.12f;
				reachZ = hand == side ? -.42f : -.65f;
			}
			case DODGE -> { reachX = hand * (.22f + .18f * stroke); reachY = .06f; reachZ = -.48f; }
			case WALL_RUN -> {
				if (hand == side) {
					reachX = hand * .42f;
					reachY = -.02f + cycle * .025f;
					reachZ = -.43f;
				} else {
					// Swing around a low shoulder: forward into the lower corner of
					// the view, then down and behind it. Mirror the same arc on either wall.
					float swing = (float) Math.sin(phase);
					float angle = -.50f - 1.20f * swing;
					y = -.38f;
					reachX = x - hand * .08f;
					reachY = y - .78f * (float) Math.cos(angle);
					reachZ = z + .78f * (float) Math.sin(angle);
				}
			}
			case WALL_CLIMB -> { reachY = .04f + cycle * .24f; reachZ = -.65f - cycle * .08f; }
			case CLIMB -> { reachY = .04f + cycle * .16f; reachZ = -.65f - cycle * .08f; }
			case WALL_SLIDE -> {
				if (hand == side) {
					// Use the wall run's planted support pose without a walking bob.
					reachX = hand * .42f;
					reachY = -.02f;
					reachZ = -.43f;
				} else {
					// Keep the free hand low and still; a rear-facing slide relaxes both hands.
					reachX = hand * .28f;
					reachY = -.42f;
					reachZ = -.64f;
				}
			}
			// The hanging sensor places the ledge below the eyes, not above the camera.
			case ARMHANG -> {
				if (side == 0) {
					// Facing the ledge: both palms track the same below-eye grip line.
					reachX = hand * .34f;
					reachY = -.06f + cycle * .035f;
					reachZ = -.48f + cycle * .06f;
				} else if (hand == side) {
					// Keep the support hand toward the ledge, farther out at the edge of the view.
					reachX = hand * .70f;
					reachY = -.08f + cycle * .025f;
					reachZ = -.62f;
				} else {
					// Bring the released hand closer to the camera and toward the centre.
					reachX = hand * .26f;
					reachY = .10f + cycle * .10f;
					reachZ = -.40f + cycle * .05f;
				}
			}
			case VAULT -> {
				// Speed-vault hands follow the Dying Light-like vault: the support
				// hand sweeps back while the active hand reaches over the obstacle.
				reachY = hand == side ? -.28f - .32f * stroke : .06f + cycle * .10f;
				reachX = hand * (hand == side ? .52f : .30f);
				reachZ = hand == side ? -.60f + .30f * progress : -.68f + cycle * .12f;
			}
			case ROLL -> { reachX = hand * .12f; reachY = .12f + .16f * stroke; reachZ = -.42f; }
			case SWIM -> { reachX = hand * (.3f + .28f * (cycle + 1)); reachY = -.12f; reachZ += cycle * .26f; }
			case SWIM_BOOST -> {
				// Keep the original outward stroke's speed and reach its off-screen
				// endpoint on the last sampled tick. Never reverse back toward the centre.
				float pushProgress = Math.clamp(age / (action.duration - 1), 0, 1);
				float push = (float) Math.sin(pushProgress * Math.PI * .5);
				reachX = hand * (.15f + .7f * push);
				reachY = -.08f - .3f * push;
				reachZ = -.8f + .55f * push;
			}
			case WALL_JUMP -> { reachX = hand * .4f; reachY = .2f - .35f * progress; reachZ = -.7f + .45f * progress; }
			case WALL_JUMP_UP -> {
				boolean wallHand = side != 0 && hand == side;
				reachX = hand * (wallHand ? .46f : .32f);
				reachY = (wallHand ? -.02f : -.16f) + (wallHand ? .48f : .34f) * progress;
				reachZ = wallHand ? -.48f : -.66f;
			}
			case SUPPORT_JUMP -> { reachY = -.25f - .35f * progress; reachZ = -.6f + .35f * progress; }
			case SUPPORT_JUMP_UP -> {
				// Match the vault jump's synchronized lift, reaching higher past the ledge.
				reachX = hand * .3f;
				reachY = -.32f + .82f * stroke;
				reachZ = -.65f;
			}
			case VAULT_JUMP, ENTITY_JUMP, SWIM_JUMP -> { reachY = -.32f + .52f * stroke; reachZ = -.65f; }
			case BAR_SWING_JUMP -> {
				// Release from the bar with both hands pulling down, then opening out.
				reachX = hand * (.30f + .12f * stroke);
				reachY = -.02f + .42f * stroke;
				reachZ = -.52f + .20f * progress;
			}
		}
		return new Arm(x, y, z, reachX - x, reachY - y, reachZ - z);
	}

	private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
}
