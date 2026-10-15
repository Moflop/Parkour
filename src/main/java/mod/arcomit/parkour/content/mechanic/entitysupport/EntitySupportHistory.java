package mod.arcomit.parkour.content.mechanic.entitysupport;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 服务端保留短暂的实体位置历史，以接住客户端正在插值显示的真实顶部。 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public final class EntitySupportHistory {

	/** 原版实体更新间隔与客户端插值共同造成的延迟；不外推未来位置。 */
	private static final int HISTORY_TICKS = 8;
	public static final double SEARCH_MARGIN = 4;
	private static final Map<Entity, ArrayDeque<Sample>> HISTORY = new WeakHashMap<>();

	private record Sample(long tick, AABB bounds) {}

	private EntitySupportHistory() {}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		Entity entity = event.getEntity();
		if (entity.level().isClientSide || !EntitySupport.isSupportType(entity)) return;
		if (!entity.isAlive() || (!ParkourConfig.enableEntityWalking && !ParkourConfig.enableEntityJump)) {
			HISTORY.remove(entity);
			return;
		}
		var samples = HISTORY.computeIfAbsent(entity, ignored -> new ArrayDeque<>());
		AABB bounds = entity.getBoundingBox();
		if (!samples.isEmpty() && samples.getLast().bounds.getCenter().distanceToSqr(bounds.getCenter()) > 4) {
			samples.clear();// 传送不留下可踩的旧位置。
		}
		samples.addLast(new Sample(entity.level().getGameTime(), bounds));
		while (samples.size() > HISTORY_TICKS) samples.removeFirst();
	}

	/** 只在服务端线程读取历史；客户端碰撞始终严格使用眼前的碰撞箱。 */
	public static List<AABB> bounds(Entity entity) {
		if (entity.level().isClientSide) return List.of(entity.getBoundingBox());
		var samples = HISTORY.get(entity);
		if (samples == null || samples.isEmpty()) return List.of(entity.getBoundingBox());
		long now = entity.level().getGameTime();
		AABB current = entity.getBoundingBox();
		var result = new ArrayList<AABB>();
		for (Sample sample : samples) {
			if (now - sample.tick <= HISTORY_TICKS
					&& sample.bounds.getCenter().distanceToSqr(current.getCenter()) <= SEARCH_MARGIN * SEARCH_MARGIN
					&& !sample.bounds.equals(current)) result.add(sample.bounds);
		}
		result.add(current);
		return result;
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		HISTORY.clear();
	}
}
