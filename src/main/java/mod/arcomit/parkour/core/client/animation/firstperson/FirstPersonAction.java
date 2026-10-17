package mod.arcomit.parkour.core.client.animation.firstperson;

/** Local presentation only; timed clips are started after successful gameplay actions. */
public enum FirstPersonAction {
	NONE(0), CRAWL(0), SLIDE(0), DODGE(8), WALL_RUN(0), WALL_CLIMB(0),
	// Roll releases after 8 ticks; the 5-tick lowering finishes before the camera rises at tick 14.
	WALL_SLIDE(0), ARMHANG(0), VAULT(10), ROLL(8), CLIMB(0), SWIM(0),
	WALL_JUMP(10), WALL_JUMP_UP(10), SUPPORT_JUMP(12), SUPPORT_JUMP_UP(8), VAULT_JUMP(8), ENTITY_JUMP(10),
	BAR_SWING_JUMP(12),
	// Stop at the outward stroke's peak, without the return half of the old 14-tick clip.
	SWIM_BOOST(8), SWIM_JUMP(10);

	public final int duration;
	FirstPersonAction(int duration) { this.duration = duration; }
}
