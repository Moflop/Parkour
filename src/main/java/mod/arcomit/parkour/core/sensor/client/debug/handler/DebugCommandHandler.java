package mod.arcomit.parkour.core.sensor.client.debug.handler;


import com.mojang.brigadier.CommandDispatcher;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.sensor.client.debug.SensorDebugType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * 客户端调试指令处理器，注册 {@code /parkour debug <类型>} 命令系列，
 * 用于在游戏中切换传感器碰撞盒的可视化渲染目标。
 * <p>
 * 支持的子命令对应 {@link SensorDebugType} 的每种值，切换后
 * {@link DebugSensorRenderHandler#DEBUG_TYPE} 将被更新，仅渲染对应类型的传感器碰撞盒。
 * 使用 {@code /parkour debug nothing} 关闭所有渲染。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class DebugCommandHandler {

	/**
	 * 注册 {@code /parkour debug <子命令>} 客户端指令树，每个子命令切换
	 * {@link DebugSensorRenderHandler#DEBUG_TYPE} 到对应的{@link SensorDebugType}值。
	 * <p>在客户端指令注册事件中触发，仅客户端生效，不影响服务端。</p>
	 */
	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		dispatcher.register(Commands.literal("parkour").then(Commands.literal("debug")
				// /parkour debug wall_slide
				.then(Commands.literal("wall_slide").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.WALL_SLIDE;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY WALL SLIDE"));
					return 1;
				}))
				// /parkour debug wall_run
				.then(Commands.literal("wall_run").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.WALL_RUN;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY WALL RUN"));
					return 1;
				}))
				// /parkour debug wall_climb
				.then(Commands.literal("wall_climb").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.WALL_CLIMB;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY WALL CLIMB"));
					return 1;
				}))
				// /parkour debug wall_jump
				.then(Commands.literal("wall_jump").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.WALL_JUMP;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY WALL JUMP"));
					return 1;
				}))
				// /parkour debug armhang_eye
				.then(Commands.literal("armhang_eye").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.ARMHANG_EYE;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY ARMHANG EYE"));
					return 1;
				}))
				// /parkour debug armhang_top
				.then(Commands.literal("armhang_top").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE =
							SensorDebugType.ARMHANG_TOP;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: ONLY ARMHANG TOP"));
					return 1;
				}))
				// /parkour debug nothing
				.then(Commands.literal("nothing").executes(context -> {
					DebugSensorRenderHandler.DEBUG_TYPE = SensorDebugType.NONE;
					context.getSource().sendSystemMessage(Component.literal(
							"§e[Parkour] §aSensor Debug: HIDDEN ALL"));
					return 1;
				}))));
	}
}
