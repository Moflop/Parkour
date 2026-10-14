package mod.arcomit.parkour.compat;

import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Resolves a moving surface's material in block space and plays it in world space. */
public final class DynamicSurfaceSounds {
	private DynamicSurfaceSounds() {}

	public static void playStepSound(Player player, @Nullable AABB contactBox,
			@Nullable TagKey<Block> skipTag, float volumeMultiplier) {
		if (player.level().isClientSide() || contactBox == null) {
			return;
		}
		DynamicBlockHit hit = DynamicCollisions.findSensorCollision(player.level(), player,
				contactBox, skipTag);
		if (hit == null) {
			return;
		}
		SoundType sound = hit.state().getSoundType(hit.blockLevel(), hit.localPos(), player);
		Vec3 position = hit.worldCenter();
		player.level().playSound(null, position.x, position.y, position.z, sound.getStepSound(),
				SoundSource.PLAYERS, sound.getVolume() * volumeMultiplier, sound.getPitch());
	}
}
