package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.ParkourConfig;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 移除速度限制反作弊的Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

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
