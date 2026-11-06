package mod.arcomit.parkour.compat.tacz;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;

/** Loaded on the client only when TACZ is installed. */
public final class TaczFirstPersonCompat {
	private static LocalPlayer trackedPlayer;
	private static boolean parkourAnimationWasPlaying;

	private TaczFirstPersonCompat() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TaczFirstPersonCompat::blockShooting);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TaczFirstPersonCompat::onClientTick);
	}

	public static void stopShooting(LocalPlayer player) {
		// TACZ polls its own binding each tick, independently of vanilla item use.
		// Release an existing hold as well as any clicks queued before the animation.
		ShootKey.SHOOT_KEY.setDown(false);
		while (ShootKey.SHOOT_KEY.consumeClick()) {}
		ShootKey.shootControllerTick(false);

		// Releasing a charged gun can itself fire it, so discard the charge first.
		var data = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
		data.chargeProgress = 0;
		data.isCharging = false;
	}

	private static void blockShooting(GunShootEvent event) {
		if (event.getLogicalSide() != LogicalSide.CLIENT) return;
		var player = Minecraft.getInstance().player;
		if (player == null || event.getShooter() != player
				|| !FirstPersonParkourRenderer.isAnimationPlaying()) return;
		// Also covers rebound keys and controllers that refresh their input after
		// our tick handler. This event runs before TACZ schedules the shot.
		event.setCanceled(true);
		stopShooting(player);
	}

	private static void onClientTick(ClientTickEvent.Pre event) {
		updateAnimationState(Minecraft.getInstance().player);
	}

	public static void beforeHandRender(LocalPlayer player) {
		updateAnimationState(player);
	}

	private static void updateAnimationState(LocalPlayer player) {
		if (player == null) {
			trackedPlayer = null;
			parkourAnimationWasPlaying = false;
			return;
		}
		if (trackedPlayer != player) {
			trackedPlayer = player;
			parkourAnimationWasPlaying = FirstPersonParkourRenderer.isAnimationPlaying();
			return;
		}

		ItemStack heldItem = player.getMainHandItem();

		boolean animationPlaying = FirstPersonParkourRenderer.isAnimationPlaying();
		if (parkourAnimationWasPlaying && !animationPlaying && player.isAlive()) {
			if (IGun.getIGunOrNull(heldItem) != null) {
				restartGunDrawAnimation(heldItem, player);
			}
		}
		parkourAnimationWasPlaying = animationPlaying;
	}

	private static void restartGunDrawAnimation(ItemStack heldItem, LocalPlayer player) {
		var renderer = IClientItemExtensions.of(heldItem.getItem()).getCustomRenderer();
		if (!(renderer instanceof AnimateGeoItemRenderer<?, ?> gunRenderer)) return;
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		gunRenderer.tryInit(heldItem, player, partialTick);
	}
}
