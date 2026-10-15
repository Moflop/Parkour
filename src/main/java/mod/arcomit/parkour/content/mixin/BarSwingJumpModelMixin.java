package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.content.behavior.barswing.client.BarSwingJumpPlayerAnimModifier;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerModel.class)
public class BarSwingJumpModelMixin {
	@ModifyVariable(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private float parkour$recoverWalkSwing(float amount, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
		return entity instanceof Player player ? amount * BarSwingJumpPlayerAnimModifier.walkWeight(player, ageInTicks) : amount;
	}
}
