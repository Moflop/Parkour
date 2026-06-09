package mod.arcomit.parkour.content.context;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * 墙体移动数据上下文，追踪玩家与墙面的碰撞关系。
 *
 * <p>维护五种墙体交互方向（墙跑碰撞面、墙跑移动面、爬墙面、滑墙面、垂挂面）
 * 以及一个内部追踪的 {@code lastCollisionRaw} 用于互斥判断。
 *
 * <p>每个方向字段使用 int（{@link Direction#get3DDataValue()}）序列化 +
 * 运行时 {@link Direction} 懒加载缓存的双轨模式。int 字段值 -1 表示无有效方向。
 *
 * <p>当某个碰撞方向发生变更时，通过 {@code onCollisionChanged} 自动
 * 清除互斥的其他方向（如爬墙开始时清除墙跑碰撞方向），保证动作互斥。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Getter
@NoArgsConstructor
public class WallMovementData {
	public static final Codec<WallMovementData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							Codec.INT.optionalFieldOf("wallRunCollisionDir3DData", -1)
									.forGetter(WallMovementData::getRunCollisionRaw),
							Codec.INT.optionalFieldOf("wallRunMovementDir3DData", -1)
									.forGetter(WallMovementData::getRunMoveRaw),
							Codec.INT.optionalFieldOf("wallClimbCollisionDir3DData", -1)
									.forGetter(WallMovementData::getClimbRaw),
							Codec.INT.optionalFieldOf("wallSlideCollisionDir3DData", -1)
									.forGetter(WallMovementData::getSlideRaw),
							Codec.INT.optionalFieldOf(
											"lastWallMovementCollisionDir3DData", -1)
									.forGetter(WallMovementData::getLastCollisionRaw),
							Codec.INT.optionalFieldOf("armhangDir3DData", -1)
									.forGetter(WallMovementData::getArmhangRaw),
							Codec.INT.optionalFieldOf("armhangCooldown", -1)
									.forGetter(WallMovementData::getArmhangCooldown),
							Codec.DOUBLE.optionalFieldOf("obstaclesHeight", 0.0)
									.forGetter(WallMovementData::getObstaclesHeight))
					.apply(instance, WallMovementData::new));
	public static final StreamCodec<ByteBuf, WallMovementData> STREAM_CODEC =
			StreamCodec.of((buf, data) -> {
				ByteBufCodecs.VAR_INT.encode(buf, data.getRunCollisionRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getRunMoveRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getClimbRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getSlideRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getLastCollisionRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getArmhangRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getArmhangCooldown());
				ByteBufCodecs.DOUBLE.encode(buf, data.getObstaclesHeight());
			}, buf -> new WallMovementData(ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.DOUBLE.decode(buf)));

	// ---- int 字段（序列化用） ----
	private int runCollisionRaw = -1;
	private int runMoveRaw = -1;
	private int climbRaw = -1;
	private int slideRaw = -1;
	private int lastCollisionRaw = -1;
	private int armhangRaw = -1;
	@Setter
	private int armhangCooldown = 0;
	@Setter
	private Vec3 armhangLastPos = null;
	@Setter
	private float armhangMoveDist = 0f;
	@Setter
	private double obstaclesHeight = 0.0;

	// ---- Direction 缓存字段（惰性加载，不参与序列化） ----
	private Direction runCollision;
	private Direction runMove;
	private Direction climb;
	private Direction slide;
	private Direction armhang;

	public WallMovementData(int runCollisionRaw, int runMoveRaw, int climbRaw, int slideRaw,
			int lastCollisionRaw, int armhangRaw, int armhangCooldown,
			double obstaclesHeight) {
		this.runCollisionRaw = runCollisionRaw;
		this.runMoveRaw = runMoveRaw;
		this.climbRaw = climbRaw;
		this.slideRaw = slideRaw;
		this.lastCollisionRaw = lastCollisionRaw;
		this.armhangRaw = armhangRaw;
		this.armhangCooldown = armhangCooldown;
		this.obstaclesHeight = obstaclesHeight;
	}

	// ==================== 私有 Helper ====================

	private static int encode(Direction dir) {
		return dir == null ? -1 : dir.get3DDataValue();
	}

	private static Direction resolve(int raw, Direction cache) {
		if (cache != null)
			return cache;
		return raw == -1 ? null : Direction.from3DDataValue(raw);
	}

	// ==================== WallRun Collision ====================

	public int getRunCollisionRaw() {
		return runCollisionRaw;
	}

	public void setRunCollisionRaw(int raw) {
		runCollisionRaw = raw;
		runCollision = null;
		onCollisionChanged(raw, this::resetClimb);
	}

	public Direction getRunCollision() {
		return runCollision = resolve(runCollisionRaw, runCollision);
	}

	public void setRunCollision(Direction dir) {
		runCollision = dir;
		runCollisionRaw = encode(dir);
		onCollisionChanged(runCollisionRaw, this::resetClimb);
	}

	public void resetRunCollision() {
		runCollision = null;
		runCollisionRaw = -1;
	}

	// ==================== WallRun Move ====================

	public int getRunMoveRaw() {
		return runMoveRaw;
	}

	public void setRunMoveRaw(int raw) {
		runMoveRaw = raw;
		runMove = null;
	}

	public Direction getRunMove() {
		return runMove = resolve(runMoveRaw, runMove);
	}

	public void setRunMove(Direction dir) {
		runMove = dir;
		runMoveRaw = encode(dir);
	}

	public void resetRunMove() {
		runMove = null;
		runMoveRaw = -1;
	}

	// ==================== WallClimb Collision ====================

	public int getClimbRaw() {
		return climbRaw;
	}

	public void setClimbRaw(int raw) {
		climbRaw = raw;
		climb = null;
		onCollisionChanged(raw, this::resetRunCollision);
	}

	public Direction getClimb() {
		return climb = resolve(climbRaw, climb);
	}

	public void setClimb(Direction dir) {
		climb = dir;
		climbRaw = encode(dir);
		onCollisionChanged(climbRaw, this::resetRunCollision);
	}

	public void resetClimb() {
		climb = null;
		climbRaw = -1;
	}

	// ==================== WallSlide Collision ====================

	public int getSlideRaw() {
		return slideRaw;
	}

	public void setSlideRaw(int raw) {
		slideRaw = raw;
		slide = null;
		onCollisionChanged(raw, () -> {
			resetRunCollision();
			resetClimb();
		});
	}

	public Direction getSlide() {
		return slide = resolve(slideRaw, slide);
	}

	public void setSlide(Direction dir) {
		slide = dir;
		slideRaw = encode(dir);
		onCollisionChanged(slideRaw, () -> {
			resetRunCollision();
			resetClimb();
		});
	}

	public void resetSlide() {
		slide = null;
		slideRaw = -1;
	}

	// ==================== Armhang ====================

	public int getArmhangRaw() {
		return armhangRaw;
	}

	public void setArmhangRaw(int raw) {
		armhangRaw = raw;
		armhang = null;
	}

	public Direction getArmhang() {
		return armhang = resolve(armhangRaw, armhang);
	}

	public void setArmhang(Direction dir) {
		armhang = dir;
		armhangRaw = encode(dir);
	}

	public void resetArmhang() {
		armhang = null;
		armhangRaw = -1;
	}

	// ==================== LastWallMovement Collision（纯内部追踪，无Direction API） ====================

	public int getLastCollisionRaw() {
		return lastCollisionRaw;
	}

	public void setLastCollisionRaw(int raw) {
		lastCollisionRaw = raw;
	}

	// ==================== 互斥逻辑 ====================

	private void onCollisionChanged(int newRaw, Runnable resetConflicting) {
		if (newRaw != lastCollisionRaw) {
			setLastCollisionRaw(newRaw);
			resetConflicting.run();
		}
	}

	// ==================== copyFrom ====================

	public void copyFrom(WallMovementData other) {
		this.runCollisionRaw = other.runCollisionRaw;
		this.runMoveRaw = other.runMoveRaw;
		this.climbRaw = other.climbRaw;
		this.slideRaw = other.slideRaw;
		this.lastCollisionRaw = other.lastCollisionRaw;
		this.armhangRaw = other.armhangRaw;
		this.armhangCooldown = other.armhangCooldown;
		this.obstaclesHeight = other.obstaclesHeight;
		this.runCollision = other.runCollision;
		this.runMove = other.runMove;
		this.climb = other.climb;
		this.slide = other.slide;
		this.armhang = other.armhang;
	}
}
