package mod.arcomit.parkour.content.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 防止攻击时打断疾跑的Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

	@Redirect(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
	private void preventSprintInvalid(Player instance, boolean isSprinting) {
		// 留空：不执行 setSprinting(false)，从而不打断疾跑
	}
}
