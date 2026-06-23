package mod.arcomit.parkour.content.client.handler;

import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.landingroll.client.animation.player.LandingRollPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.speedvault.client.animation.player.SpeedVaultAnimModifier;
import mod.arcomit.parkour.content.client.init.ParkourPlayerAnimations;
import mod.arcomit.parkour.core.client.animation.camera.CameraAnimationRegistry;
import mod.arcomit.parkour.core.client.animation.player.ClientAnimationRegistry;
import mod.arcomit.parkour.core.client.animation.player.v3.ParkourAnimLayer;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.proxy.client.*;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * 客户端初始化处理器。
 *
 * <p>在客户端启动阶段完成以下一次性初始化工作：
 * <ol>
 *   <li>创建各代理（输入、音效、动画、相机、本地玩家服务）的客户端实现实例</li>
 *   <li>注册一次性动作动画（翻滚、速过），通过 Action Modifier 播放固定帧数的过场动画</li>
 *   <li>注册相机动画重载监听器</li>
 * </ol>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientParkourSetupHandler {
	public static final Identifier PARKOUR_ANIM_LAYER_ID = ParkourMod.prefix("parkour_anim_layer");
	private static final int PARKOUR_ANIM_LAYER_PRIORITY = 2000;

	/**
	 * 客户端启动时执行一次性初始化。
	 *
	 * <p>依次：
	 * <ol>
	 *   <li>创建各代理的客户端实现实例</li>
	 *   <li>通过 {@code enqueueWork} 在主线安全地注册动画层和一次性动作动画（Action Modifier）</li>
	 * </ol>
	 *
	 * @param event 客户端启动事件，不为 null
	 */
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		ParkourProxies.INPUT_PROXY = new ClientInputProxyImpl();
		ParkourProxies.SOUND_PROXY = new ClientSoundProxyImpl();
		ParkourProxies.PLAYER_ANIM_PROXY = new ClientPlayerAnimProxyImpl();
		ParkourProxies.CAMERA_PROXY = new ClientCameraAnimProxyImpl();
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY =
				new ClientLocalPlayerServicesProxyImpl();
		ParkourProxies.MINECRAFT_PROXY = new ClientMinecraftProxyImpl();
		ParkourProxies.GET_CLIENT_CONFIG_PROXY = new ClientGetClientConfigProxyImpl();

		event.enqueueWork(() -> {
			PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
					PARKOUR_ANIM_LAYER_ID, PARKOUR_ANIM_LAYER_PRIORITY,
					ParkourAnimLayer::new
			);

			// 落地翻滚（一次性动画，18tick总长，最后6tick过渡融合）
			ClientAnimationRegistry.registerActionModifier(
					ParkourPlayerAnimations.LANDING_ROLL.id, player -> {
						return new LandingRollPlayerAnimModifier(player, 18,
								6);
					});

			ClientAnimationRegistry.registerActionModifier(
					ParkourPlayerAnimations.SPEED_VAULT_LEFT.id, player -> {
						return new SpeedVaultAnimModifier(player, 10);
					});
			ClientAnimationRegistry.registerActionModifier(
					ParkourPlayerAnimations.SPEED_VAULT_RIGHT.id, player -> {
						return new SpeedVaultAnimModifier(player, 10);
					});
		});

	}

	/**
	 * 注册自定义重载监听器，使相机动画 JSON 文件在 /reload 时自动刷新。
	 *
	 * @param event 客户端重载监听器注册事件，不为 null
	 */
	@SubscribeEvent
	public static void onRegisterClientReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(Identifier.withDefaultNamespace("camera_animations"),
				CameraAnimationRegistry.INSTANCE);
	}
}
