package mod.arcomit.parkour.content.action.speedvaultjump.client.handler;

import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonAction;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.speedvaultjump.SpeedVaultJumpAction;
import mod.arcomit.parkour.content.action.speedvaultjump.network.SpeedVaultJumpC2SPayload;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 速过跳跃客户端输入处理器——再次按下跳跃键时立即起跳，并通知服务端同步执行。
 *
 * <p>仅响应按下瞬间，长按和键盘重复事件不会触发。键盘和鼠标均使用当前跳跃键绑定，
 * 以最高优先级处理，避免同一次输入继续触发蹬墙跳。</p>
 *
 * @author Mitok
 * @since 2026-10-14
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSpeedVaultJumpHandler {

	/**
	 * 监听跳跃键按下事件，兼容键码和扫描码绑定。
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onKeyAction(InputEvent.Key event) {
		Minecraft mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS && mc.options.keyJump.matches(
				event.getKey(), event.getScanCode())) {
			tryJump(mc);
		}
	}

	/**
	 * 监听鼠标跳跃绑定，仅响应按下瞬间。
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseAction(InputEvent.MouseButton.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS && mc.options.keyJump.matchesMouse(
				event.getButton())) {
			tryJump(mc);
		}
	}

	/**
	 * 本地预测速过跳跃，成功后先同步位置，再发送动作请求。
	 *
	 * <p>界面打开、游戏暂停、窗口失去焦点或按住潜行键时不处理输入。
	 * 跳跃资格由 {@link SpeedVaultJumpAction#execute} 统一检查。</p>
	 */
	private static void tryJump(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.screen != null || mc.isPaused() || !mc.isWindowActive()) {
			return;
		}
		if (mc.options.keyShift.isDown() || ParkourProxies.INPUT_PROXY.getShiftKeyDown(player)) {
			return;
		}

		if (SpeedVaultJumpAction.execute(player)) {
			FirstPersonParkourRenderer.play(FirstPersonAction.VAULT_JUMP);
			player.sendPosition();
			PacketDistributor.sendToServer(new SpeedVaultJumpC2SPayload());
		}
	}
}
