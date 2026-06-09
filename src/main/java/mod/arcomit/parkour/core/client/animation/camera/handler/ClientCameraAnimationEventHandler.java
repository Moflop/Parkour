package mod.arcomit.parkour.core.client.animation.camera.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.client.animation.camera.CameraAnimationManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 将摄像机动画的旋转关键帧应用到玩家视角的渲染管线。
 * <p>
 * 仅在播放动画且玩家处于第一人称时生效，通过累加 pitch/yaw/roll 偏移实现视角动效。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientCameraAnimationEventHandler {

	/**
	 * 每帧渲染前计算当前摄像机角度时触发。
	 * <p>
	 * 副效应：推进 {@link CameraAnimationManager} 的时间轴——即使第三人称不应用旋转， 时间仍需流逝以保证切回第一人称时进度正确。
	 * <p>
	 * 旋转偏移应用规则：pitch 正加、yaw 正加、roll 负减（与 Blockbench 的右手坐标系适配）。
	 */
	@SubscribeEvent
	public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
		CameraAnimationManager controller = CameraAnimationManager.INSTANCE;

		// 依然要在每帧推演时间，保证即使切到第三人称，动画时间依然正常流逝
		controller.tick();

		boolean isFirstPerson =
				Minecraft.getInstance().options.getCameraType().isFirstPerson();

		if (controller.isPlaying() && isFirstPerson) {
			float[] rotation = controller.getCurrentRotation();

			event.setPitch(event.getPitch() + rotation[0]);
			event.setYaw(event.getYaw() + rotation[1]);
			event.setRoll(event.getRoll() - rotation[2]);
		}
	}
}
