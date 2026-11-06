package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.content.behavior.armhang.client.ClientArmhangMovement;
import mod.arcomit.parkour.content.behavior.armhang.client.ClientArmhangRotation;
import mod.arcomit.parkour.content.behavior.armhang.network.BroadcastArmhangDirS2CPayload;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangDirC2SPayload;
import mod.arcomit.parkour.content.behavior.armhang.server.ServerArmhangSound;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArmhangDirectionTest {
	@Test
	void missingAndVerticalHangPointsAreRejectedBeforePlayerAccess() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertFalse(ArmhangCollision.hasValidHangPoint(null, direction));
			assertFalse(ArmhangPhysics.tryAdhesion(null, direction));
		}
	}

	@Test
	void invalidStoredDirectionsSkipPhysicsRotationMovementAndSound() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			// Exercise consumer guards even if a custom context returns an unsanitized value.
			WallMovementData wall = new WallMovementData() {
				@Override public Direction getArmhang() { return direction; }
			};
			wall.setArmhangLastPos(Vec3.ZERO);
			assertDoesNotThrow(() -> ArmhangPhysics.applyLevitateAndAdhesion(null, wall));
			assertDoesNotThrow(() -> ArmhangRotation.tryInsideCornerRotation(null, wall));
			assertDoesNotThrow(() -> ClientArmhangMovement.applyArmhangMovement(null, null, wall));
			assertDoesNotThrow(() -> ClientArmhangRotation.tryOutsideCornerRotation(
					null, 1, direction, wall, null));
			assertDoesNotThrow(() -> ServerArmhangSound.playMovementSound(null, wall));
		}
	}

	@Test
	void verticalNetworkDirectionsAreRejectedBeforeEnqueuingWorldAccess() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertDoesNotThrow(() -> SyncArmhangDirC2SPayload.Server.handle(
					new SyncArmhangDirC2SPayload(direction), null));
			assertDoesNotThrow(() -> BroadcastArmhangDirS2CPayload.Client.handle(
					new BroadcastArmhangDirS2CPayload(1, direction), null));
		}
	}

	@Test
	void horizontalNetworkDirectionsStillDispatchTheirQueuedHandlers() {
		AtomicInteger dispatched = new AtomicInteger();
		IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(
				IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
				(proxy, method, arguments) -> {
					if (method.getName().equals("enqueueWork")) {
						dispatched.incrementAndGet();
						return CompletableFuture.completedFuture(null);
					}
					throw new AssertionError("The queued world task must remain deferred: " + method);
				});
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			SyncArmhangDirC2SPayload.Server.handle(new SyncArmhangDirC2SPayload(direction), context);
			BroadcastArmhangDirS2CPayload.Client.handle(
					new BroadcastArmhangDirS2CPayload(1, direction), context);
		}
		assertEquals(8, dispatched.get());
	}
}
