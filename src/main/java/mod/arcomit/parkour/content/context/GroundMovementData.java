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
 * 地面移动数据上下文，管理地面跑酷动作的冷却和时机窗口。
 *
 * <p>关键字段：
 * <ul>
 *   <li>{@code slideCooldown} - 滑铲冷却剩余 tick，>0 时禁止再次滑铲</li>
 *   <li>{@code landingRollWindow} - 落地翻滚的输入窗口 tick，落地后几 tick 内按下翻滚键仍有效</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroundMovementData {
	public static final Codec<GroundMovementData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							Codec.INT.optionalFieldOf("slideCooldown", 0)
									.forGetter(GroundMovementData::getSlideCooldown),
							Codec.INT.optionalFieldOf("landingRollWindow", 0)
									.forGetter(GroundMovementData::getLandingRollWindow))
					.apply(instance, GroundMovementData::new));
	public static final StreamCodec<ByteBuf, GroundMovementData> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT,
					GroundMovementData::getSlideCooldown, ByteBufCodecs.VAR_INT,
					GroundMovementData::getLandingRollWindow,
					GroundMovementData::new);

	/** 滑铲冷却（tick） */
	private int slideCooldown = 0;
	/** 落地翻滚窗口（tick） */
	private int landingRollWindow = 0;

	public void copyFrom(GroundMovementData other) {
		this.slideCooldown = other.slideCooldown;
		this.landingRollWindow = other.landingRollWindow;
	}
}
