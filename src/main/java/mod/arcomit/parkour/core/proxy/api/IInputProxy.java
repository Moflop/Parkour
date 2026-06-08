package mod.arcomit.parkour.core.proxy.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

/**
 * 玩家输入代理接口。
 * <p>
 * 封装对玩家原始输入（WASD方向、跳跃、潜行等）的读取。
 * 状态机需要根据当前输入决定跑酷动作的触发时机和方向，
 * 但客户端输入对象仅在客户端存在，服务端通过此接口安全降级。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IInputProxy {
	/**
	 * 获取玩家横向（A/D键）输入的力度值。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 正值表示右移，负值表示左移，取值范围 {@code [-1, 1]}；服务端返回 {@code 0}
	 */
	float getLeftImpulse(Player player);

	/**
	 * 获取玩家纵向（W/S键）输入的力度值。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 正值表示前进，负值表示后退，取值范围 {@code [-1, 1]}；服务端返回 {@code 0}
	 */
	float getForwardImpulse(Player player);

	/**
	 * 是否按下上方向键（如跳跃绑定的空格键）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getUp(Player player);

	/**
	 * 是否按下下方向键（如潜行绑定的Shift键）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getDown(Player player);

	/**
	 * 是否按下左方向键。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getLeft(Player player);

	/**
	 * 是否按下右方向键。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getRight(Player player);

	/**
	 * 是否按下跳跃键。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getJumping(Player player);

	/**
	 * 是否按下潜行键（Shift）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 按下时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean getShiftKeyDown(Player player);

	/**
	 * 获取归一化后的移动方向向量。
	 *
	 * @param player 目标玩家，不可为null
	 * @return x分量表示左右移动、y分量表示前后移动；服务端返回 {@link Vec2#ZERO}
	 */
	Vec2 getMoveVector(Player player);

	/**
	 * 玩家是否产生前进方向的输入（区别于有没有实际位移，仅判断是否有按W键的意图）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 有前进输入时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean hasForwardImpulse(Player player);
}
