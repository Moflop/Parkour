package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** 为玩家的碰撞查询补充实体顶部，保持其他实体之间的碰撞规则。 */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin {

	@ModifyReturnValue(method = "getEntityCollisions", at = @At("RETURN"))
	private List<VoxelShape> parkour$entitySupport(List<VoxelShape> original, Entity entity, AABB query) {
		return entity instanceof Player player ? EntitySupport.addCollisions(original, player, query) : original;
	}
}
