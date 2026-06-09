package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.core.proxy.api.IInputProxy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

/**
 * {@link IInputProxy} 的服务端空操作实现。
 * <p>
 * 服务端不存在 {@code LocalPlayer#input} 对象，所有按键查询均返回 安全的零值——这对状态机安全性无影响，因为服务端不应基于客户端输入 做任何行为判断。返回
 * {@code false} 或 {@code 0} 等同于"无输入"。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummyInputProxy implements IInputProxy {
	@Override
	public float getLeftImpulse(Player player) {
		return 0;
	}

	@Override
	public float getForwardImpulse(Player player) {
		return 0;
	}

	@Override
	public boolean getUp(Player player) {
		return false;
	}

	@Override
	public boolean getDown(Player player) {
		return false;
	}

	@Override
	public boolean getLeft(Player player) {
		return false;
	}

	@Override
	public boolean getRight(Player player) {
		return false;
	}

	@Override
	public boolean getJumping(Player player) {
		return false;
	}

	@Override
	public boolean getShiftKeyDown(Player player) {
		return false;
	}

	@Override
	public Vec2 getMoveVector(Player player) {
		return Vec2.ZERO;
	}

	@Override
	public boolean hasForwardImpulse(Player player) {
		return false;
	}
}
