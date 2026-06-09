package mod.arcomit.parkour.content.context;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 跑酷总数据上下文，聚合所有跑酷子数据到单个 Attachment 中。
 *
 * <p>通过 NeoForge {@link net.neoforged.neoforge.attachment.AttachmentType}
 * 挂载到每个玩家，数据更新实时同步（服务端-&gt;客户端）。包含六大子数据：
 * {@link GroundMovementData}、{@link WallMovementData}、{@link JumpData}、
 * {@link SwimMovementData}、{@link InputData}、{@link StateData}。
 *
 * <p>使用 Java {@code record} 保证值语义和不可变性，网络序列化通过
 * {@link #STREAM_CODEC} 实现，NBT 持久化通过 {@link #CODEC} 实现。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record ParkourContext(GroundMovementData ground, WallMovementData wall, JumpData jump,
                             SwimMovementData swim, InputData input, StateData state) {

	public static final Codec<ParkourContext> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							GroundMovementData.CODEC.optionalFieldOf("ground",
											new GroundMovementData())
									.forGetter(ParkourContext::ground),
							WallMovementData.CODEC.optionalFieldOf("wall",
											new WallMovementData())
									.forGetter(ParkourContext::wall),
							JumpData.CODEC.optionalFieldOf("jump", new JumpData())
									.forGetter(ParkourContext::jump),
							SwimMovementData.CODEC.optionalFieldOf("swim",
											new SwimMovementData())
									.forGetter(ParkourContext::swim),
							InputData.CODEC.optionalFieldOf("input", new InputData())
									.forGetter(ParkourContext::input))
					.apply(instance,
							(ground, wall, jump, swim, input) -> new ParkourContext(
									ground, wall, jump, swim,
									input, new StateData())));

	public static final StreamCodec<ByteBuf, ParkourContext> STREAM_CODEC =
			StreamCodec.composite(GroundMovementData.STREAM_CODEC,
					ParkourContext::ground, WallMovementData.STREAM_CODEC,
					ParkourContext::wall, JumpData.STREAM_CODEC,
					ParkourContext::jump, SwimMovementData.STREAM_CODEC,
					ParkourContext::swim, InputData.STREAM_CODEC,
					ParkourContext::input, StateData.STREAM_CODEC,
					ParkourContext::state, ParkourContext::new);

	/**
	 * 默认空构造函数，用于 Attachment 初始化。
	 */
	public ParkourContext() {
		this(new GroundMovementData(), new WallMovementData(), new JumpData(),
				new SwimMovementData(), new InputData(), new StateData());
	}

	/**
	 * 将跑酷上下文写入玩家的 NeoForge Attachment。
	 *
	 * <p>副作用：覆盖玩家已有的全部跑酷数据。
	 *
	 * @param player  目标玩家，不为 null
	 * @param context 要写入的上下文，可以为 null（但 Attachment 体系通常不建议 null）
	 */
	public static void set(Player player, ParkourContext context) {
		player.setData(ParkourAttachmentTypes.PARKOUR_CONTEXT, context);
	}

	/**
	 * 从玩家的 NeoForge Attachment 读取跑酷上下文。
	 *
	 * @param player 目标玩家，不为 null
	 * @return 绑定的跑酷上下文，若未设置则返回默认全零实例，不会为 null
	 */
	public static ParkourContext get(Player player) {
		return player.getData(ParkourAttachmentTypes.PARKOUR_CONTEXT);
	}
}
