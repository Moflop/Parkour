package mod.arcomit.parkour.compat.sable.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.arcomit.parkour.compat.sable.client.SablePlayerLightCompat;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sable's precise final light pass must start with unshadowed world skylight. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

	@WrapOperation(method = "getPackedLightCoords",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getSkyLightLevel(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)I"))
	private int parkour$sampleWorldSkyBeforePreciseShipShadow(EntityRenderer<?> renderer, Entity entity,
			BlockPos vanillaBlock, Operation<Integer> original, @Local(argsOnly = true) float partialTick) {
		BlockPos renderBlock = SablePlayerLightCompat.worldSkyProbe(entity, vanillaBlock, partialTick);
		return renderBlock == null ? original.call(renderer, entity, vanillaBlock)
				: entity.level().getBrightness(LightLayer.SKY, renderBlock);
	}
}
