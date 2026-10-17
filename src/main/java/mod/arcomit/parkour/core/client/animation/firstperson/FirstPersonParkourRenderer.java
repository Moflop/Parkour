package mod.arcomit.parkour.core.client.animation.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import mod.arcomit.parkour.ClientParkourConfig;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.handler.ClientParkourSetupHandler;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.utils.Directions;
import mod.arcomit.parkour.core.client.animation.player.ParkourAnimationController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Camera-space arms for non-bar actions. Isolated models keep PAL's full-body poses untouched. */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public final class FirstPersonParkourRenderer {
	private static final float ARM_DEPTH_OFFSET = .12F;
	private static final FirstPersonMotion MOTION = new FirstPersonMotion();
	private static PlayerModel<AbstractClientPlayer> wide, slim;
	private static AbstractClientPlayer owner;
	private static String previousState = "";

	@SubscribeEvent
	public static void models(EntityRenderersEvent.AddLayers event) {
		wide = new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		slim = new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	public static FirstPersonMotion motion() { return MOTION; }

	public static void play(FirstPersonAction action) { play(action, 1); }
	public static void play(FirstPersonAction action, int side) {
		var player = Minecraft.getInstance().player;
		if (player == null || !enabled(player)) return;
		bind(player);
		MOTION.play(action, side);
		stopInteraction();
	}

	private static void bind(AbstractClientPlayer player) {
		if (owner == player) return;
		owner = player;
		previousState = "";
		MOTION.reset();
	}

	private static boolean enabled(AbstractClientPlayer player) {
		boolean emptyHands = player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
		var state = ParkourContext.get(player).state();
		return (ParkourConfig.enableFirstPersonAnimations || emptyHands)
				&& (ParkourConfig.enableCrawlFirstPersonAnimations || !state.getStateId().getPath().equals("crawl"))
				&& ClientParkourConfig.enableParkour && state.isParkourEnabled() && player.isAlive()
				&& !player.isSpectator() && !player.isPassenger() && !player.isFallFlying()
				&& !player.getAbilities().flying;
	}

	private static boolean palOwnsHands(AbstractClientPlayer player) {
		var layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, ClientParkourSetupHandler.PARKOUR_ANIM_LAYER_ID);
		return layer instanceof ParkourAnimationController controller && controller.isActive()
				&& controller.getFirstPersonMode() != FirstPersonMode.NONE;
	}

	/** The bar controller is independent of the server's non-bar arm animation setting. */
	public static boolean isAnimationPlaying() {
		var player = Minecraft.getInstance().player;
		return player != null && player.isAlive() && !player.isSpectator()
				&& (palOwnsHands(player) || owner == player && enabled(player) && MOTION.isActive());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void blockInteraction(InputEvent.InteractionKeyMappingTriggered event) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null || mc.isPaused() || !isAnimationPlaying()) return;
		event.setCanceled(true);
		event.setSwingHand(false);
		stopInteraction();
	}

	/** Block mouse presses, but let releases reach MouseHandler so its held state is cleared. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void blockMouseInteractions(InputEvent.MouseButton.Pre event) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null || mc.isPaused() || !isAnimationPlaying()) return;
		int button = event.getButton();
		if ((button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)
				|| event.getAction() != GLFW.GLFW_PRESS) return;
		event.setCanceled(true);
		stopInteraction();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void beforeTick(ClientTickEvent.Pre event) {
		stopInteraction();
	}

	private static void stopInteraction() {
		var mc = Minecraft.getInstance();
		if (mc.isPaused() || mc.gameMode == null || !isAnimationPlaying()) return;
		if (mc.gameMode.isDestroying()) mc.gameMode.stopDestroyBlock();
		if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void tick(ClientTickEvent.Post event) {
		var mc = Minecraft.getInstance();
		bind(mc.player);
		if (mc.player == null || !enabled(mc.player) || palOwnsHands(mc.player)) {
			MOTION.reset();
			previousState = "";
			return;
		}
		if (mc.isPaused()) return;
		var player = mc.player;
		var context = ParkourContext.get(player);
		String state = context.state().getStateId().getPath();
		int side = context.state().getAnimVariant() == 0 ? -1 : 1;
		// State variants describe left/right for wall running and sliding. For
		// wall slide and armhang the hand that is actually near the surface depends
		// on the current camera turn, so derive it from the same surface frame used
		// by the third-person modifiers.
		if (state.equals("wall_slide")) side = wallSlideHandSide(player);
		if (state.equals("armhang")) side = armhangHandSide(player);
		if (state.equals("backstep") && !state.equals(previousState)) MOTION.play(FirstPersonAction.DODGE, side);
		previousState = state;
		FirstPersonAction action = switch (state) {
			case "crawl" -> FirstPersonAction.CRAWL;
			case "slide" -> FirstPersonAction.SLIDE;
			case "wall_run" -> FirstPersonAction.WALL_RUN;
			case "wall_climb" -> FirstPersonAction.WALL_CLIMB;
			case "wall_slide" -> FirstPersonAction.WALL_SLIDE;
			case "armhang" -> FirstPersonAction.ARMHANG;
			default -> FirstPersonAction.NONE;
		};
		float distance = (float) player.position().distanceTo(new net.minecraft.world.phys.Vec3(player.xo, player.yo, player.zo));
		MOTION.tick(action, side, distance);
		stopInteraction();
	}

	/** Called before vanilla/PAL/item-mod hand selection and RenderHandEvent dispatch. */
	public static boolean renderHands(float partialTick, PoseStack stack, MultiBufferSource.BufferSource buffer,
			LocalPlayer player, int packedLight) {
		var mc = Minecraft.getInstance();
		if (player == null || player != mc.player || owner != player || !enabled(player) || palOwnsHands(player)
				|| !mc.options.getCameraType().isFirstPerson() || mc.getCameraEntity() != player
				|| mc.options.hideGui || !MOTION.isActive() || wide == null || slim == null) return false;
		// Screens and item input never relinquish rendering ownership while a motion is active.
		var pose = MOTION.pose(partialTick);
		boolean isSlim = player.getSkin().model() == PlayerSkin.Model.SLIM;
		var model = isSlim ? slim : wide;
		try {
			if (MOTION.action() == FirstPersonAction.BAR_SWING_JUMP) {
				renderBodyAndLegs(stack, buffer, packedLight, player, model);
			}
			renderArm(stack, buffer, packedLight, player, model.leftArm, model.leftSleeve, pose.left(), -1, isSlim);
			renderArm(stack, buffer, packedLight, player, model.rightArm, model.rightSleeve, pose.right(), 1, isSlim);
		} finally {
			// The cancelled vanilla method normally flushes the hand buffers.
			buffer.endBatch();
		}
		return true;
	}

	/** Return the side of the arm closest to a wall normal in camera space. */
	public static int cameraSideForSurface(Player player, net.minecraft.world.phys.Vec3 normal) {
		if (player == null || normal == null || normal.horizontalDistanceSqr() < 1.0e-6) return -1;
		var look = player.getViewVector(1.0F);
		var right = look.cross(new net.minecraft.world.phys.Vec3(0, 1, 0));
		if (right.horizontalDistanceSqr() < 1.0e-6) return -1;
		return normal.dot(right) >= 0 ? 1 : -1;
	}

	public static int wallHandSide(Player player, net.minecraft.core.Direction wall) {
		return wall == null ? -1 : cameraSideForSurface(player,
				DynamicSurfaceMotion.renderWallNormal(player, wall, 1.0F));
	}

	private static int armhangHandSide(Player player) {
		var wall = ParkourContext.get(player).wall().getArmhang();
		if (wall == null) return 0;
		float surfaceYaw = Directions.horizontalYaw(DynamicSurfaceMotion.renderWallNormal(player, wall, 1.0F));
		float lookAwayYaw = net.minecraft.util.Mth.wrapDegrees(player.getYRot() - surfaceYaw);
		// Match ArmhangAnimState and ArmhangAnimMath: positive camera-minus-surface
		// yaw keeps the left hand attached (-1); negative yaw keeps the right (+1).
		return Math.abs(lookAwayYaw) > 75.0F ? (lookAwayYaw > 0 ? -1 : 1) : 0;
	}

	private static int wallSlideHandSide(Player player) {
		var wall = ParkourContext.get(player).wall();
		int raw = wall.getSlideRaw();
		if (raw < 0 || raw > 5) return 0;
		var direction = net.minecraft.core.Direction.from3DDataValue(raw);
		if (direction == null) return 0;
		float surfaceYaw = Directions.horizontalYaw(DynamicSurfaceMotion.renderWallNormal(player, direction, 1.0F));
		float relative = net.minecraft.util.Mth.wrapDegrees(surfaceYaw - player.yBodyRot);
		return Math.abs(relative) > 130.0F ? 0 : (relative > 0 ? 1 : -1);
	}

	/** Draw the lower first-person body during bar releases. */
	private static void renderBodyAndLegs(PoseStack stack, MultiBufferSource buffer, int packedLight,
			AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
		var texture = player.getSkin().texture();
		stack.pushPose();
		try {
			// Keep the torso below the camera during bar releases.
			stack.translate(0, -.62F, .30F);
			stack.mulPose(Axis.XP.rotationDegrees(-16F));
			model.body.setPos(0, 0, 0);
			model.body.setRotation(0, 0, 0);
			model.body.render(stack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
			if (player.isModelPartShown(PlayerModelPart.JACKET)) {
				model.jacket.copyFrom(model.body);
				model.jacket.render(stack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY);
			}
		} finally {
			stack.popPose();
		}

		for (int side : new int[]{-1, 1}) {
			stack.pushPose();
			try {
				stack.translate(side * .125F, -.12F, .22F);
				stack.mulPose(Axis.XP.rotationDegrees(-22F + side * 5F));
				ModelPart leg = side < 0 ? model.leftLeg : model.rightLeg;
				ModelPart pants = side < 0 ? model.leftPants : model.rightPants;
				leg.setPos(0, 0, 0);
				leg.setRotation(0, 0, 0);
				leg.render(stack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
				PlayerModelPart part = side < 0 ? PlayerModelPart.LEFT_PANTS_LEG : PlayerModelPart.RIGHT_PANTS_LEG;
				if (player.isModelPartShown(part)) {
					pants.copyFrom(leg);
					pants.render(stack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY);
				}
			} finally {
				stack.popPose();
			}
		}
	}

	private static void renderArm(PoseStack stack, MultiBufferSource buffer, int packedLight,
			AbstractClientPlayer player, ModelPart arm,
			ModelPart sleeve, FirstPersonPose.Arm pose, int side, boolean isSlim) {
		stack.pushPose();
		try {
			// Camera forward is -Z. Shift before rotation so sleeves and held items
			// share the same depth without changing the arm's pose or reach direction.
			stack.translate(pose.x(), pose.y(), pose.z() - ARM_DEPTH_OFFSET);
			Vector3f direction = new Vector3f(pose.dx(), pose.dy(), pose.dz()).normalize();
			Quaternionf armRotation = armRotation(direction);
			stack.mulPose(armRotation);
			// Centre the vanilla cube on its shoulder, retaining its original 12-pixel length.
			stack.translate(side * (isSlim ? .5 : 1) / 16, 2.0 / 16, 0);
			arm.setPos(0, 0, 0);
			arm.setRotation(0, 0, 0);
			sleeve.copyFrom(arm);
			if (!player.isInvisible()) {
				var texture = player.getSkin().texture();
				arm.render(stack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
				if (player.isModelPartShown(side < 0 ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE)) {
					sleeve.render(stack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY);
				}
			}
			renderHeldItem(stack, buffer, packedLight, player, side, isSlim);
		} finally {
			stack.popPose();
		}
	}

	static Quaternionf armRotation(Vector3f direction) {
		Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction);
		Vector3f itemUp = new Vector3f(0, 1, 0).fma(-direction.y, direction);
		if (itemUp.lengthSquared() < 1.0e-6F) {
			itemUp.set(0, 0, -1).fma(direction.z, direction);
		}
		itemUp.normalize();
		Vector3f currentItemUp = rotation.transform(new Vector3f(0, 0, -1));
		float angle = (float) Math.atan2(direction.dot(new Vector3f(currentItemUp).cross(itemUp)),
				currentItemUp.dot(itemUp));
		Quaternionf roll = new Quaternionf().rotationAxis(angle, direction.x, direction.y, direction.z);
		return roll.mul(rotation);
	}

	private static void renderHeldItem(PoseStack stack, MultiBufferSource buffer, int packedLight,
			AbstractClientPlayer player, int side, boolean isSlim) {
		HumanoidArm arm = side < 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		InteractionHand hand = player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		var item = player.getItemInHand(hand);
		if (item.isEmpty()) return;
		// Match PlayerModel.translateToHand and ItemInHandLayer's grip, including slim skins.
		if (isSlim) stack.translate(side * .5F / 16, 0, 0);
		stack.mulPose(Axis.XP.rotationDegrees(-90));
		stack.mulPose(Axis.YP.rotationDegrees(180));
		stack.translate(side / 16.0F, .125F, -.625F);
		// Held-model transforms attach to the animated palm. They also let TACZ render just
		// its gun model, since its first-person renderer otherwise draws its own pair of arms.
		Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, item,
				arm == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
				arm == HumanoidArm.LEFT, stack, buffer, packedLight);
	}
}
