package mod.arcomit.parkour.core.client.animation.player.v3;

public enum ParkourAnimType {
	// 显式定义权重，数字越大优先级越高
	STATE(1),
	ACTION(0);

	private final int priority;

	ParkourAnimType(int priority) {
		this.priority = priority;
	}

	/**
	 * 判断当前类型是否能够覆盖目标类型
	 */
	public boolean canOverride(ParkourAnimType currentType) {
		if (currentType == null) {
			return true;
		}
		return this.priority >= currentType.priority;
	}
}
