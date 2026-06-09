package mod.arcomit.parkour.content.mechanic.stepheight;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.utils.AttributeHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 根据玩家是否疾跑，自动调整跨越台阶的高度上限。
 * <p>
 * 疾跑时允许跨上更高的方块，步行时亦有小幅提升。 当配置关闭该机制时，移除属性加成恢复原版行为。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class StepHeightIncreaseMechanic {
	private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
			ParkourMod.prefix("step_height");
	private static final double VANILLA_STEP_HEIGHT = 0.6;

	public static void handle(Player player) {
		if (!ParkourConfig.enableStepHeightIncrease) {
			AttributeHelper.removeModifier(player, Attributes.STEP_HEIGHT,
					STEP_HEIGHT_MODIFIER_ID);
			return;
		}

		double stepHeightBonus = (player.isSprinting() && !player.isSwimming()) ?
				ParkourConfig.sprintStepHeight - VANILLA_STEP_HEIGHT :
				ParkourConfig.walkStepHeight - VANILLA_STEP_HEIGHT;

		AttributeHelper.setModifier(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER_ID,
				stepHeightBonus, AttributeModifier.Operation.ADD_VALUE);
	}
}
