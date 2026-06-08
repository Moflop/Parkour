package mod.arcomit.parkour.core.statemachine.state;

import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 跑酷状态接口，定义状态生命周期的三层调度模型。
 *
 * <p>每个状态的生命周期回调被拆分为三个独立层级：
 * <ul>
 *   <li><b>Simulation 层</b>：物理移动和速度向量修改的核心层，需要双端（Server + LocalPlayer）同时执行以保证预测一致。</li>
 *   <li><b>Server 层</b>：服务端权威逻辑，如数值扣除、条件判定、持久化等。</li>
 *   <li><b>Client 层</b>：客户端视觉效果，如粒子、音效、渲染参数等。</li>
 * </ul>
 *
 * <p>进入/退出/Tick 时，框架自动按端分发到对应层级，子类只需覆写关心的回调即可。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IParkourState {

	/**
	 * 默认的动画变体 ID
	 */
	public static final int DEFAULT_ANIM_VARIANT = 0;

	/**
	 * 返回此状态下注册的所有转换规则。
	 *
	 * <p>每次 tick 都会被状态机遍历以评估是否需要切换状态。
	 *
	 * @return 不可变的转换规则列表，不为 null，无规则时为空列表
	 */
	List<IParkourStateTransition> getTransitions();

	/**
	 * 进入状态时触发，自动判断当前逻辑端并分发到 Simulation/Server/Client 三层。
	 *
	 * <p>服务端：执行 Simulation + Server 层<br>
	 * 客户端本地玩家：执行 Simulation + Client 层<br>
	 * 客户端其他玩家：仅执行 Client 层
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，承载当前状态数据，不为 null
	 */
	default void onEnter(Player player, ParkourContext context) {
		boolean isClient = player.level().isClientSide();

		if (isClient) {
			if (player.isLocalPlayer()) {
				onSimulationEnter(player, context);
			}
			onClientEnter(player, context);

		} else {
			onSimulationEnter(player, context);
			onServerEnter(player, context);
		}
	}

	/**
	 * 进入状态 — 服务端权威逻辑入口，如扣除体力值、记录时间戳等。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onServerEnter(Player player, ParkourContext context) {
	}

	/**
	 * 进入状态 — 物理模拟层入口，仅在 Server 和 LocalPlayer 上执行，用于初始化速度向量、设置重力系数等。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onSimulationEnter(Player player, ParkourContext context) {
	}

	/**
	 * 进入状态 — 客户端渲染层入口，用于播放动画、粒子效果、音效等纯视觉反馈。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onClientEnter(Player player, ParkourContext context) {
	}

	/**
	 * 退出状态时触发，自动判断当前逻辑端并分发到对应的三层回调。
	 *
	 * <p>分发规则与 {@link #onEnter} 一致：服务端走 Simulation+Server，客户端本地玩家走 Simulation+Client，远程玩家仅走 Client。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onExit(Player player, ParkourContext context) {
		boolean isClient = player.level().isClientSide();

		if (isClient) {
			if (player.isLocalPlayer()) {
				onSimulationExit(player, context);
			}
			onClientExit(player, context);

		} else {
			onSimulationExit(player, context);
			onServerExit(player, context);
		}
	}

	/**
	 * 退出状态 — 服务端权威逻辑出口，如恢复数值、清除标记等。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onServerExit(Player player, ParkourContext context) {
	}

	/**
	 * 退出状态 — 物理模拟层出口，仅在 Server 和 LocalPlayer 上执行，用于恢复默认物理参数。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onSimulationExit(Player player, ParkourContext context) {
	}

	/**
	 * 退出状态 — 客户端渲染层出口，如停止动画、清除粒子等。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onClientExit(Player player, ParkourContext context) {
	}

	/**
	 * 每 tick 的状态驱动入口，自动分发到对应层级。
	 *
	 * <p>分发规则与 {@link #onEnter} 一致。每一帧状态机都会调用此方法。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onTick(Player player, ParkourContext context) {
		boolean isClient = player.level().isClientSide();

		if (isClient) {
			if (player.isLocalPlayer()) {
				onSimulationTick(player, context);
			}
			onClientTick(player, context);

		} else {
			onSimulationTick(player, context);
			onServerTick(player, context);
		}
	}

	/**
	 * 服务端 Tick — 处理权威的数值扣除、条件判定等仅服务端有权执行的逻辑。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onServerTick(Player player, ParkourContext context) {
	}

	/**
	 * 核心模拟 Tick：专用于物理移动、速度向量修改等需要【双端预测】的逻辑。 该方法只会在 Server 和 LocalPlayer 上执行。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onSimulationTick(Player player, ParkourContext context) {
	}

	/**
	 * 客户端 Tick — 仅处理纯视觉反馈，如相机摇晃、HUD 更新、动画进度推进等。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onClientTick(Player player, ParkourContext context) {
	}

	/**
	 * 状态进入前的准入校验，由状态机在评估转换规则时调用。
	 *
	 * <p>在转换评估流程中作为第二道防线：先判断转换条件，再调用此方法确认目标状态允许进入。
	 * 返回 false 时状态机将跳过该转换，继续评估下一条规则。
	 *
	 * @param player  待切换状态的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示允许进入此状态，默认实现始终返回 true
	 */
	default boolean canEnter(Player player, ParkourContext context) {
		return true;
	}

	/**
	 * 校验当前状态是否仍然合法，由状态机每 tick 调用。
	 *
	 * <p>返回 false 时状态机将强制回退到默认状态。典型场景：玩家落地后离开滞空状态、护盾耗尽后退出防御状态。
	 *
	 * @param player  状态所属的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示可以继续维持当前状态，默认实现始终返回 true
	 */
	default boolean isValid(Player player, ParkourContext context) {
		return true;
	}

	/**
	 * 获取该状态下自定义的完整实体尺寸（包含碰撞箱宽、高以及视线高度）。
	 *
	 * <p>状态切换时，若新旧尺寸不同则触发 {@link Player#refreshDimensions} 重算碰撞箱。
	 *
	 * @param player 状态所属的玩家，不为 null
	 * @return 自定义尺寸；返回 null 表示使用原版默认尺寸
	 */
	default EntityDimensions getCustomDimensions(Player player) {
		return null;
	}

	/**
	 * 当状态机决定进入此状态时调用，用于生成并分配一个变体 ID。
	 *
	 * <p>默认返回 {@link #DEFAULT_ANIM_VARIANT}（0，代表无变体或默认动画）。
	 * 如需随机变体，应使用双端一致的随机数生成方式，确保服务器和客户端生成的变体 ID 一致，
	 * 以避免动画不同步。
	 *
	 * @param player 状态所属的玩家，不为 null
	 * @return 动画变体 ID，取值范围取决于具体状态实现
	 */
	default int generateVariant(Player player) {
		return DEFAULT_ANIM_VARIANT;
	}

	/**
	 * 返回此状态下强制应用的玩家姿势（如蹲伏、爬行、游泳等）。
	 *
	 * <p>状态切换时，状态机会调用 {@link Player#setForcedPose} 应用此值。
	 *
	 * @return 该状态绑定的姿势；返回 null 表示不强制覆盖原版姿势
	 */
	default Pose getLinkedPose() {
		return null;
	}

	/**
	 * 当有其他玩家（tracker）开始追踪该实体（target）时调用，用于向追踪者发送该状态特有的额外同步数据包。
	 *
	 * <p>Minecraft 默认只同步基础实体数据，状态特有的自定义参数（如动画变体、额外属性）
	 * 需要通过此回调以网络包形式补发给新加入视野的追踪者。
	 *
	 * @param tracker 开始追踪的玩家（服务端），接收额外同步数据，不为 null
	 * @param target  被追踪的玩家（即状态所属玩家），不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	default void onTrackingStart(ServerPlayer tracker, Player target, ParkourContext context) {
	}
}
