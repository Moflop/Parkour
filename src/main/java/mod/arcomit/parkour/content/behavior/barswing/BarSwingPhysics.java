package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 在原版 travel 的位置应用摆荡约束，保留真实方块碰撞，不叠加行走加速或重力。 */
public final class BarSwingPhysics {
	private BarSwingPhysics() {}

	/** 水平移出横杆之后恢复被这根杆截断的起跳抬升，仍然要求头顶真实净空。 */
	public static void finishReleaseTravel(Player player) {
		BarSwingData data = BarSwingData.get(player);
		if (data.releaseAssist && player.isLocalPlayer()) {
			if (data.active || player.tickCount - data.releaseTick > 8
					|| !BarSwingCollision.hasBar(player, data)) data.releaseAssist = false;
			else {
				Vec3 horizontal = data.releaseMotion.multiply(1, 0, 1);
				Vec3 velocity = player.getDeltaMovement();
				if (velocity.dot(data.forward) < horizontal.dot(data.forward) * 0.5
						&& BarSwingCollision.clearTravel(player, player.position(), player.position().add(horizontal))) {
					player.setDeltaMovement(horizontal.x, velocity.y, horizontal.z);
					data.releaseAssist = false;
				}
			}
		}
		double lift = data.releaseLift;
		data.releaseLift = 0;
		if (lift <= 0 || !player.isLocalPlayer() || data.active || player.onGround()
				|| player.tickCount - data.releaseTick > 1 || player.getDeltaMovement().y > 0.01
				|| !BarSwingCollision.hasBar(player, data)) return;
		if (BarSwingCollision.clear(player, player.getBoundingBox().expandTowards(0, lift, 0))) {
			Vec3 velocity = player.getDeltaMovement();
			player.setDeltaMovement(velocity.x, lift, velocity.z);
		}
	}

	public static boolean travel(Player player) {
		if (!(ParkourContext.get(player).state().getState() instanceof BarSwingState)) return false;
		BarSwingData data = BarSwingData.get(player);
		if (!data.active) return false;
		player.resetFallDistance();
		if (!player.isLocalPlayer()) return true;
		DynamicCollisions.clearSurfaceInertia(player);
		double input = ParkourProxies.INPUT_PROXY.getForwardImpulse(player);
		var next = BarSwingMath.step(data.angle, data.angularVelocity, input);
		data.previousAngle = data.angle;
		data.angle = reachableAngle(player, data, next.angle());
		data.angularVelocity = Math.abs(data.angle - next.angle()) > 1.0E-5 ? -next.speed() * 0.12 : next.speed();
		Vec3 target = BarSwingMath.position(data.anchor, data.forward, data.down, data.angle);
		Vec3 movement = target.subtract(player.position());
		// 抓杆第一帧有少量径向误差，分帧收敛，避免吸附瞬移。
		double maximum = 0.55 + Math.min(1.5, data.anchorMotion.length());
		if (movement.length() > maximum) movement = movement.normalize().scale(maximum);
		Vec3 before = player.position();
		player.setDeltaMovement(movement);
		DynamicCollisions.move(player, MoverType.SELF, movement);
		data.velocity = player.position().subtract(before);
		player.setDeltaMovement(data.velocity);
		if (data.velocity.distanceToSqr(movement) > 0.0025) {
			// 原生求解器仍拥有最终碰撞决定权；贴墙时消耗摆动动量，保留抓握。
			Vec3 offset = player.position().subtract(data.anchor);
			data.angle = Math.atan2(offset.dot(data.forward), offset.dot(data.down));
			data.angularVelocity = 0;
		}
		if (DynamicCollisions.isOnGround(player)) {
			ParkourStateMachine.localTransitionTo(player, ParkourStates.DEFAULT.get());
		}
		return true;
	}

	/** 沿圆弧分段扫描，再逼近碰撞边界；不会沿墙滑到偏离支点长度的位置。 */
	private static double reachableAngle(Player player, BarSwingData data, double requested) {
		double safe = data.angle;
		Vec3 last = BarSwingMath.position(data.anchor, data.forward, data.down, safe);
		int steps = Math.max(1, (int) Math.ceil(Math.abs(requested - safe) / 0.025));
		for (int i = 1; i <= steps; i++) {
			double angle = data.angle + (requested - data.angle) * i / steps;
			Vec3 next = BarSwingMath.position(data.anchor, data.forward, data.down, angle);
			if (!clearSegment(player, last, next)) {
				double blocked = angle;
				for (int iteration = 0; iteration < 9; iteration++) {
					double middle = (safe + blocked) * 0.5;
					Vec3 point = BarSwingMath.position(data.anchor, data.forward, data.down, middle);
					if (clearSegment(player, last, point)) safe = middle;
					else blocked = middle;
				}
				return safe;
			}
			safe = angle;
			last = next;
		}
		return requested;
	}

	private static boolean clearSegment(Player player, Vec3 start, Vec3 end) {
		return BarSwingCollision.clearTravel(player, start, end);
	}
}
