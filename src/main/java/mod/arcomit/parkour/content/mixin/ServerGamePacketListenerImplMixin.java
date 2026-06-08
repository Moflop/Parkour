package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.ParkourConfig;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
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

	@ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F))
	private float modifyConstantPlayerMaxSpeed(float speed) {
		if (ParkourConfig.removeSpeedLimits) {
			return Float.MAX_VALUE;
		}
		return speed;
	}

	@ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 300.0F))
	private float modifyConstantElytraMaxSpeed(float speed) {
		if (ParkourConfig.removeSpeedLimits) {
			return Float.MAX_VALUE;
		}
		return speed;
	}

	@ModifyConstant(method = "handleMovePlayer", constant = @Constant(doubleValue = 0.0625))
	private double modifyConstantMovedWrong(double speed) {
		if (ParkourConfig.removeSpeedLimits) {
			return Double.MAX_VALUE;
		}
		return speed;
	}

	@ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100.0))
	private double modifyConstantVehicleMaxSpeed(double speed) {
		if (ParkourConfig.removeSpeedLimits) {
			return Double.MAX_VALUE;
		}
		return speed;
	}

	@ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 0.0625))
	private double modifyConstantVehicleMovedWrong(double speed) {
		if (ParkourConfig.removeSpeedLimits) {
			return Double.MAX_VALUE;
		}
		return speed;
	}
}
