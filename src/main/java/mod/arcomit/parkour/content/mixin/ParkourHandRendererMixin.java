package mod.arcomit.parkour.content.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reserve the custom arm pass before PAL and item mods dispatch their hand renders. */
@Mixin(value = ItemInHandRenderer.class, priority = 1100)
public abstract class ParkourHandRendererMixin {
	@Shadow private float mainHandHeight;
	@Shadow private float oMainHandHeight;
	@Shadow private float offHandHeight;
	@Shadow private float oOffHandHeight;

	@Inject(method = "tick", at = @At("TAIL"))
	private void parkour$keepVanillaHandsLowered(CallbackInfo ci) {
		var mc = Minecraft.getInstance();
		if (mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player
				&& FirstPersonParkourRenderer.isAnimationPlaying()) {
			// Keep both interpolation endpoints down until the animated hands have
			// withdrawn. Vanilla then raises them with its normal item-switch timing.
			mainHandHeight = oMainHandHeight = 0;
			offHandHeight = oOffHandHeight = 0;
		}
	}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void parkour$renderAnimatedHands(float partialTick, PoseStack stack, MultiBufferSource.BufferSource buffer,
			LocalPlayer player, int packedLight, CallbackInfo ci) {
		if (FirstPersonParkourRenderer.renderHands(partialTick, stack, buffer, player, packedLight)) ci.cancel();
	}
}
