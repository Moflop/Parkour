package mod.arcomit.parkour.core.client.animation.firstperson;

/** Local presentation only; timed clips are started after successful gameplay actions. */
public enum FirstPersonAction {
	NONE(0), CRAWL(0), SLIDE(0), DODGE(8), WALL_RUN(0), WALL_CLIMB(0),
	WALL_SLIDE(0), ARMHANG(0), VAULT(10), ROLL(18), CLIMB(0), SWIM(0),
	WALL_JUMP(10), SUPPORT_JUMP(12), VAULT_JUMP(10), ENTITY_JUMP(10),
	SWIM_BOOST(14), SWIM_JUMP(10);

	public final int duration;
	FirstPersonAction(int duration) { this.duration = duration; }
}
