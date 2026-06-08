package mod.arcomit.parkour.content.behavior.wallrun.client.animation.player;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 墙跑运动的数学计算，独立处理四肢、头部补偿等插值算法。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallRunAnimMath {

	/**
	 * 逐帧计算跑墙动画的所有骨骼数据。
	 *
	 * <p>身体偏航角补偿使视觉朝向与运动方向一致；头部在身体强制转向的基础上
	 * 独立旋转以平滑跟随鼠标视角；四肢以正弦波模拟跑步摆幅，
	 * 靠墙侧手臂做出按压墙面的支撑动作，非靠墙侧手臂正常摆动。
	 *
	 * @param player       目标玩家，不能为 null
	 * @param state        动画内部状态（振幅和相位），不能为 null
	 * @param partialTick  部分 tick 插值因子，范围 [0, 1)
	 * @param isWallOnLeft 墙体是否在玩家左侧，影响手臂/躯干偏转方向
	 * @return 当前帧完整骨骼数据，永远不会为 null
	 */
	public static FrameData calculate(Player player, WallRunAnimState state, float partialTick,
			boolean isWallOnLeft) {
		FrameData data = new FrameData();

		// 【修复核心1】提前获取墙跑的目标偏航角 (targetYaw) 和香草身体偏航角
		float currentVanillaBodyYaw =
				Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		float targetYaw = Direction.from3DDataValue(
				wallMovementData.getRunMoveRaw()).toYRot();

		float yawDiff = Mth.wrapDegrees(targetYaw - currentVanillaBodyYaw);
		data.bodyRotY = yawDiff * Mth.DEG_TO_RAD; // 为 body 保存补偿角

		// 头部计算
		float pitch = Mth.rotLerp(partialTick, player.xRotO, player.getXRot());
		float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.getYHeadRot());
		float netYaw = Mth.wrapDegrees(headYaw - targetYaw); // 【修复核心2】抵消身体的强制旋转

		data.headRotX = pitch * Mth.DEG_TO_RAD;
		data.headRotY = netYaw * Mth.DEG_TO_RAD;
		data.headRotZ = 0f;

		// 躯干和四肢动作计算
		float smoothAmplitude = Mth.lerp(partialTick, state.amplitudeO, state.amplitude);
		if (smoothAmplitude > 0.001f) {
			data.hasMotion = true;
			float smoothPhase =
					Mth.lerp(partialTick, state.animPhaseO, state.animPhase);
			float runSwing = Mth.sin(smoothPhase) * smoothAmplitude * 1.2f;
			float wallArmBob = (float) Math.toRadians(
					-8 * Math.cos(smoothPhase)) * smoothAmplitude;

			data.leftLegRotX = -runSwing;
			data.rightLegRotX = runSwing;

			data.leftArmRotX = isWallOnLeft ? wallArmBob : runSwing;
			data.rightArmRotX = isWallOnLeft ? -runSwing : wallArmBob;

			float yaw = (float) Math.toRadians(
					8 * Math.cos(smoothPhase)) * smoothAmplitude;
			data.torsoRotY = isWallOnLeft ? -yaw : yaw;

			data.bodyPosY = (float) Math.pow(Math.cos(smoothPhase),
					2) * smoothAmplitude * 1.5f;
		}

		return data;
	}

	/**
	 * 环绕最短路径的弧度插值，始终走较小角度方向。
	 *
	 * @param start 起始弧度值
	 * @param end   目标弧度值
	 * @return 按最短路径一步到位的插值结果
	 */
	public static float rotLerpRadians(float start, float end) {
		float diff = end - start;
		while (diff < -(float) Math.PI)
			diff += (float) Math.PI * 2f;
		while (diff >= (float) Math.PI)
			diff -= (float) Math.PI * 2f;
		return start + diff;
	}


	/**
	 * 单帧骨骼数据容器。
	 *
	 * <p>头部旋转由代码完全控制；四肢、躯干、身体上下位移在 {@code hasMotion} 为 true 时有效。
	 */
	public static class FrameData {
		/** 头部绕 X/Y/Z 轴旋转（弧度） */
		public float headRotX, headRotY, headRotZ;
		/** 左右腿绕 X 轴旋转（弧度） */
		public float leftLegRotX, rightLegRotX;
		/** 左右臂绕 X 轴旋转（弧度） */
		public float leftArmRotX, rightArmRotX;
		/** 躯干绕 Y 轴旋转（弧度） */
		public float torsoRotY;
		/** 身体 Y 轴位移，模拟跑步起伏 */
		public float bodyPosY;
		/** 身体绕 Y 轴旋转补偿值（弧度） */
		public float bodyRotY;
		/** 当前是否有有效运动，false 时四肢保持默认姿态 */
		public boolean hasMotion = false;
	}
}
