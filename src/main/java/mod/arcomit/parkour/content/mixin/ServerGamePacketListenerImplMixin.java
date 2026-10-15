package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 移除速度限制反作弊的Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

	/** 站在真实实体顶部属于有效支撑，不能被原版的纯方块悬空检测踢出。 */
	@ModifyReturnValue(method = "noBlocksAround", at = @At("RETURN"))
	private boolean parkour$entityGround(boolean original, Entity entity) {
		return original && !(entity instanceof Player player && EntitySupport.findSupport(player) != null);
	}

	@WrapOperation(
			method = { "handleMovePlayer", "handleMoveVehicle" },
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z")
	)
	private boolean bypassAntiCheat(ServerGamePacketListenerImpl instance, Operation<Boolean> original) {
		// 强制告诉服务器“该玩家是房主”，无视后续的位移惩罚
		if (ParkourConfig.removeSpeedLimits) {
			return true;
		}
		return original.call(instance);
	}
}
