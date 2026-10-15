package mod.arcomit.parkour.compat.create.mixin;

import mod.arcomit.parkour.compat.create.CreateMovementCompat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.ContraptionHandler", remap = false)
public abstract class ContraptionHandlerMixin {
    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private static void parkour$publishFinalPosition(Level level, CallbackInfo ci) {
        if (level.isClientSide()) CreateMovementCompat.flushPosition(level);
    }
}
