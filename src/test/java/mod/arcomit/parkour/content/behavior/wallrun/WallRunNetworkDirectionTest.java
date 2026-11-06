package mod.arcomit.parkour.content.behavior.wallrun;

import mod.arcomit.parkour.content.behavior.wallrun.network.BroadcastWallRunDirS2CPayload;
import mod.arcomit.parkour.content.behavior.wallslide.network.BroadcastWallSlideDirS2CPayload;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WallRunNetworkDirectionTest {
	@Test
	void invalidDirectionsCannotEnqueueClientWorldAccess() {
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertDoesNotThrow(() -> BroadcastWallRunDirS2CPayload.Client.handle(
					new BroadcastWallRunDirS2CPayload(1, invalid), null));
			assertDoesNotThrow(() -> BroadcastWallSlideDirS2CPayload.Client.handle(
					new BroadcastWallSlideDirS2CPayload(1, invalid), null));
		}
	}

	@Test
	void allHorizontalDirectionsStillDispatch() {
		AtomicInteger queued = new AtomicInteger();
		IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(
				IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
				(proxy, method, arguments) -> {
					if (method.getName().equals("enqueueWork")) {
						queued.incrementAndGet();
						return CompletableFuture.completedFuture(null);
					}
					throw new AssertionError("World work must remain deferred: " + method);
				});
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BroadcastWallRunDirS2CPayload.Client.handle(new BroadcastWallRunDirS2CPayload(1, direction), context);
			BroadcastWallSlideDirS2CPayload.Client.handle(new BroadcastWallSlideDirS2CPayload(1, direction), context);
		}
		assertEquals(8, queued.get());
	}
}
