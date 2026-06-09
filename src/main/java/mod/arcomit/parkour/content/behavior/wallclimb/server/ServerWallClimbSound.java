package mod.arcomit.parkour.content.behavior.wallclimb.server;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.utils.BlockCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端爬墙脚步声播放器 —— 根据玩家移动距离和面前墙体材质循环播放脚步声。
 *
 * <p>模仿原版行走脚步声的距离触发机制：累计移动距离超过阈值时播放一次脚步声，
 * 并读取墙体方块对应的音效材质（石头、木头等）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerWallClimbSound {
	/** 移动距离放大系数，使脚步声触发频率匹配视觉移动感 */
	private static final float SOUND_DISTANCE_MULTIPLIER = 1.2F;
	/** 音量缩放系数，爬墙脚步声略小于行走 */
	private static final float SOUND_VOLUME_MULTIPLIER = 0.15F;

	/**
	 * 以玩家视线高度位置为基准，获取面朝墙体方块的材质音效并播放脚步声。
	 *
	 * <p>仅在服务端执行。当玩家当前 tick 位移量低于零阈值或玩家头顶有方块时跳过，
	 * 否则按放大系数累计移动距离，每超过下一触发阈值时播一次脚步声。 墙体方块为空气时静默跳过。
	 *
	 * @param player           目标玩家，不能为 null，副作用：修改其 moveDist 和 nextStep
	 * @param wallMovementData 墙体运动数据，记录当前攀爬方向，不能为 null
	 */
	public static void playFootstepSound(Player player, WallMovementData wallMovementData) {
		Level level = player.level();
		// 必须在服务端执行
		if (level.isClientSide()) {
			return;
		}
		Direction wallDirection = wallMovementData.getClimb();
		Vec3 velocity = player.getDeltaMovement();
		// 使用速度向量的长度作为移动距离
		float distanceMovedThisTick = (float) velocity.length();

		if (distanceMovedThisTick < ParkourConstants.ZERO_THRESHOLD) {
			return;
		}

		if (BlockCollisions.isBlockedTowards(player, Direction.UP,
				ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)) {
			return;
		}

		player.moveDist += distanceMovedThisTick * SOUND_DISTANCE_MULTIPLIER;

		if (player.moveDist <= player.nextStep) {
			return;
		}
		player.nextStep = player.moveDist + 1.0F;

		BlockPos playerPos = BlockPos.containing(player.getX(),
				player.getY() + player.getEyeHeight(), player.getZ());
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
