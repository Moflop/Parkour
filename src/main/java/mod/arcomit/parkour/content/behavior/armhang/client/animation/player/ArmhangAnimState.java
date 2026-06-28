package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 负责维护与更新垂挂动画的独立状态机（如移动振幅、转身权重等）。
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

	// === 新增字段：用于平滑悬挂角度过渡 ===
	public float armhangYawO = 0f;
	public float armhangYaw = 0f;
	private boolean firstTick = true;

	public void tick(Player player) {
		this.animPhaseO = this.animPhase;
		this.amplitudeO = this.amplitude;
		this.lookAwayWeightO = this.lookAwayWeight;
		this.armhangYawO = this.armhangYaw;

		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		Direction armhangDir = wallMovementData.getArmhang();

		if (armhangDir != null) {
			// 1. 获取绝对的目标墙面角度
			float targetYaw = armhangDir.toYRot();
			float cameraYaw = player.getYRot();

			float targetRelLook = Mth.wrapDegrees(cameraYaw - targetYaw);

			// 侧身判定 (曲线：0.2f Lerp)
			if (Math.abs(targetRelLook) > 75.0f) {
				this.lookAwayWeight = Mth.lerp(0.2f, this.lookAwayWeight, 1.0f);
				this.currentLookAwaySign = targetRelLook > 0 ? 1.0f : -1.0f;
			} else {
				this.lookAwayWeight = Mth.lerp(0.2f, this.lookAwayWeight, 0f);
			}

			// 2. 躯干整体朝向
			if (this.firstTick) {
				this.armhangYaw = targetYaw;
				this.armhangYawO = targetYaw;
				this.firstTick = false;
			} else {
				// === 关键修复：弃用 approachDegrees，使用与 lookAwayWeight 完全相同的 0.2f 插值权重 ===
				// Mth.rotLerp 可以处理角度环绕问题，确保它与侧身补偿完美相互抵消
				this.armhangYaw = Mth.rotLerp(0.2f, this.armhangYaw, targetYaw);
			}

		} else {
			this.firstTick = true;
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
