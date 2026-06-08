package mod.arcomit.parkour.content.mechanic.jumpstrength;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.utils.AttributeHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 为玩家附加恒定的跳跃力度属性加成，使其能跳到更高。
 * <p>
 * 无条件生效——只要模组加载即始终应用该加成值。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class JumpStrengthIncreaseMechanic {
	private static final ResourceLocation JUMP_STRENGTH_ADD_MODIFIER_ID =
			ParkourMod.prefix("jump_strength_add");
	private static final double ADD_JUMP_STRENGTH = 0.01;

	public static void handle(Player player) {
		AttributeHelper.setModifier(player, Attributes.JUMP_STRENGTH,
				JUMP_STRENGTH_ADD_MODIFIER_ID, ADD_JUMP_STRENGTH,
				AttributeModifier.Operation.ADD_VALUE);
	}
}
