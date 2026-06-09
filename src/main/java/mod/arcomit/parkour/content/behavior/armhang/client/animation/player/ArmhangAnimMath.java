package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 负责垂挂动画的骨骼旋转与位置数学计算。每一帧仅需调用一次计算，生成该帧所有骨骼的偏移数据。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangAnimMath {

	/**
	 * 根据当前动画状态和玩家朝向，计算一帧内所有相关骨骼的旋转和平移偏移量。
	 * <p>
	 * 计算流程：
	 * <ol>
	 * <li>头部：独立于身体旋转，取玩家相机朝向并扣除身体转身分量</li>
	 * <li>身体：Y 轴旋转对齐悬挂方向，叠加侧身观察时的扭转角</li>
	 * <li>手臂：一侧下垂（模拟抓墙悬挂），另一侧跟随移动摆动</li>
	 * <li>身体 Y 轴弹跳：随移动相位上下起伏</li>
	 * </ol>
	 * 同一帧只调用一次，结果缓存于 {@link FrameData} 中供所有骨骼复用。
	 *
	 * @param player      目标玩家，不可为 null
	 * @param state       动画状态机，提供前帧/当前帧的插值权重和摆动相位，不可为 null
	 * @param partialTick 渲染部分 tick 值，范围 [0, 1)，用于平滑动画
	 * @param targetYaw   悬挂方向对应的 Yaw 角度（度），身体将向此方向对齐
	 * @return 包含所有骨骼偏移的帧数据，不会为 null
	 */
	public static FrameData calculate(Player player, ArmhangAnimState state, float partialTick,
			float targetYaw) {
		FrameData data = new FrameData();

		float smoothLookAway =
				Mth.lerp(partialTick, state.lookAwayWeightO, state.lookAwayWeight);
		float bodyTurnAngle =
				smoothLookAway * state.currentLookAwaySign * 90.0f * Mth.DEG_TO_RAD;

		// 头部独立修正
		float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		float cameraYaw = Mth.lerp(partialTick, player.yRotO, player.getYRot());
		float netYaw = Mth.wrapDegrees(cameraYaw - targetYaw);
		data.headRotX = pitch * Mth.DEG_TO_RAD;
		data.headRotY = netYaw * Mth.DEG_TO_RAD - bodyTurnAngle;

		// 身体旋转逻辑
		float currentVanillaBodyYaw =
				Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float yawDiff = Mth.wrapDegrees(targetYaw - currentVanillaBodyYaw);
		data.bodyRotY = yawDiff * Mth.DEG_TO_RAD + bodyTurnAngle;

		// 手部及身体弹跳逻辑
		float moveWeight = 1.0f - smoothLookAway;
		float smoothPhase = Mth.lerp(partialTick, state.animPhaseO, state.animPhase);
		float smoothAmplitude = Mth.lerp(partialTick, state.amplitudeO, state.amplitude);

		float armSwingX = Mth.cos(smoothPhase) * smoothAmplitude * 0.15f * moveWeight;
		float armSwingZ =
				(Mth.sin(smoothPhase) * 0.25f - 0.15f) * smoothAmplitude * moveWeight;

		// 下垂时的角度 (180度，即 PI)
		float dropArmRotX = 135.0f * Mth.DEG_TO_RAD * smoothLookAway;
		float dropArmRotZ = 15.0f * Mth.DEG_TO_RAD * smoothLookAway;

		// 身体弹跳 (Bounce)
		data.bodyPosY =
				(1.0f - Mth.cos(smoothPhase * 2.0f)) * 0.5f * smoothAmplitude * 1.2f * moveWeight;

		// 左手计算
		data.leftArmRotX = armSwingX;
		data.leftArmRotZ = -armSwingZ;
		data.leftArmRotY = 0f;
		if (state.currentLookAwaySign > 0) {
			data.leftArmRotY = -bodyTurnAngle;
		} else {
			data.leftArmRotX += dropArmRotX;
			data.leftArmRotZ -= dropArmRotZ;
		}

		// 右手计算
		data.rightArmRotX = armSwingX;
		data.rightArmRotZ = armSwingZ;
		data.rightArmRotY = 0f;
		if (state.currentLookAwaySign < 0) {
			data.rightArmRotY = -bodyTurnAngle;
		} else {
			data.rightArmRotX += dropArmRotX;
			data.rightArmRotZ += dropArmRotZ;
		}

		return data;
	}


	/**
	 * 用于存储某一帧中所有受影响骨骼的变换数据，避免逐个骨骼渲染时重复计算插值。
	 */
	public static class FrameData {
		public float headRotX, headRotY;
		public float bodyRotY, bodyPosY;
		public float leftArmRotX, leftArmRotY, leftArmRotZ;
		public float rightArmRotX, rightArmRotY, rightArmRotZ;
	}
}
