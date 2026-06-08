package mod.arcomit.parkour.core.statemachine.state;

import java.util.List;

/**
 * 跑酷状态的抽象基类，提供转换规则的缓存注册机制。
 *
 * <p>子类在构造函数中调用 {@link #registerTransitions} 一次性注册所有转换规则，
 * 之后每次 tick 的 {@link #getTransitions} 调用都直接返回缓存的不可变列表，零额外开销。
 *
 * <p>具体状态只需覆写生命周期回调（onServerTick / onSimulationTick 等），无需关心规则管理。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public abstract class AbstractParkourState implements IParkourState {

	/** 缓存的转换规则列表，默认空实例不占用额外内存 */
	private List<IParkourStateTransition> transitions = List.of();

	/**
	 * 在子类构造函数中一次性注册此状态的所有转换规则。
	 *
	 * <p>调用后规则列表被固化为不可变集合，后续无法修改。多次调用会覆盖之前的注册。
	 *
	 * @param transitions 此状态的转换规则数组，为空时可传入零参数
	 */
	protected final void registerTransitions(IParkourStateTransition... transitions) {
		this.transitions = List.of(transitions);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>返回初始化时注册的不可变列表，每次调用零内存分配。
	 */
	@Override
	public final List<IParkourStateTransition> getTransitions() {
		return this.transitions;
	}
}
