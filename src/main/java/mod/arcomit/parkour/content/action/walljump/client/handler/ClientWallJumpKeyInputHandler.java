package mod.arcomit.parkour.content.action.walljump.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.walljump.WallJumpAction;
import mod.arcomit.parkour.content.action.walljump.network.WallJumpC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;


/**
 * 蹬墙跳客户端输入处理器——监听键盘/鼠标的跳跃键按下事件， 在客户端侧执行蹬墙跳动作并将位置和事件同步到服务端。
 *
 * <p>仅客户端注册（{@code @OnlyIn(Dist.CLIENT)}），监听键盘和鼠标两个
 * 输入通道，合并到统一的处理逻辑中。内置150ms冷却防连点。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientWallJumpKeyInputHandler {

	/** 两次跳跃之间的最小间隔（毫秒），防止连续按跳跃键频繁触发 */
	private static final long JUMP_COOLDOWN_MS = 150;
	/** 最近一次有效跳跃按下的时间戳（毫秒），用于冷却判定 */
	private static long lastJumpPressMs = 0;

	/**
	 * 监听键盘按键事件，将动作和键码转发到统一处理逻辑。
	 *
	 * @param event 键盘输入事件
	 */
	@SubscribeEvent
	public static void onKeyAction(InputEvent.Key event) {
		handleKeyAction(event.getAction(), event.getKey());
	}

	/**
	 * 监听鼠标按键事件（弹起后），将动作和按钮码转发到统一处理逻辑。
	 *
	 * @param event 鼠标按键事件
	 */
	@SubscribeEvent
	public static void onMouseAction(InputEvent.MouseButton.Post event) {
		handleKeyAction(event.getAction(), event.getButton());
	}

	/**
	 * 统一输入处理入口。
	 *
	 * <p>前置条件（任一不满足则静默返回）：
	 * <ol>
	 *   <li>玩家存在且无GUI界面打开</li>
	 *   <li>输入动作为按下（PRESS）</li>
	 *   <li>按下的键等于跳跃键绑定</li>
	 *   <li>配置中蹬墙跳功能已启用</li>
	 *   <li>距上次跳跃已超过冷却时间</li>
	 * </ol>
	 * </p>
	 *
	 * @param action   输入动作类型，{@code InputConstants.PRESS}表示按下
	 * @param inputKey 按键/按钮的整型码
	 * @sideeffect 执行蹬墙跳动作（修改玩家速度）
	 * @sideeffect 发送客户端位置到服务端
	 * @sideeffect 发送 WallJumpC2SPayload 网络包到服务端
	 */
	private static void handleKeyAction(int action, int inputKey) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.screen != null) {
			return;
		}
		if (!isJumpPress(mc, action, inputKey)) {
			return;
		}
		if (!ParkourConfig.enableWallJump) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastJumpPressMs < JUMP_COOLDOWN_MS) {
			return;
		}
		lastJumpPressMs = now;

		WallJumpAction.execute(player);
		if (player.isLocalPlayer()) {
			player.sendPosition();
			PacketDistributor.sendToServer(new WallJumpC2SPayload());
		}
	}

	/**
	 * 判断输入事件是否为跳跃键按下。
	 *
	 * @param mc       Minecraft客户端实例，不可为null
	 * @param action   输入动作类型，必须是PRESS才返回true
	 * @param inputKey 按键码，必须等于当前绑定的跳跃键才返回true
	 * @return true表示用户按下了跳跃键
	 */
	private static boolean isJumpPress(Minecraft mc, int action, int inputKey) {
		if (action != InputConstants.PRESS) {
			return false;
		}
		if (inputKey != mc.options.keyJump.getKey().getValue()) {
			return false;
		}

		return true;
	}
}
