package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Per-player transient grip state; never survives death, reconnection or a dimension change. */
public final class ArmhangData {
	public ArmhangGrip grip;
	public ArmhangGrip lastBroadcastGrip;
	public boolean active, jumpReady;
	public int entryTick, mantleTicks, mantleDuration = ArmhangMotion.MANTLE_TICKS;
	public double catchImpact, shimmySpeed, shimmyDistance;
	public ArmhangMantle.Plan mantle;
	public boolean mantleBlocked;
	public Vec3 lastSurfacePosition;
	public java.util.UUID pendingSourceId;
	public ArmhangGrip pendingLocalGrip;
	private Vec3 previousLeft, previousRight;
	private int leftTransferTick = -100, rightTransferTick = -100;
	private boolean moveLeft = true;

	public static ArmhangData get(Player player) { return player.getData(ParkourAttachmentTypes.ARMHANG); }

	public void acceptGrip(Player player, ArmhangGrip next) {
		if (grip != null && grip.sameSurface(next)) {
			if (grip.localLeftHand().distanceToSqr(next.localLeftHand()) > 1.0E-8) {
				previousLeft = localHand(player, 0, true);
				leftTransferTick = player.tickCount;
			}
			if (grip.localRightHand().distanceToSqr(next.localRightHand()) > 1.0E-8) {
				previousRight = localHand(player, 0, false);
				rightTransferTick = player.tickCount;
			}
		} else previousLeft = previousRight = null;
		grip = next;
		active = true;
	}

	public void resolveSource(Player player) {
		if (pendingSourceId == null || pendingLocalGrip == null || grip == null) return;
		Vec3 anchor = grip.center(1);
		for (var source : mod.arcomit.parkour.compat.DynamicCollisions.sources(player.level(),
				new net.minecraft.world.phys.AABB(anchor, anchor).inflate(8))) {
			if (pendingSourceId.equals(source.networkId())) {
				grip = new ArmhangGrip(pendingLocalGrip.block(), source, pendingLocalGrip.localCenter(),
						pendingLocalGrip.localNormal(), pendingLocalGrip.localLeftHand(), pendingLocalGrip.localRightHand());
				pendingSourceId = null;
				pendingLocalGrip = null;
				break;
			}
		}
	}

	public boolean pressJump(boolean down) {
		if (!down) { jumpReady = true; return false; }
		if (!jumpReady) return false;
		jumpReady = false;
		return true;
	}

	public void begin(Player player, ArmhangGrip next) {
		grip = next;
		active = next != null;
		lastBroadcastGrip = null;
		entryTick = player.tickCount;
		catchImpact = Math.min(1, Math.max(0.15, -player.getDeltaMovement().y));
		shimmySpeed = shimmyDistance = 0;
		jumpReady = false;
		mantle = null;
		mantleTicks = 0;
		mantleBlocked = false;
		previousLeft = previousRight = null;
		leftTransferTick = rightTransferTick = -100;
		lastSurfacePosition = null;
		pendingSourceId = null;
		pendingLocalGrip = null;
		moveLeft = true;
	}

	/** One palm stays on its material point while the other reaches to the next real ledge point. */
	public void updateGrip(Player player, ArmhangGrip next) {
		if (next == null) return;
		if (grip == null || !grip.sameSurface(next)) {
			grip = next;
			previousLeft = previousRight = null;
			return;
		}
		double leftGap = grip.localLeftHand().distanceTo(next.localLeftHand());
		double rightGap = grip.localRightHand().distanceTo(next.localRightHand());
		if (Math.max(leftGap, rightGap) < 0.22) return;
		boolean left = Math.max(leftGap, rightGap) > 0.42 ? leftGap > rightGap : moveLeft;
		if (left) {
			previousLeft = localHand(player, 0, true);
			leftTransferTick = player.tickCount;
		} else {
			previousRight = localHand(player, 0, false);
			rightTransferTick = player.tickCount;
		}
		grip = new ArmhangGrip(next.block(), next.source(), next.localCenter(), next.localNormal(),
				left ? next.localLeftHand() : grip.localLeftHand(),
				left ? grip.localRightHand() : next.localRightHand());
		moveLeft = !left;
	}

	public static Vec3 renderLeftHand(Player player, float partialTick) { return get(player).hand(player, partialTick, true); }
	public static Vec3 renderRightHand(Player player, float partialTick) { return get(player).hand(player, partialTick, false); }

	private Vec3 hand(Player player, float partialTick, boolean left) {
		if (grip == null) return player.getEyePosition(partialTick);
		return grip.world(localHand(player, partialTick, left), partialTick);
	}

	private Vec3 localHand(Player player, float partialTick, boolean left) {
		Vec3 target = left ? grip.localLeftHand() : grip.localRightHand();
		Vec3 previous = left ? previousLeft : previousRight;
		double t = (player.tickCount + partialTick - (left ? leftTransferTick : rightTransferTick)) / 3.0;
		if (previous != null && t < 1) {
			double amount = ArmhangMotion.smooth(t);
			target = previous.lerp(target, amount).add(grip.localNormal().scale(-Math.sin(Math.PI * amount) * 0.045))
					.add(0, Math.sin(Math.PI * amount) * 0.07, 0);
		}
		return target;
	}
}
