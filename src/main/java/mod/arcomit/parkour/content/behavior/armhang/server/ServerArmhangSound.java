package mod.arcomit.parkour.content.behavior.armhang.server;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端悬挂移动音效。
 * <p>
 * 纯逻辑系统：从 WallMovementData 中读取上帧状态，计算实际位移后触发音效， 并写回数据。避免了使用外部 Map 导致的数据分散问题。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerArmhangSound {
	private static final double ZERO_THRESHOLD = 1.0E-7;
	private static final float SOUND_DISTANCE_MULTIPLIER = 0.6F;
	private static final float SOUND_VOLUME_MULTIPLIER = 0.15F;

	public static void playMovementSound(Player player, WallMovementData wallMovementData) {
		Level level = player.level();
		if (level.isClientSide()) {
			return;
		}

		Direction wallDirection = wallMovementData.getArmhang();
		Vec3 lastPos = wallMovementData.getArmhangLastPos();

		if (wallDirection == null || lastPos == null) {
			return;
		}

		// 1. 获取当前坐标并计算真实的 tick 间距差
		Vec3 currentPos = player.position();
		double dx = currentPos.x - lastPos.x;
		double dy = currentPos.y - lastPos.y;
		double dz = currentPos.z - lastPos.z;
		float distanceMovedThisTick = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

		// 刷新上一帧坐标供下个 tick 使用
		wallMovementData.setArmhangLastPos(currentPos);

		if (distanceMovedThisTick < ZERO_THRESHOLD) {
			return;
		}

		// 2. 累加步数
		float currentDist = wallMovementData.getArmhangMoveDist();
		currentDist += distanceMovedThisTick * SOUND_DISTANCE_MULTIPLIER;

		if (currentDist < 1.0F) {
			wallMovementData.setArmhangMoveDist(currentDist);
			return;
		}

		wallMovementData.setArmhangMoveDist(currentDist - 1.0F);

		// 3. 寻找方块并广播音效
		BlockPos playerPos = BlockPos.containing(player.getX(),
				player.getY() + player.getEyeHeight(), player.getZ());

		if (ArmhangEyeSensor.isValidCollision(player, wallDirection)) {
			double height = player.getBbHeight();
			playerPos = BlockPos.containing(player.getX(),
					player.getY() + player.getEyeHeight() - (height * ParkourConstants.ARMHANG_GRIP_HEIGHT_RATIO),
					player.getZ());
		} else if (ArmhangTopSensor.isValidCollision(player, wallDirection)) {
			double height = player.getBbHeight();
			playerPos = BlockPos.containing(player.getX(),
					player.getY() + height - (height * ParkourConstants.ARMHANG_GRIP_HEIGHT_RATIO),
					player.getZ());
		}

		BlockPos wallPos = playerPos.relative(wallDirection);
		BlockState blockState = level.getBlockState(wallPos);

		if (blockState.isAir()) {
			return;
		}

		SoundType soundType = blockState.getSoundType(level, wallPos, player);
		float volume = soundType.getVolume() * SOUND_VOLUME_MULTIPLIER;
		float pitch = soundType.getPitch();

		level.playSound(null, wallPos.getX(), wallPos.getY(), wallPos.getZ(),
				soundType.getStepSound(), SoundSource.PLAYERS, volume, pitch);
	}
}
