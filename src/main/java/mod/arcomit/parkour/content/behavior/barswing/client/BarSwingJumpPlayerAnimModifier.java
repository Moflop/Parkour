package mod.arcomit.parkour.content.behavior.barswing.client;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/** 跃出收臂后逐渐交还行走摆臂，避免高速飞行时原版步态突然插入动作末端。 */
public class BarSwingJumpPlayerAnimModifier extends AbstractModifier {
	private static final Map<Player, Integer> RELEASES = new WeakHashMap<>();
	private final Player player;
	private boolean started;

	public BarSwingJumpPlayerAnimModifier(Player player) { this.player = player; }

	@Override
	public void setupAnim(AnimationData state) {
		if (!started) {
			RELEASES.put(player, player.tickCount);
			started = true;
		}
		super.setupAnim(state);
	}

	public static float walkWeight(Player player, float ageInTicks) {
		Integer release = RELEASES.get(player);
		if (release == null) return 1;
		float age = ageInTicks - release;
		if (age >= 21 || age < 0) {
			RELEASES.remove(player);
			return 1;
		}
		return recoveryWeight(age);
	}

	public static float recoveryWeight(float age) {
		float progress = Math.clamp((age - 13) / 8, 0, 1);
		return progress * progress * (3 - 2 * progress);
	}
}
