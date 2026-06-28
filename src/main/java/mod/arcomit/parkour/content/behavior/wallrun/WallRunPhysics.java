package mod.arcomit.parkour.content.behavior.wallrun;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.armhang.network.BroadcastArmhangDirS2CPayload;
import mod.arcomit.parkour.content.behavior.wallrun.network.BroadcastWallRunDirS2CPayload;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 负责墙跑状态下的物理受力与运动控制逻辑。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallRunPhysics {
	private static final double VANILLA_SPRINT_SPEED_MULTIPLIER = 2.15; // 原版疾跑的速度倍率

	/**
	 * 进入跑墙时记录碰撞墙体方向和当前面朝方向作为固定运动方向， 后续 tick 中玩家将始终沿此方向移动。
	 *
	 * @param player           目标玩家，不能为 null
	 * @param wallMovementData 墙体数据容器，会被覆写碰撞方向和运动方向，不能为 null
	 */
	public static void setupInitialMovement(Player player, WallMovementData wallMovementData) {
		Direction wallCollisionDir =
				WallRunCollision.findFirstWallCollisionDirection(player);
		wallMovementData.setRunCollision(wallCollisionDir);
		Direction movementDir = player.getDirection();
		wallMovementData.setRunMove(movementDir);

		if (!player.level().isClientSide()) {
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
					new BroadcastWallRunDirS2CPayload(
							player.getId(),
							movementDir));
		}
	}

	/**
	 * 每 tick 施加跑墙物理：以玩家移动速度属性为基准乘以 {@value #VANILLA_SPRINT_SPEED_MULTIPLIER} 倍
	 * 沿固定运动方向移动，同时向碰撞墙体施加吸附力防止飘离。
	 *
	 * @param player           目标玩家，不能为 null，速度直接覆写，位置通过 move 偏移
	 * @param wallMovementData 墙体数据，包含运动方向和碰撞方向，不能为 null
	 */
	public static void applyWallRunMovement(Player player, WallMovementData wallMovementData) {
		Direction movementDir = Direction.from3DDataValue(wallMovementData.getRunMoveRaw());

		Vec3 runDirection = new Vec3(movementDir.getStepX(), 0,
				movementDir.getStepZ()).normalize();

		double attributeSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double targetSpeed = attributeSpeed * VANILLA_SPRINT_SPEED_MULTIPLIER;

		Direction wallCollisionDirection =
				Direction.from3DDataValue(wallMovementData.getRunCollisionRaw());

		if (wallCollisionDirection != null) {
			// 向固定的运动方向移动
			player.setDeltaMovement(runDirection.scale(targetSpeed));

			// 墙面吸附
			Vec3 wallNormal = new Vec3(wallCollisionDirection.getStepX(), 0,
					wallCollisionDirection.getStepZ());
			Vec3 adhesionForce = wallNormal.scale(ParkourConstants.WALL_ADHESION_FORCE);
			boolean wasOnGround = player.onGround();
			player.move(MoverType.PLAYER, adhesionForce);
			if (wasOnGround) {
				// 恢复 onGround 状态，防止由于水平贴墙导致的意外状态结束
				player.setOnGround(true);
			}

			player.resetFallDistance();
		}
	}
}
