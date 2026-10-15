package mod.arcomit.parkour.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 在物品提示中说明现有按键的作用，不引入新的操作绑定。 */
public class HorizontalBarItem extends BlockItem {
	public HorizontalBarItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.parkour.horizontal_bar.grab").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.parkour.horizontal_bar.swing").withStyle(ChatFormatting.GRAY));
	}
}
