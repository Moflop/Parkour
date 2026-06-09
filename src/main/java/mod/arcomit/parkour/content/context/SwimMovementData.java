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
 * 游泳数据上下文，管理水中推进的冷却计时。
 *
 * <p>{@code boostCooldown} 为 0 时允许触发水中加速，
 * 触发后重置为配置值并按 tick 递减。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwimMovementData {
	public static final Codec<SwimMovementData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							Codec.INT.optionalFieldOf("swimmingBoostCooldown", 0)
									.forGetter(SwimMovementData::getBoostCooldown))
					.apply(instance, SwimMovementData::new));
	public static final StreamCodec<ByteBuf, SwimMovementData> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT,
					SwimMovementData::getBoostCooldown, SwimMovementData::new);

	private int boostCooldown = 0;

	public void copyFrom(SwimMovementData other) {
		this.boostCooldown = other.boostCooldown;
	}
}
