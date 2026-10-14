package mod.arcomit.parkour.content.behavior.wallclimb.client.animation.player;

import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.client.animation.player.SurfaceAnimationMath;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 负责墙攀的身体锁死偏航角代数计算。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallClimbAnimMath {

	/**
	 * 计算爬墙时身体偏航角的补偿值。
	 *
	 * <p>原版引擎会根据玩家鼠标输入平滑旋转身体（{@code yBodyRot}），
	 * 但爬墙时玩家的绝对面朝方向由墙体方向决定。这里计算两者差值， 供动画修改器补偿，使视觉身体朝向与墙体法线方向一致。
	 *
	 * @param player      目标玩家，不能为 null
	 * @param partialTick 当前帧的部分 tick 插值因子，范围 [0, 1)
	 * @return 当前帧骨骼数据，包含身体偏航角差值（弧度），永远不会为 null
	 */
	public static FrameData calculate(Player player, float partialTick) {
		FrameData data = new FrameData();

		Direction wall = ParkourContext.get(player).wall().getClimb();
		if (wall == null) wall = player.getDirection();
		float targetYaw = Directions.horizontalYaw(
				DynamicSurfaceMotion.renderWallNormal(player, wall, partialTick));
		data.bodyYawDiff = SurfaceAnimationMath.bodyYawRadians(targetYaw,
				player.yBodyRotO, player.yBodyRot, partialTick);

		return data;
	}

	/**
	 * 单帧骨骼数据，目前仅包含身体偏航角补偿值。
	 */
	public static class FrameData {
		/** 身体绕 Y 轴需要额外旋转的弧度值 */
		public float bodyYawDiff;
	}
}
