package mod.arcomit.parkour.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.compat.create.CreateCorrectionCollision;
import mod.arcomit.parkour.compat.create.CreateMovementCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // Create skips ServerPlayer collision, but its motion packets still feed vanilla travel.
    // Do not intercept the separate movement performed when handling position packets.
    @WrapOperation(method = {"travel", "handleRelativeFrictionAndCalculateMovement"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void parkour$clipServerTravel(LivingEntity entity, MoverType type, Vec3 motion,
            Operation<Void> original) {
        if (entity instanceof ServerPlayer player && CreateMovementCompat.isEnabled(player)) {
            motion = CreateCorrectionCollision.clip(player, motion);
        }
        original.call(entity, type, motion);
    }
}
