package mod.arcomit.parkour.content.behavior.speedvault;

import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 负责 Speed Vault 状态下的物理受力与运动控制逻辑。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SpeedVaultPhysics {

	public static final int MAX_VAULT_TICK = 2;
	private static final double FORWARD_VELOCITY = 0.45;
	/** 障碍物高度额外余量（单位：格），确保垂直速度能跨过障碍物顶部 */
	private static final double OBSTACLE_CLEARANCE = 0.02;
	/** 标准玩家身高（单位：格），用于根据当前身高比例缩放垂直速度 */
	private static final double PLAYER_STANDARD_HEIGHT = 1.8;
	/** 翻越过程中的水平移动速度倍率 */
	private static final double VAULT_HORIZONTAL_SPEED = 0.1;

	/**
	 * 计算并施加翻越位移：垂直速度根据障碍物高度和玩家身高自适应计算，
	 * 确保在 {@value #MAX_VAULT_TICK} tick 内能跨过障碍物；
	 * 水平速度取 WASD 输入方向的 {@value #VAULT_HORIZONTAL_SPEED} 倍慢速位移。
	 *
	 * @param player  目标玩家，不能为 null，速度直接被覆写
	 * @param wallData 墙体运动数据，包含已探测的障碍物高度，不能为 null
	 */
	public static void applyVaultMovement(Player player, WallMovementData wallData) {
		double obstacleHeight = wallData.getObstaclesHeight();
		Vec3 moveDir = getInputMoveDirection(player);

		double verticalVel =
				((obstacleHeight + OBSTACLE_CLEARANCE) / MAX_VAULT_TICK) / (player.getBbHeight() / PLAYER_STANDARD_HEIGHT);
		Vec3 horizontalVel = moveDir.scale(VAULT_HORIZONTAL_SPEED);

		player.setDeltaMovement(horizontalVel.x, verticalVel, horizontalVel.z);
	}

	/**
	 * 退出速过时施加向前的水平初速度（{@value #FORWARD_VELOCITY} 倍输入方向），
	 * 垂直速度设为 0 交由重力接管自然下落。
	 *
	 * @param player 目标玩家，不能为 null，速度直接被覆写
	 */
	public static void applyExitVelocity(Player player) {
		Vec3 moveDir = getInputMoveDirection(player);
		// 让重力接管垂直速度
		player.setDeltaMovement(moveDir.x * FORWARD_VELOCITY, 0,
				moveDir.z * FORWARD_VELOCITY);
	}

	/**
	 * 将 WASD 输入转换为世界坐标下的水平方向单位向量。
	 *
	 * <p>W/S 控制前后（{@code zza}），A/D 控制左右（{@code xxa}），
	 * 结合玩家 Yaw 角旋转后归一化。若无任何按键输入，退回使用视线方向。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 水平面单位向量，不包含 Y 轴分量
	 */
	private static Vec3 getInputMoveDirection(Player player) {
		float forward = player.zza; // W/S，正值向前
		float strafe = player.xxa; // A/D，正值向右

		if (forward != 0 || strafe != 0) {
			// 将相对移动方向转为世界方向
			float yaw = player.getYRot() * ((float) Math.PI / 180F);
			float sin = Mth.sin(yaw);
			float cos = Mth.cos(yaw);
			double worldX = strafe * cos - forward * sin;
			double worldZ = forward * cos + strafe * sin;
			return new Vec3(worldX, 0, worldZ).normalize();
		} else {
			// 没有按键时保持原来的视线方向作为后备
			Vec3 look = player.getLookAngle();
			return new Vec3(look.x, 0, look.z).normalize();
		}
	}
}
