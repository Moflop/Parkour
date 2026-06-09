package mod.arcomit.parkour.content.context;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 输入数据上下文，记录自定义按键的输入参数。
 *
 * <p>当前仅包含左右方向冲量值（{@code leftImpulse}），供滑铲空中变向（tap strafing）
 * 等机制读取玩家侧向输入强度。参与 NBT 序列化和网络同步。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InputData {
	public static final Codec<InputData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(Codec.FLOAT.optionalFieldOf("leftImpulse", 0f)
							.forGetter(InputData::getLeftImpulse))
					.apply(instance, InputData::new));
	public static final StreamCodec<ByteBuf, InputData> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.FLOAT, InputData::getLeftImpulse,
					InputData::new);

	private float leftImpulse = 0f;

	public void copyFrom(InputData other) {
		this.leftImpulse = other.leftImpulse;
	}
}
