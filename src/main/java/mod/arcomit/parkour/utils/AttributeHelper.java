package mod.arcomit.parkour.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * 属性修改工具，避免每 Tick 频繁操作。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class AttributeHelper {

	/**
	 * 为玩家设置属性修饰符，仅在值变化时写入以避免每Tick重复叠加。
	 *
	 * @param player     目标玩家，不可为null
	 * @param attribute  要修改的属性，不可为null；如果玩家不持有该属性则静默跳过
	 * @param modifierId 修饰符唯一标识，用于幂等去重
	 * @param amount     修饰符数值，正负均可
	 * @param operation  修饰符运算方式（加法/乘法等）
	 */
	public static void setModifier(Player player, Holder<Attribute> attribute,
			Identifier modifierId, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}

		AttributeModifier existing = instance.getModifier(modifierId);
		if (existing == null || existing.amount() != amount) {
			instance.removeModifier(modifierId);
			instance.addTransientModifier(
					new AttributeModifier(modifierId, amount, operation));
		}
	}

	/**
	 * 移除玩家身上已存在的属性修饰符，若不存在则静默跳过。
	 *
	 * @param player     目标玩家，不可为null
	 * @param attribute  要清理的属性，不可为null
	 * @param modifierId 待移除的修饰符标识
	 */
	public static void removeModifier(Player player, Holder<Attribute> attribute,
			Identifier modifierId) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null && instance.getModifier(modifierId) != null) {
			instance.removeModifier(modifierId);
		}
	}
}
