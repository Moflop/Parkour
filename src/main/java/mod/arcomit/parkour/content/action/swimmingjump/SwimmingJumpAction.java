package mod.arcomit.parkour.content.action.swimmingjump;

import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 游泳跳跃动作——在水中按跳跃键向上窜出水面。
 *
 * <p>不满足准入条件时静默返回。速度修改保持水平分量不变，
 * 仅将垂直速度设为固定值{@value #SWIMMING_JUMP_VERTICAL_SPEED}（模拟原版水中跳跃力度）。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingJumpAction {
	/** 游泳跳跃的垂直速度（单位：方/秒） */
	private static final double SWIMMING_JUMP_VERTICAL_SPEED = 0.42;

	/**
	 * 执行游泳跳跃，将玩家垂直速度固定为{@value #SWIMMING_JUMP_VERTICAL_SPEED}方/秒。
	 *
	 * @param player 执行跳跃的玩家，不可为null
	 * @sideeffect 直接修改玩家垂直速度（保留水平速度分量）
	 */
	public static void execute(Player player) {
		if (!SwimmingJumpEligibilityChecker.check(player)) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, SWIMMING_JUMP_VERTICAL_SPEED, motion.z);

		JumpData jumpData = ParkourContext.get(player).jump();
		jumpData.setJumped(true);
	}
}
