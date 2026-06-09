package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 负责维护与更新垂挂动画的独立状态机（如移动振幅、转身权重等）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangAnimState {
	/** 前一帧的移动相位 */
	public float animPhaseO = 0f;
	/** 当前帧的移动相位，用于驱动手臂摆动和身体弹跳 */
	public float animPhase = 0f;
	/** 前一帧的摆动幅度 */
	public float amplitudeO = 0f;
	/** 当前帧的摆动幅度，由移动强度驱动 */
	public float amplitude = 0f;

	/** 前一帧的侧身观察权重 */
	public float lookAwayWeightO = 0f;
	/** 当前帧的侧身观察权重，0 表示正对墙面，1 表示完全侧身 */
	public float lookAwayWeight = 0f;
	/** 侧身方向符号：1.0 向右看，-1.0 向左看，锁定后不会在阈值附近来回翻转 */
	public float currentLookAwaySign = 1.0f;

	/**
	 * 更新一 tick 的动画状态。
	 * <p>
	 * 将当前值保存为前一帧值（用于渲染插值），然后根据玩家视角偏离墙面的角度 计算侧身权重，根据玩家水平移动速度计算摆动相位和幅度。
	 *
	 * @param player 目标玩家，不可为 null
	 */
	public void tick(Player player) {
		this.animPhaseO = this.animPhase;
		this.amplitudeO = this.amplitude;
		this.lookAwayWeightO = this.lookAwayWeight;

		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		Direction armhangDir = wallMovementData.getArmhang();

		if (armhangDir != null) {
			float targetYaw = armhangDir.toYRot();
			float cameraYaw = player.getYRot();
			float relLook = Mth.wrapDegrees(cameraYaw - targetYaw);

			// 当偏离角度大于75度时，触发侧身状态
			if (Math.abs(relLook) > 75.0f) {
				this.lookAwayWeight = Mth.lerp(0.2f, this.lookAwayWeight, 1.0f);
				// 记录是向左看还是向右看，防止在转身过程中抽搐
				this.currentLookAwaySign = relLook > 0 ? 1.0f : -1.0f;
			} else {
				this.lookAwayWeight = Mth.lerp(0.2f, this.lookAwayWeight, 0f);
			}
		}

		// 处理移动动画（仅在面向墙面时产生有效移动）
		double dx = player.getX() - player.xo;
		double dz = player.getZ() - player.zo;
		float movementIntensity = (float) Math.sqrt(dx * dx + dz * dz) * 10.0f;

		if (movementIntensity > 0.05f && this.lookAwayWeight < 0.5f) {
			this.animPhase += 0.6f;
			this.amplitude = Mth.lerp(0.2f, this.amplitude, 1.0f);
		} else {
			this.amplitude = Mth.lerp(0.2f, this.amplitude, 0f);
		}
	}
}
