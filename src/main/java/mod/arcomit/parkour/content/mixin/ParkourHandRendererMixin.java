package mod.arcomit.parkour.content.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reserve the custom arm pass before PAL and item mods dispatch their hand renders. */
@Mixin(value = ItemInHandRenderer.class, priority = 1100)
public abstract class ParkourHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void parkour$renderAnimatedHands(float partialTick, PoseStack stack, MultiBufferSource.BufferSource buffer,
			LocalPlayer player, int packedLight, CallbackInfo ci) {
		if (FirstPersonParkourRenderer.renderHands(partialTick, stack, buffer, player, packedLight)) ci.cancel();
	}
}
