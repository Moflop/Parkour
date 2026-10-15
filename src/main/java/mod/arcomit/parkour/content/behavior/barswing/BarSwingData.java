package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import mod.arcomit.parkour.compat.DynamicCollisionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 每位玩家独立的摆荡约束；不持久化，重生后不继承抓握与冷却。 */
public class BarSwingData {
	public BlockPos bar;
	public ResourceKey<Level> dimension;
	public Direction.Axis axis = Direction.Axis.X;
	public Vec3 anchor = Vec3.ZERO;
	public Vec3 forward = new Vec3(0, 0, 1);
	public Vec3 down = new Vec3(0, -1, 0);
	public Vec3 barDirection = new Vec3(1, 0, 0);
	public DynamicCollisionSource source;
	public java.util.UUID sourceId;
	public Vec3 localAnchor = Vec3.ZERO;
	public Vec3 localForward = new Vec3(0, 0, 1);
	public Vec3 anchorMotion = Vec3.ZERO;
	public double angle, previousAngle, angularVelocity;
	public Vec3 velocity = Vec3.ZERO;
	public int cooldownUntil;
	public boolean active;
	public double releaseLift;
	public int releaseTick;
	public Vec3 releaseMotion = Vec3.ZERO;
	public boolean releaseAssist;

	public static BarSwingData get(Player player) {
		return player.getData(ParkourAttachmentTypes.BAR_SWING);
	}
}
