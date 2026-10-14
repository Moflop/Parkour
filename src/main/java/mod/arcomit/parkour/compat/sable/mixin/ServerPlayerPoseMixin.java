package mod.arcomit.parkour.compat.sable.mixin;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps a temporary server travel pose from being synchronized at the restored position. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayerPoseMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V", shift = At.Shift.AFTER))
	private void parkour$refreshPoseAfterPositionRestore(CallbackInfo ci) {
		if (!ParkourChecks.isVanillaState(ParkourContext.get(player))) {
			// Sable does not resolve server-player travel against its structures. The
			// discarded travel position may therefore select a crawling pose inside
			// a beam. Query native clearance again at the restored, synchronized position.
			player.updatePlayerPose();
		}
	}
}
