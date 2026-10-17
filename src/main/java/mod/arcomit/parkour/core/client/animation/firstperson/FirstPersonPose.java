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
				reachX = hand == side ? hand * .42f : hand * .25f;
				reachY = hand == side ? .04f : -.18f + cycle * .07f;
				reachZ = hand == side ? -.42f : -.62f;
			}
			case WALL_CLIMB, CLIMB -> { reachY = .04f + cycle * .16f; reachZ = -.65f - cycle * .08f; }
			case WALL_SLIDE -> { reachX = hand * .43f; reachY = .15f; reachZ = -.58f; }
			// The hanging sensor places the ledge below the eyes, not above the camera.
			case ARMHANG -> { reachY = -.3f + cycle * .045f; reachZ = -.65f; }
			case VAULT -> {
				reachY = hand == side ? -.25f - .38f * stroke : .08f;
				reachX = hand * (hand == side ? .48f : .32f);
				reachZ = -.65f + .4f * progress;
			}
			case ROLL -> { reachX = hand * .12f; reachY = .12f + .16f * stroke; reachZ = -.42f; }
			case SWIM -> { reachX = hand * (.3f + .28f * (cycle + 1)); reachY = -.12f; reachZ += cycle * .26f; }
			case SWIM_BOOST -> { reachX = hand * (.15f + .7f * stroke); reachY = -.08f - .3f * stroke; reachZ = -.8f + .55f * stroke; }
			case WALL_JUMP -> { reachX = hand * .4f; reachY = .2f - .35f * progress; reachZ = -.7f + .45f * progress; }
			case SUPPORT_JUMP -> { reachY = -.25f - .35f * progress; reachZ = -.6f + .35f * progress; }
			case VAULT_JUMP, ENTITY_JUMP, SWIM_JUMP -> { reachY = -.32f + .52f * stroke; reachZ = -.65f; }
		}
		return new Arm(x, y, z, reachX - x, reachY - y, reachZ - z);
	}

	private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
}
