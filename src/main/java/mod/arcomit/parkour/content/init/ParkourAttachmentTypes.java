package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.sensor.SensorDataManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge 附件类型注册表。
 *
 * <p>定义所有通过 NeoForge Attachment 系统挂载到玩家实体的数据类型：
 * <ul>
 *   <li>{@link #PARKOUR_CONTEXT} - 跑酷总数据上下文，参与磁盘和网络序列化</li>
 *   <li>{@link #SENSOR_DATA_MANAGER} - 传感器数据管理器，仅内存，不持久化</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourAttachmentTypes {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
			DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,
					ParkourMod.MODID);

	/** 跑酷总数据上下文附件，序列化到 NBT 并跨网络同步 */
	public static final Supplier<AttachmentType<ParkourContext>> PARKOUR_CONTEXT =
			ATTACHMENT_TYPES.register("parkour_context",
					() -> AttachmentType.builder(ParkourContext::new)
							.serialize(ParkourContext.CODEC.fieldOf(
									"parkour_context"))
							.sync(ParkourContext.STREAM_CODEC).build());

	/** 传感器数据管理器附件，仅内存存储不参与序列化 */
	public static final Supplier<AttachmentType<SensorDataManager>> SENSOR_DATA_MANAGER =
			ATTACHMENT_TYPES.register("sensor_data_manager",
					() -> AttachmentType.builder(SensorDataManager::new)
							.build());

	public static void register(IEventBus bus) {
		ATTACHMENT_TYPES.register(bus);
	}
}
