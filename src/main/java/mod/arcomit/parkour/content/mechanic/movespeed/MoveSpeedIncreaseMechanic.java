package mod.arcomit.parkour.content.mechanic.movespeed;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.AttributeHelper;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 根据玩家是否疾跑，自动提升基础移动速度。
 * <p>
 * 疾跑和步行各使用独立的倍率配置，以"乘以基础值"的方式叠加到原版移速属性上。 当配置关闭该机制时，移除属性加成恢复原版行为。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class MoveSpeedIncreaseMechanic {
	private static final ResourceLocation SPEED_MULTIPLIER_MODIFIER_ID =
			ParkourMod.prefix("speed_multiplier");
	private static final double VANILLA_SPEED_MULTIPLIER = 1.0;

	public static void handle(Player player) {
		if (ParkourChecks.isVanillaState(ParkourContext.get(player)) || !ParkourConfig.enableMoveSpeedIncrease) {
			AttributeHelper.removeModifier(player, Attributes.MOVEMENT_SPEED,
					SPEED_MULTIPLIER_MODIFIER_ID);
			return;
		}

		double speedBonus = player.isSprinting() ?
				ParkourConfig.sprintSpeedMultiplier - VANILLA_SPEED_MULTIPLIER :
				ParkourConfig.walkSpeedMultiplier - VANILLA_SPEED_MULTIPLIER;

		AttributeHelper.setModifier(player, Attributes.MOVEMENT_SPEED,
				SPEED_MULTIPLIER_MODIFIER_ID, speedBonus,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}
}
