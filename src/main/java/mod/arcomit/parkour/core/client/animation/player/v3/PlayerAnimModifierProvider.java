package mod.arcomit.parkour.core.client.animation.player.v3;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import net.minecraft.world.entity.player.Player;
import java.util.List;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-27
 */
@FunctionalInterface
public interface PlayerAnimModifierProvider {
	List<AbstractModifier> getModifiers(Player player, int variant);
}
