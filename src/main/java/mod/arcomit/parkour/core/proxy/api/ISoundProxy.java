package mod.arcomit.parkour.core.proxy.api;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * 音效播放代理接口。
 * <p>
 * 封装客户端音效系统的调用——播放绑定在实体上的位置音效。 服务端无 {@code SoundManager} 和 {@code EntityBoundSoundInstance}，
 * 通过此接口安全降级。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface ISoundProxy {
	/**
	 * 播放一个跟随实体位置移动的音效实例。
	 * <p>
	 * 音效在实体所在位置以指定的音量和音高播放。实体移动时音效位置自动跟随。 服务端调用时无操作——游戏中的声音必定由玩家客户端自行播放。
	 * </p>
	 *
	 * @param soundEvent 音效事件，定义了要播放的具体音效资源，不可为null
	 * @param source     音效来源分类（如玩家、环境、方块等），不可为null
	 * @param volume     音量倍率，{@code 1.0F} 为原始音量，取值范围通常为 {@code [0.0F, 1.0F]}
	 * @param pitch      音高倍率，{@code 1.0F} 为原始音高，范围为 {@code [0.5F, 2.0F]}
	 * @param entity     音效绑定的实体，音效位置跟随该实体移动，不可为null
	 * @param seed       随机种子，用于音效的随机变体选择，每个种子产生固定的随机结果
	 */
	void playEntityBoundSound(SoundEvent soundEvent, SoundSource source, float volume,
			float pitch, Entity entity, long seed);
}
