package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.core.proxy.api.ISoundProxy;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * {@link ISoundProxy} 的服务端空操作实现。
 * <p>
 * 服务端无 {@code SoundManager} 和 {@code EntityBoundSoundInstance}，
 * 音效播放请求全部丢弃。这符合 Minecraft 的设计——音效由客户端本地播放，
 * 服务端仅通过原版数据包触发，不参与本模组的音效管线。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummySoundProxy implements ISoundProxy {
	@Override
	public void playEntityBoundSound(SoundEvent soundEvent, SoundSource source, float volume,
			float pitch, Entity entity, long seed) {
	}
}
