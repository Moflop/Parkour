package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 防止攻击打断疾跑，并保护跑酷状态绑定的强制姿态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

	@WrapWithCondition(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
	private boolean preventSprintInvalid(Player instance, boolean isSprinting) {
		if (ParkourChecks.isVanillaState(ParkourContext.get(instance))) {
			return isSprinting;
		}
		// 返回 false 阻止 setSprinting 运行，从而不打断疾跑
		return false;
	}

	@Inject(method = "setForcedPose", at = @At("HEAD"), cancellable = true)
	private void onSetForcedPose(Pose pose, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		Pose linkedPose = ParkourContext.get(player).state().getState().getLinkedPose();
		// TACZ 等模组会在未爬行时每 tick 清除强制姿态，使滑铲在低顶下变为
		// 原版爬行并被状态校验中断。动作期间保留当前状态声明的姿态。
		// 状态机先切换状态再设置姿态，因此回到默认状态后仍可正常清除。
		if (linkedPose != null && pose != linkedPose) {
			ci.cancel();
		}
	}
}
