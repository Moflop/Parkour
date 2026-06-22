package mod.arcomit.parkour.core.proxy.api;

import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.world.entity.player.Player;

public interface IGetClientConfigProxy {

	boolean isEnableParkour(ParkourContext context);
}
