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

/**
 * 跳跃数据上下文，记录跳跃的状态和蹬墙跳的方向历史。
 *
 * <p>维护三种蹬墙跳类型的方向（视角跳、上跳、平行跳）以及一个内部追踪的
 * {@code lastJumpRaw} 用于互斥判断。三种跳跃类型之间互斥：设置一种类型会自动清除另外两种。
 *
 * <p>额外追踪：
 * <ul>
 *   <li>{@code jumped} - 已起跳但尚未落地，供垂挂判断"玩家在空中"</li>
 *   <li>{@code ticksSinceLastJump} - 距上次跳跃的 tick 数，最高 100，用于防止连续跳跃</li>
 *   <li>{@code jumpReleaseGraceTicks} - 跳跃键松开后的宽限 tick 数，允许松开后仍判定为按着</li>
 * </ul>
 *
 * <p>方向字段使用 int（{@link Direction#get3DDataValue()}）序列化 +
 * Direction 惰性缓存的双轨模式。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Getter
@Setter
@NoArgsConstructor
public class JumpData {
	public static final Codec<JumpData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(Codec.BOOL.optionalFieldOf("isJumped", false)
									.forGetter(JumpData::isJumped),
							Codec.INT.optionalFieldOf("lastViewWallJumpDir3DData", -1)
									.forGetter(JumpData::getLastViewJumpRaw),
							Codec.INT.optionalFieldOf("lastUpWallJumpDir3DData", -1)
									.forGetter(JumpData::getLastUpJumpRaw),
							Codec.INT.optionalFieldOf("lastParallelWallJumpDir3DData",
											-1)
									.forGetter(JumpData::getLastParallelJumpRaw),
							Codec.INT.optionalFieldOf("lastWallJumpDir3DData", -1)
									.forGetter(JumpData::getLastJumpRaw),
							Codec.INT.optionalFieldOf("ticksSinceLastJump", 100)
									.forGetter(JumpData::getTicksSinceLastJump),
							Codec.INT.optionalFieldOf("jumpReleaseGraceTicks", 0)
									.forGetter(JumpData::getJumpReleaseGraceTicks))
					.apply(instance, JumpData::new));
	public static final StreamCodec<ByteBuf, JumpData> STREAM_CODEC =
			StreamCodec.of((buf, data) -> {
				ByteBufCodecs.BOOL.encode(buf, data.isJumped());
				ByteBufCodecs.VAR_INT.encode(buf, data.getLastViewJumpRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getLastUpJumpRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getLastParallelJumpRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getLastJumpRaw());
				ByteBufCodecs.VAR_INT.encode(buf, data.getTicksSinceLastJump());
				ByteBufCodecs.VAR_INT.encode(buf, data.getJumpReleaseGraceTicks());
			}, buf -> new JumpData(ByteBufCodecs.BOOL.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf)));

	// ---- 序列化字段 ----
	private boolean jumped = false;
	private int lastViewJumpRaw = -1;
	private int lastUpJumpRaw = -1;
	private int lastParallelJumpRaw = -1;
	private int lastJumpRaw = -1;
	private int ticksSinceLastJump = 100;
	private int jumpReleaseGraceTicks = 0;

	// ---- Direction 缓存（惰性加载，仅对外有引用的三种类型） ----
	private transient Direction lastViewJump;
	private transient Direction lastUpJump;
	private transient Direction lastParallelJump;

	public JumpData(boolean jumped, int lastViewJumpRaw, int lastUpJumpRaw,
			int lastParallelJumpRaw, int lastJumpRaw, int ticksSinceLastJump,
			int jumpReleaseGraceTicks) {
		this.jumped = jumped;
		this.lastViewJumpRaw = lastViewJumpRaw;
		this.lastUpJumpRaw = lastUpJumpRaw;
		this.lastParallelJumpRaw = lastParallelJumpRaw;
		this.lastJumpRaw = lastJumpRaw;
		this.ticksSinceLastJump = ticksSinceLastJump;
		this.jumpReleaseGraceTicks = jumpReleaseGraceTicks;
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

	private void onJumpChanged(int newRaw, Runnable resetConflicting) {
		if (newRaw != lastJumpRaw) {
			setLastJumpRaw(newRaw);
			resetConflicting.run();
		}
	}

	// ==================== copyFrom ====================

	public void copyFrom(JumpData other) {
		this.jumped = other.jumped;
		this.lastViewJumpRaw = other.lastViewJumpRaw;
		this.lastUpJumpRaw = other.lastUpJumpRaw;
		this.lastParallelJumpRaw = other.lastParallelJumpRaw;
		this.lastJumpRaw = other.lastJumpRaw;
		this.ticksSinceLastJump = other.ticksSinceLastJump;
		this.jumpReleaseGraceTicks = other.jumpReleaseGraceTicks;
		this.lastViewJump = other.lastViewJump;
		this.lastUpJump = other.lastUpJump;
		this.lastParallelJump = other.lastParallelJump;
	}

	// ==================== ViewJump ====================

	public int getLastViewJumpRaw() {
		return lastViewJumpRaw;
	}

	public void setLastViewJumpRaw(int raw) {
		lastViewJumpRaw = raw;
		lastViewJump = null;
		onJumpChanged(raw, () -> {
			resetLastUpJump();
			resetLastParallelJump();
		});
	}

	public Direction getLastViewJump() {
		return lastViewJump = resolve(lastViewJumpRaw, lastViewJump);
	}

	public void setLastViewJump(Direction dir) {
		lastViewJump = dir;
		lastViewJumpRaw = encode(dir);
		onJumpChanged(lastViewJumpRaw, () -> {
			resetLastUpJump();
			resetLastParallelJump();
		});
	}

	public void resetLastViewJump() {
		lastViewJump = null;
		lastViewJumpRaw = -1;
	}

	// ==================== UpJump ====================

	public int getLastUpJumpRaw() {
		return lastUpJumpRaw;
	}

	public void setLastUpJumpRaw(int raw) {
		lastUpJumpRaw = raw;
		lastUpJump = null;
		onJumpChanged(raw, () -> {
			resetLastViewJump();
			resetLastParallelJump();
		});
	}

	public Direction getLastUpJump() {
		return lastUpJump = resolve(lastUpJumpRaw, lastUpJump);
	}

	public void setLastUpJump(Direction dir) {
		lastUpJump = dir;
		lastUpJumpRaw = encode(dir);
		onJumpChanged(lastUpJumpRaw, () -> {
			resetLastViewJump();
			resetLastParallelJump();
		});
	}

	public void resetLastUpJump() {
		lastUpJump = null;
		lastUpJumpRaw = -1;
	}

	// ==================== ParallelJump ====================

	public int getLastParallelJumpRaw() {
		return lastParallelJumpRaw;
	}

	public void setLastParallelJumpRaw(int raw) {
		lastParallelJumpRaw = raw;
		lastParallelJump = null;
		onJumpChanged(raw, () -> {
			resetLastViewJump();
			resetLastUpJump();
		});
	}

	public Direction getLastParallelJump() {
		return lastParallelJump = resolve(lastParallelJumpRaw, lastParallelJump);
	}

	public void setLastParallelJump(Direction dir) {
		lastParallelJump = dir;
		lastParallelJumpRaw = encode(dir);
		onJumpChanged(lastParallelJumpRaw, () -> {
			resetLastViewJump();
			resetLastUpJump();
		});
	}

	public void resetLastParallelJump() {
		lastParallelJump = null;
		lastParallelJumpRaw = -1;
	}

	// ==================== LastJump（纯内部追踪，仅Raw） ====================

	public int getLastJumpRaw() {
		return lastJumpRaw;
	}

	public void setLastJumpRaw(int raw) {
		lastJumpRaw = raw;
	}
}
