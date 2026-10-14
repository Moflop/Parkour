package mod.arcomit.parkour.content.behavior.armhang;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Restores the whole vanilla movement result when a tentative ledge step is rejected. */
public record ArmhangMoveSnapshot(Vec3 position, Vec3 velocity, boolean onGround,
		boolean horizontalCollision, boolean verticalCollision, boolean verticalCollisionBelow,
		boolean minorHorizontalCollision) {
	public static ArmhangMoveSnapshot capture(Player player) {
		return new ArmhangMoveSnapshot(player.position(), player.getDeltaMovement(), player.onGround(),
				player.horizontalCollision, player.verticalCollision, player.verticalCollisionBelow,
				player.minorHorizontalCollision);
	}

	public void restore(Player player) {
		restore(player, position);
	}

	public void restore(Player player, Vec3 safePosition) {
		player.setPos(safePosition);
		player.setDeltaMovement(velocity);
		player.setOnGround(onGround);
		player.horizontalCollision = horizontalCollision;
		player.verticalCollision = verticalCollision;
		player.verticalCollisionBelow = verticalCollisionBelow;
		player.minorHorizontalCollision = minorHorizontalCollision;
	}
}
