package mod.arcomit.parkour.content.context;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * 跑酷状态数据，维护当前状态、计时和动画变体。
 *
 * <p>通过双轨设计支持高效的运行时访问：{@code stateId}/{@code ticksInState}
 * 参与网络序列化（通过{@link #STREAM_CODEC}），而{@code cachedState}/{@code cachedKey} 为 transient
 * 惰性缓存，避免每帧从注册表以 Identifier 反查状态实例。
 *
 * <p>状态切换时 {@link #setState(IParkourState)} 自动同步缓存与序列化字段，
 * 不参与磁盘序列化（Codec 在 {@link ParkourContext} 层面构建时固定为默认值）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@NoArgsConstructor
public class StateData {

	public static final StreamCodec<ByteBuf, StateData> STREAM_CODEC =
			StreamCodec.composite(Identifier.STREAM_CODEC, StateData::getStateId,
					ByteBufCodecs.VAR_INT, StateData::getTicksInState,
					StateData::new);

	/** 当前状态的注册键，-1 表示无状态 */
	@Getter
	private Identifier stateId = ParkourStates.DEFAULT.getId();
	/** 进入当前状态后经过的 tick 数 */
	@Getter
	@Setter
	private int ticksInState = 0;
	/** 动画变体索引，同一状态可对应多种动画（如墙跑左右），0 为默认 */
	@Getter
	@Setter
	private int animationVariant = 0;

	/** 当前状态的注册键缓存，避免重复调用 {@code Registry.getKey()} */
	private transient Identifier cachedKey = ParkourStates.DEFAULT.getId();
	/** 当前状态实例缓存，null 时通过 {@link #getState()} 惰性加载 */
	private transient IParkourState cachedState = null;
	/** 上一个状态实例，供转换规则判断来源状态，null 表示尚未发生切换 */
	@Getter
	@Setter
	private transient IParkourState lastState = null;

	/**
	 * 从网络包反序列化时使用，传入注册键和 tick 计数重建状态数据。
	 *
	 * @param stateId      状态的 Identifier 注册键，null 时回退到默认状态
	 * @param ticksInState 已在该状态中的 tick 数，非负
	 */
	public StateData(Identifier stateId, int ticksInState) {
		this.stateId = stateId != null ? stateId : ParkourStates.DEFAULT.getId();
		this.ticksInState = ticksInState;
	}

	/**
	 * 通过注册表查找当前状态实例，结果会被缓存。
	 *
	 * @return 当前状态实例，若注册表中找不到则返回默认状态，不会为 null
	 */
	public IParkourState getState() {
		if (cachedState == null) {
			cachedState = ParkourRegistries.PARKOUR_STATE_REGISTRY.get(stateId)
					.map(r -> r.value()).orElse(null);
			if (cachedState == null) {
				cachedState = ParkourStates.DEFAULT.get();
			}
		}
		return cachedState;
	}

	/**
	 * 切换到新的跑酷状态，同步更新缓存、注册键和序列化字段。
	 *
	 * @param newState 目标状态实例，不为 null
	 */
	public void setState(IParkourState newState) {
		cachedState = newState;
		if (cachedKey == null || ParkourRegistries.PARKOUR_STATE_REGISTRY.get(cachedKey)
				.map(r -> r.value() != newState).orElse(true)) {
			Identifier freshKey =
					ParkourRegistries.PARKOUR_STATE_REGISTRY.getKey(newState);
			cachedKey = freshKey != null ? freshKey : ParkourStates.DEFAULT.getId();
		}
		stateId = cachedKey;
	}

	/**
	 * 从另一个 StateData 复制所有字段（含缓存），用于上下文数据的批量同步。
	 *
	 * @param other 源数据，不为 null
	 */
	public void copyFrom(StateData other) {
		this.stateId = other.stateId;
		this.ticksInState = other.ticksInState;
		this.cachedState = other.cachedState;
		this.cachedKey = other.cachedKey;
	}
}
