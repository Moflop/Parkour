package mod.arcomit.parkour.core.sensor.client.debug.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.sensor.client.debug.SensorDebugType;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import mod.arcomit.parkour.core.sensor.impl.WallJumpSensor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * 客户端调试渲染处理器，在世界的半透明实体渲染阶段之后将传感器碰撞盒绘制为彩色线框。
 * <p>
 * 根据 {@link #DEBUG_TYPE} 的当前值决定渲染哪种传感器的碰撞盒：
 * <ul>
 *   <li>{@code WALL_SLIDE} -- 四个水平方向的 HeadFeet 碰撞盒</li>
 *   <li>{@code WALL_RUN} -- 玩家左右两侧的 HeadFeet 碰撞盒</li>
 *   <li>{@code WALL_CLIMB} -- 玩家面向方向的 HeadFeet 碰撞盒</li>
 *   <li>{@code WALL_JUMP} -- 四个水平方向的 WallJump 碰撞盒</li>
 *   <li>{@code ARMHANG_EYE} -- 玩家面向方向的 ArmhangEye 碰撞盒</li>
 *   <li>{@code ARMHANG_TOP} -- 玩家面向方向的 ArmhangTop 碰撞盒</li>
 *   <li>{@code NONE} -- 不渲染任何碰撞盒</li>
 * </ul>
 * 红色线框表示该方向碰撞成立，绿色表示未碰撞。渲染使用平滑插值位置以消除画面抖动。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class DebugSensorRenderHandler {

	/** 四个水平方向常量，避免重复创建列表 */
	private static final List<Direction> HORIZONTALS =
			List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

	/**
	 * 当前调试渲染类型，由{@code /parkour debug} 指令动态切换。
	 * 设为{@link SensorDebugType#NONE} 时不渲染任何碰撞盒。
	 */
	public static SensorDebugType DEBUG_TYPE = SensorDebugType.NONE;

	/**
	 * 注册世界渲染事件，在实体渲染之后绘制传感器碰撞盒线框。
	 * <p>使用平滑插值位置（{@link net.minecraft.util.Mth#lerp}）消除高帧率下的画面抖动，
	 * 红色表示碰撞成立，绿色表示未碰撞。批量渲染结束后提交 {@link RenderType#lines()} 批次。</p>
	 */
	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;
		if (DEBUG_TYPE == SensorDebugType.NONE)
			return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

		// 平滑插值消除高帧率下的画面抖动
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		double smoothX = Mth.lerp(partialTick, player.xo, player.getX());
		double smoothY = Mth.lerp(partialTick, player.yo, player.getY());
		double smoothZ = Mth.lerp(partialTick, player.zo, player.getZ());
		Vec3 logicPos = player.position();
		double offsetX = smoothX - logicPos.x;
		double offsetY = smoothY - logicPos.y;
		double offsetZ = smoothZ - logicPos.z;

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

		CollisionChecker checker;
		BoxProvider provider;
		List<Direction> directions;

		switch (DEBUG_TYPE) {
			case WALL_SLIDE:
				checker = HeadFeetSensor::isValidCollision;
				provider = HeadFeetSensor::getBoxes;
				directions = HORIZONTALS;
				break;
			case WALL_RUN: {
				Direction facing = player.getDirection();
				checker = HeadFeetSensor::isValidCollision;
				provider = HeadFeetSensor::getBoxes;
				directions = List.of(facing.getCounterClockWise(),
						facing.getClockWise());
				break;
			}
			case WALL_CLIMB:
				checker = HeadFeetSensor::isValidCollision;
				provider = HeadFeetSensor::getBoxes;
				directions = List.of(player.getDirection());
				break;
			case WALL_JUMP:
				checker = WallJumpSensor::isValidCollision;
				provider = WallJumpSensor::getBoxes;
				directions = HORIZONTALS;
				break;
			case ARMHANG_EYE:
				checker = ArmhangEyeSensor::isValidCollision;
				provider = ArmhangEyeSensor::getBoxes;
				directions = List.of(player.getDirection());
				break;
			case ARMHANG_TOP:
				checker = ArmhangTopSensor::isValidCollision;
				provider = ArmhangTopSensor::getBoxes;
				directions = List.of(player.getDirection());
				break;
			default:
				poseStack.popPose();
				return;
		}

		for (Direction dir : directions) {
			renderDirectionBoxes(player, dir, checker, provider, poseStack,
					vertexConsumer, offsetX, offsetY, offsetZ);
		}

		bufferSource.endBatch(RenderType.lines());

		poseStack.popPose();
	}

	/**
	 * 对单个方向进行碰撞检测并渲染所有相关 AABB 线框。
	 *
	 * @param player       目标玩家，不能为 null
	 * @param direction    要检测的方向，不能为 null
	 * @param checker      碰撞检测方法引用
	 * @param provider     碰撞盒获取方法引用
	 * @param poseStack    渲染姿态栈
	 * @param vertexConsumer 线条顶点消费者
	 * @param offsetX      平滑渲染 X 偏移量
	 * @param offsetY      平滑渲染 Y 偏移量
	 * @param offsetZ      平滑渲染 Z 偏移量
	 */
	private static void renderDirectionBoxes(Player player, Direction direction,
			CollisionChecker checker, BoxProvider provider, PoseStack poseStack,
			VertexConsumer vertexConsumer, double offsetX, double offsetY,
			double offsetZ) {
		boolean colliding = checker.test(player, direction);
		// 红色=碰撞，绿色=通畅
		float r = colliding ? 1.0F : 0.0F;
		float g = colliding ? 0.0F : 1.0F;
		float b = 0.0F;

		for (AABB box : provider.get(player, direction)) {
			AABB smoothBox = box.move(offsetX, offsetY, offsetZ);
			LevelRenderer.renderLineBox(poseStack, vertexConsumer, smoothBox.minX,
					smoothBox.minY, smoothBox.minZ, smoothBox.maxX,
					smoothBox.maxY, smoothBox.maxZ, r, g, b, 1.0F);
		}
	}

	/**
	 * 碰撞检测函数式接口
	 */
	@FunctionalInterface
	private interface CollisionChecker {
		boolean test(Player player, Direction direction);
	}


	/**
	 * 碰撞箱获取函数式接口
	 */
	@FunctionalInterface
	private interface BoxProvider {
		List<AABB> get(Player player, Direction direction);
	}
}
