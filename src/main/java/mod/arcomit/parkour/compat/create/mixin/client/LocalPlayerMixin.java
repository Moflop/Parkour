package mod.arcomit.parkour.compat.create.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.compat.create.CreateMovementCompat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V"))
    private void parkour$sendAfterContraptionCollision(LocalPlayer player, Operation<Void> original) {
        if (!CreateMovementCompat.deferPosition(player)) original.call(player);
    }
}
