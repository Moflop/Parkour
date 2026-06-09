package mod.arcomit.parkour.content.behavior.armhang.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.context.InputData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 悬挂状态下禁用原版前后移动输入。
 * <p>
 * 原版的前进和左右移动在悬挂时被清零，改为由 {@link ClientArmhangMovement} 接管水平移动逻辑。左右原始输入值在清零前保存到 {@link InputData}，
 * 供悬挂平移计算方向使用。
 * <p>
 * 仅在客户端注册——服务端不需要处理移动输入。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientArmhangDisableMoveHandler {

	/**
	 * 拦截移动输入更新事件：若玩家处于手臂悬挂状态， 清零前进和左右移动，将原始左右输入保存用于悬挂平移方向判定。
	 */
	@SubscribeEvent
	public static void disableMoveWhileArmhanging(MovementInputUpdateEvent event) {
		Player player = event.getEntity();
		StateData stateData = ParkourContext.get(player).state();
		IParkourState currentState = stateData.getState();
		if (!(currentState instanceof ArmhangState)) {
			return;
		}

		Input input = event.getInput();
		input.forwardImpulse = 0;
		// 传入原左右移动Input用于确定移动方向
		InputData inputData = ParkourContext.get(player).input();
		inputData.setLeftImpulse(input.leftImpulse);
		input.leftImpulse = 0;
	}
}
