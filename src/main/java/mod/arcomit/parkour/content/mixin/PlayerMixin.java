package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.content.behavior.crawl.CrawlState;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	@Inject(method = "setForcedPose", at = @At("HEAD"), cancellable = true)
	private void onSetForcedPose(Pose pose, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (ParkourContext.get(player).state().getState() instanceof CrawlState && pose != Pose.SWIMMING) {
			ci.cancel();// 我看谁敢动我的爬行动作，我的爬行动作必须是最高优先级！（霸总音）
		}
	}
}
