package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** 鼠标转向和 tick 共用约束，避免渲染帧越过抓握方向或积累隐藏的回头角度。 */
public final class BarSwingView {
	private BarSwingView() {}

	public static void clamp(Player player) {
		if (!player.isLocalPlayer() || !(ParkourContext.get(player).state().getState() instanceof BarSwingState)) return;
		float forward = Directions.horizontalYaw(BarSwingData.get(player).forward);
		player.setYRot(BarSwingMath.clampYaw(player.getYRot(), forward));
		player.yRotO = BarSwingMath.clampYaw(player.yRotO, forward);
		player.setXRot(Mth.clamp(player.getXRot(), -75, 65));
		player.xRotO = Mth.clamp(player.xRotO, -75, 65);
	}
}
