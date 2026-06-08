package mod.arcomit.parkour.content.behavior.armhang.client;

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

/**
 * 客户端悬挂移动音效。
 * <p>
 * 根据玩家每 tick 实际移动距离累计步伐计数器，达到阈值后在本地播放步骤音效。
 * 音效来源为悬挂方向对应的墙壁方块，确保不同材质（石头、木头等）播放对应脚步声。
 * <p>
 * 仅在客户端执行——服务端不播放，避免双端重复。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientArmhangSound {
	/** 移动距离低于此值不计入步数，防止微小抖动触发音效 */
	private static final double ZERO_THRESHOLD = 1.0E-7;
	/** 步伐距离乘数，大于 1 使音效触发更密集 */
	private static final float SOUND_DISTANCE_MULTIPLIER = 1.2F;
	/** 音量缩放系数，悬挂音效比正常行走轻 */
	private static final float SOUND_VOLUME_MULTIPLIER = 0.15F;

	/**
	 * 按移动距离累计步伐计数器，达到阈值时播放墙壁材质的脚步声。
	 * <p>
	 * 根据当前悬挂检测类型（眼部传感器或顶部传感器）选择不同的脚步判定高度，
	 * 以适配不同方块形状下抓握位置的差异。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param wallMovementData 墙面移动数据，提供当前悬挂方向，不可为 null
	 */
	public static void playMovementSound(Player player, WallMovementData wallMovementData) {
		Level level = player.level();
		// 必须在客户端执行
		if (!level.isClientSide()) {
			return;
		}

		Direction wallDirection = wallMovementData.getArmhang();
		if (wallDirection == null) {
			return;
		}

		// 因为移动是在客户端触发的，我们可以准确获取到平滑的坐标变化
		double dx = player.getX() - player.xo;
		double dy = player.getY() - player.yo;
		double dz = player.getZ() - player.zo;
		float distanceMovedThisTick = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (distanceMovedThisTick < ZERO_THRESHOLD) {
			return;
		}

		player.moveDist += distanceMovedThisTick * SOUND_DISTANCE_MULTIPLIER;

		if (player.moveDist <= player.nextStep) {
			return;
		}
		player.nextStep = player.moveDist + 1.0F;

		// 依然使用眼睛高度判定抓握的墙壁方块
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
					player.getY() + height - (height * ParkourConstants.ARMHANG_GRIP_HEIGHT_RATIO), player.getZ());
		}
		BlockPos wallPos = playerPos.relative(wallDirection);
		BlockState blockState = level.getBlockState(wallPos);

		if (blockState.isAir()) {
			return;
		}

		SoundType soundType = blockState.getSoundType(level, wallPos, player);
		float volume = soundType.getVolume() * SOUND_VOLUME_MULTIPLIER;
		float pitch = soundType.getPitch();

		// 客户端本地播放声音，不会有延迟
		level.playLocalSound(wallPos.getX() + 0.5D, wallPos.getY() + 0.5D,
				wallPos.getZ() + 0.5D, soundType.getStepSound(),
				SoundSource.PLAYERS, volume, pitch, false);
	}
}
