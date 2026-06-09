package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.proxy.api.ISoundProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * {@link ISoundProxy} 的客户端实现。
 * <p>
 * 通过 {@code Minecraft.getInstance().getSoundManager()} 获取客户端 音效管理器，创建并播放绑定在实体上的位置音效。 标记为
 * {@link OnlyIn#CLIENT}，仅在客户端有效。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class ClientSoundProxyImpl implements ISoundProxy {
	@Override
	public void playEntityBoundSound(SoundEvent soundEvent, SoundSource source, float volume,
			float pitch, Entity entity, long seed) {
		Minecraft.getInstance().getSoundManager()
				.play(new EntityBoundSoundInstance(soundEvent, source, volume,
						pitch, entity, seed));
	}
}
