package mod.arcomit.parkour.content.behavior.wallslide.client.animation.player;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 负责墙壁滑行骨骼旋转与位置的数学计算。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlideAnimMath {

	/**
	 * 逐帧计算滑墙动画的所有骨骼数据。
	 *
	 * <p>头部独立跟随鼠标视角旋转；手臂根据玩家与墙面的相对角度计算两种姿态：
	 * 面朝墙时做出双手扶墙动作（手臂上举、侧倾），背对墙时做出自然下垂姿态，
	 * 两姿态通过 {@code backWeight} 平滑过渡；身体 Y 轴正弦起伏模拟下落微动。
	 *
	 * @param player      目标玩家，不能为 null
	 * @param state       动画状态机，提供背对权重等数据，不能为 null
	 * @param partialTick 部分 tick 插值因子，范围 [0, 1)
	 * @return 当前帧骨骼数据，墙壁方向无效时返回不含墙体姿态的默认数据
	 */
	public static FrameData calculate(Player player, WallSlideAnimState state,
			float partialTick) {
		FrameData data = new FrameData();
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);

		// 头部通用运算
		float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
		float netYaw = Mth.wrapDegrees(headYaw - bodyYaw);

		data.headRotX = pitch * Mth.DEG_TO_RAD;
		data.headRotY = netYaw * Mth.DEG_TO_RAD;

		// 检查墙面方向
		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		int dirIndex = wallMovementData.getSlideRaw();
		if (dirIndex < 0 || dirIndex > 5)
			return data;

		Direction wallDir = Direction.from3DDataValue(dirIndex);
		if (wallDir == null)
			return data;

		data.hasWallPose = true;

		// 计算手臂和身体数据
		Vec3 intoWallVec = new Vec3(-wallDir.getStepX(), 0, -wallDir.getStepZ());
		float targetWorldYaw = (float) Math.toDegrees(
				Math.atan2(-intoWallVec.x, intoWallVec.z)) + 180f;
		float yawToWall = Mth.wrapDegrees(targetWorldYaw - bodyYaw);

		boolean isWallOnRight = yawToWall > 0;
		float backWeight = Mth.lerp(partialTick, state.prevBackWeight,
				state.currentBackWeight);

		// --- 姿态 1: 侧身/面朝墙 ---
		float frontLocalYaw = Mth.clamp(yawToWall * 0.3f, -35f, 35f) * Mth.DEG_TO_RAD;
		float FRONT_PITCH = (float) Math.toRadians(-155);
		float FRONT_LEAN = (float) Math.toRadians(12);

		float frontRightArmRotX = isWallOnRight ? FRONT_PITCH : 0.2f;
		float frontRightArmRotY = isWallOnRight ? frontLocalYaw : 0f;
		float frontRightArmRotZ = isWallOnRight ? FRONT_LEAN : 0.2f;

		float frontLeftArmRotX = !isWallOnRight ? FRONT_PITCH : 0.2f;
		float frontLeftArmRotY = !isWallOnRight ? frontLocalYaw : 0f;
		float frontLeftArmRotZ = !isWallOnRight ? -FRONT_LEAN : -0.2f;

		// --- 姿态 2: 背对墙 ---
		float BACK_PITCH = 0.5f, BACK_YAW = 0.3f, BACK_ROLL = 0.2f;
		float backRightArmRotX = BACK_PITCH, backRightArmRotY = BACK_YAW, backRightArmRotZ = BACK_ROLL;
		float backLeftArmRotX = BACK_PITCH, backLeftArmRotY = -BACK_YAW, backLeftArmRotZ = -BACK_ROLL;

		// --- 丝滑应用插值保存到 FrameData ---
		data.rightArmRotX = Mth.lerp(backWeight, frontRightArmRotX, backRightArmRotX);
		data.rightArmRotY = Mth.lerp(backWeight, frontRightArmRotY, backRightArmRotY);
		data.rightArmRotZ = Mth.lerp(backWeight, frontRightArmRotZ, backRightArmRotZ);

		data.leftArmRotX = Mth.lerp(backWeight, frontLeftArmRotX, backLeftArmRotX);
		data.leftArmRotY = Mth.lerp(backWeight, frontLeftArmRotY, backLeftArmRotY);
		data.leftArmRotZ = Mth.lerp(backWeight, frontLeftArmRotZ, backLeftArmRotZ);

		float smoothTick = player.tickCount + partialTick;
		data.bodyPosY = Mth.sin(smoothTick * 1.5f) * 0.2f;

		return data;
	}


	/**
	 * 单帧骨骼数据容器。
	 *
	 * <p>头部旋转直接赋值；手臂在 {@code hasWallPose} 为 true 时才应用墙体姿态，
	 * 否则保持原版默认动画。
	 */
	public static class FrameData {
		public float headRotX, headRotY;
		/** 是否检测到有效墙体并计算了贴墙姿态，false 时手臂保持原版 */
		public boolean hasWallPose = false;
		/** 左右臂绕 X/Y/Z 轴旋转（弧度），由面朝/背对墙姿态计算得出 */
		public float rightArmRotX, rightArmRotY, rightArmRotZ;
		public float leftArmRotX, leftArmRotY, leftArmRotZ;
		/** 身体 Y 轴位移，模拟滑落时的上下微动 */
		public float bodyPosY;
	}
}
