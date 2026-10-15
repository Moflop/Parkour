package mod.arcomit.parkour.content.mechanic.entitysupport;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 实体行走与借力跳共用的接触查询，不将动态方块结构的包围盒当作平台。 */
public final class EntitySupport {

	private EntitySupport() {}

	public static boolean isEnabled(Player player) {
		return ParkourConfig.enableEntityWalking && player.isAlive()
				&& ParkourChecks.canPerformAction(player)
				&& isParkourEnabled(player);
	}

	public static boolean isParkourEnabled(Player player) {
		// 出生点搜索发生于 ServerPlayer 构造中，此时不能创建会同步的 Attachment。
		if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) return false;
		var context = ParkourContext.get(player);
		return player.level().isClientSide
				? ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(context)
				: context.state().isParkourEnabled();
	}

	public static boolean canSupport(Player player, Entity entity) {
		return entity != player && entity.isAlive() && !entity.isSpectator() && !entity.noPhysics
				&& !player.isPassengerOfSameVehicle(entity)
				&& isSupportType(entity);
	}

	public static boolean isSupportType(Entity entity) {
		return (entity instanceof LivingEntity || entity instanceof Boat || entity instanceof AbstractMinecart)
				&& !(entity instanceof ArmorStand stand && stand.isMarker());
	}

	private static AABB searchBounds(Player player, AABB query) {
		return query.inflate(player.level().isClientSide ? EntitySupportGeometry.EPSILON : EntitySupportHistory.SEARCH_MARGIN);
	}

	/** 原版 move、潜行退让和净空检查使用同一份单向顶部碰撞。 */
	public static List<VoxelShape> addCollisions(List<VoxelShape> original, Player player, AABB query) {
		if (!isEnabled(player)) return original;
		List<VoxelShape> result = null;
		for (Entity entity : player.level().getEntities(player, searchBounds(player, query),
				candidate -> canSupport(player, candidate))) {
			for (AABB bounds : EntitySupportHistory.bounds(entity)) {
				if (player.canCollideWith(entity) && bounds.equals(entity.getBoundingBox())) continue;
				// 下降中的旧顶面只能验证接触，不能挡住客户端随实体向下的位移包。
				if (bounds.maxY > entity.getBoundingBox().maxY + EntitySupportGeometry.EPSILON) continue;
				if (!bounds.intersects(query.inflate(EntitySupportGeometry.EPSILON))
						|| !EntitySupportGeometry.isAbove(player.getBoundingBox(), bounds)) continue;
				if (result == null) result = new ArrayList<>(original);
				result.add(Shapes.create(bounds));
			}
		}
		return result == null ? original : result;
	}

	@Nullable
	public static Entity findSupport(Player player) {
		if (!isEnabled(player)) return null;
		AABB body = player.getBoundingBox();
		AABB feet = new AABB(body.minX, body.minY - EntitySupportGeometry.CONTACT_TOLERANCE,
				body.minZ, body.maxX, body.minY + EntitySupportGeometry.CONTACT_TOLERANCE, body.maxZ);
		Entity nearest = null;
		double distance = Double.MAX_VALUE;
		for (Entity entity : player.level().getEntities(player, searchBounds(player, feet), candidate -> canSupport(player, candidate))) {
			AABB bounds = supportBounds(player, entity);
			if (bounds == null) continue;
			double candidateDistance = bounds.getCenter().distanceToSqr(body.getCenter());
			if (candidateDistance < distance) {
				nearest = entity;
				distance = candidateDistance;
			}
		}
		return nearest;
	}

	@Nullable
	public static AABB supportBounds(Player player, Entity entity) {
		AABB nearest = null;
		double distance = Double.MAX_VALUE;
		List<AABB> history = EntitySupportHistory.bounds(entity);
		List<AABB> surfaces = new ArrayList<>(history);
		for (int i = 1; i < history.size(); i++) {
			AABB interpolated = EntitySupportGeometry.interpolateSupport(player.getBoundingBox(),
					history.get(i - 1), history.get(i));
			if (interpolated != null) surfaces.add(interpolated);
		}
		for (AABB bounds : surfaces) {
			if (!EntitySupportGeometry.isSupported(player.getBoundingBox(), bounds)) continue;
			double candidate = bounds.getCenter().distanceToSqr(player.getBoundingBox().getCenter());
			if (candidate < distance) {
				nearest = bounds;
				distance = candidate;
			}
		}
		return nearest;
	}

	/** 必须实际靠近且无遮挡，不能隔着墙或天花板借力。 */
	public static boolean hasJumpContact(Player player, double reach) {
		AABB body = player.getBoundingBox();
		for (Entity entity : player.level().getEntities(player, searchBounds(player, body.inflate(reach)),
				candidate -> canSupport(player, candidate))) {
			for (AABB target : EntitySupportHistory.bounds(entity)) {
				if (EntitySupportGeometry.distanceSquared(body, target) > reach * reach) continue;
				Vec3 contact = EntitySupportGeometry.closestPoint(target, body.getCenter());
				Vec3 origin = EntitySupportGeometry.closestPoint(body.deflate(0.01), contact);
				if (player.level().clip(new ClipContext(origin, contact, ClipContext.Block.COLLIDER,
						ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) return true;
			}
		}
		return false;
	}
}
