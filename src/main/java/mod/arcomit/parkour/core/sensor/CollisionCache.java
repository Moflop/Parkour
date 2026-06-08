package mod.arcomit.parkour.core.sensor;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 碰撞检测结果缓存，以 tick 和玩家位置为双维度判断缓存有效性。
 * <p>
 * 当 tick 或位置任一过期时，传感器实现会重新计算碰撞箱并覆盖写入缓存。
 * 对外暴露的碰撞箱列表为不可变视图，防止外部误改。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class CollisionCache {
	/** tick未初始化的哨兵值，{@value}表示缓存从未计算过 */
	private static final long TICK_UNINITIALIZED = -1L;

	@Getter
	@Setter
	private long tick = TICK_UNINITIALIZED;
	@Getter
	@Setter
	private Vec3 position;
	private List<AABB> collisionBoxes;
	@Getter
	@Setter
	private boolean collided;

	/**
	 * 返回碰撞盒列表的不可变视图。
	 *
	 * @return 不可修改的 AABB 列表；如果尚未计算或已重置下游清空，则返回 null
	 */
	public List<AABB> getCollisionBoxes() {
		return collisionBoxes == null ? null : Collections.unmodifiableList(collisionBoxes);
	}

	/**
	 * 设置碰撞盒列表，内部做防御性拷贝以防止外部修改影响缓存。
	 *
	 * @param boxes 碰撞盒列表，可为 null（表示清空）
	 */
	public void setCollisionBoxes(List<AABB> boxes) {
		this.collisionBoxes = boxes == null ? null : new ArrayList<>(boxes);
	}

	/**
	 * 判断缓存 tick 是否与当前 tick 一致。
	 *
	 * @param tick 当前游戏 tick 计数，来自 {@code player.tickCount}
	 * @return true 表示缓存仍在同一 tick 内有效
	 */
	public boolean isValidTick(long tick) {
		return this.tick == tick;
	}

	/**
	 * 判断缓存位置是否与玩家当前位置一致。
	 *
	 * @param currentPosition 玩家当前位置，不能为 null
	 * @return false 表示位置已过期（或从未设置），需要重新计算碰撞
	 */
	public boolean isValidPosition(Vec3 currentPosition) {
		if (this.position == null) {
			return false;
		}
		return this.position.equals(currentPosition);
	}
}
