package mod.arcomit.parkour.content.behavior.wallslide;

import mod.arcomit.parkour.content.behavior.wallslide.network.BroadcastWallSlideDirS2CPayload;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 负责滑墙状态下的方向切换与跨端同步逻辑。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlideDirection {

	/**
	 * 检测是否有更近的墙体方向可用，若有则更新缓存并在服务端发送 S2C 包同步给追踪该玩家的客户端。
	 *
	 * <p>客户端自身也会执行此逻辑，因此 S2C 包只更新非本地玩家（RemotePlayer）的滑墙方向。
	 *
	 * @param player            目标玩家，不能为 null
	 * @param wallMovementData 墙体数据，方向变化时会被覆写，不能为 null
	 */
	public static void trySwitchCollisionDirection(Player player,
			WallMovementData wallMovementData) {
		Direction wallDir = WallSlideCollision.findAvailableWallDirection(player,
				wallMovementData);

		if (wallDir != null && wallMovementData.getSlide() != wallDir) {
			wallMovementData.setSlide(wallDir);

			// 既然服务端也会同步执行，直接由服务端发送广播包给周围的客户端
			if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
						new BroadcastWallSlideDirS2CPayload(
								serverPlayer.getId(), wallDir));
			}
		}
	}
}
