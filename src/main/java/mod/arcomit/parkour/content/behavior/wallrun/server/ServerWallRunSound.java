package mod.arcomit.parkour.content.behavior.wallrun.server;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端跑墙脚步声播放器 —— 利用玩家脚部位置对应墙体方块材质播放脚步声。
 *
 * <p>与爬墙脚步声类似，基于移动距离阈值触发，但使用玩家脚部高度（blockPosition）
 * 而非眼睛高度来定位墙体方块，且距离放大系数较小（跑墙速度快）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerWallRunSound {
	/** 移动距离放大系数，跑墙时较小以匹配高移速 */
	private static final float SOUND_DISTANCE_MULTIPLIER = 0.1F;
	/** 音量缩放系数 */
	private static final float SOUND_VOLUME_MULTIPLIER = 0.15F;

	/**
	 * 以玩家脚部高度为基准，获取碰撞墙体方块的材质音效并播放脚步声。
	 *
	 * <p>仅在服务端执行。速度低于阈值或墙体方块为空气时跳过。
	 * 按放大系数累计移动距离，超阈值时播放一次，副作用：修改玩家 moveDist 和 nextStep。
	 *
	 * @param player           目标玩家，不能为 null
	 * @param wallMovementData 墙体运动数据，记录碰撞方向，不能为 null
	 */
	public static void playFootstepSound(Player player, WallMovementData wallMovementData) {
		Level level = player.level();
		// 必须在服务端执行
		if (level.isClientSide()) {
			return;
		}
		Direction wallDirection =
				Direction.from3DDataValue(wallMovementData.getRunCollisionRaw());
		Vec3 velocity = player.getDeltaMovement();
		// 使用速度向量的长度作为移动距离
		float distanceMovedThisTick = (float) velocity.length();

		if (distanceMovedThisTick < ParkourConstants.ZERO_THRESHOLD) {
			return;
		}

		float distanceTraveled = distanceMovedThisTick * SOUND_DISTANCE_MULTIPLIER;
		player.moveDist += distanceTraveled;

		if (player.moveDist <= player.nextStep) {
			return;
		}
		player.nextStep = player.moveDist + 1.0F;

		BlockPos playerPos = player.blockPosition();
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
