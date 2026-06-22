package mod.arcomit.parkour.content.behavior.vanilla;

import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-22
 */
public class VanillaState extends AbstractParkourState {

	public VanillaState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(
						context)));
	}
}
