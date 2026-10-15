package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.content.behavior.barswing.BarSwingView;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityViewMixin {
	@Inject(method = "turn", at = @At("TAIL"))
	private void parkour$limitBarSwingView(double yaw, double pitch, CallbackInfo ci) {
		if ((Object) this instanceof Player player) BarSwingView.clamp(player);
	}
}
