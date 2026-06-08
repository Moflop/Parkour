package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.proxy.api.IInputProxy;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * {@link IInputProxy} 的客户端实现。
 * <p>
 * 将 {@code Player} 向下转型为 {@code LocalPlayer} 后直读其
 * {@code input} 字段中的按键状态。若传入的不是本地玩家实例则返回
 * 安全的默认值（0 或 false），保证在非客户端的调用路径下不崩溃。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class ClientInputProxyImpl implements IInputProxy {

	@Override
	public float getLeftImpulse(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.leftImpulse;
		}
		return 0;
	}

	@Override
	public float getForwardImpulse(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.forwardImpulse;
		}
		return 0;
	}

	@Override
	public boolean getUp(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.up;
		}
		return false;
	}

	@Override
	public boolean getDown(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.down;
		}
		return false;
	}

	@Override
	public boolean getLeft(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.left;
		}
		return false;
	}

	@Override
	public boolean getRight(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.right;
		}
		return false;
	}

	@Override
	public boolean getJumping(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.jumping;
		}
		return false;
	}

	@Override
	public boolean getShiftKeyDown(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.shiftKeyDown;
		}
		return false;
	}

	@Override
	public Vec2 getMoveVector(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.getMoveVector();
		}
		return Vec2.ZERO;
	}

	@Override
	public boolean hasForwardImpulse(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.input.hasForwardImpulse();
		}
		return false;
	}
}
