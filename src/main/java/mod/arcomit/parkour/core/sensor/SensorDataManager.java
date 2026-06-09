package mod.arcomit.parkour.core.sensor;

import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * 传感器数据管理器，集中管理所有传感器类型对应的{@link SensorData}实例。
 * <p>
 * 每个玩家通过 NeoForge 附件系统（{@code ParkourAttachmentTypes.SENSOR_DATA_MANAGER}） 挂载一个
 * SensorDataManager，由传感器实现通过 {@link #get(Player)} 获取后 再按{@link SensorType}取对应方向的缓存数据。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SensorDataManager {
	private final Map<SensorType, SensorData> sensorDataMap = new EnumMap<>(SensorType.class);

	public SensorDataManager() {
		for (SensorType sensorType : SensorType.values()) {
			sensorDataMap.put(sensorType, new SensorData());
		}
	}

	/**
	 * 将 SensorDataManager 挂载到玩家附件上。
	 * <p>副作用：写入 NeoForge 附件数据，覆盖已有值。</p>
	 *
	 * @param player      目标玩家，不能为 null
	 * @param dataManager 要挂载的实例，不能为 null
	 */
	public static void set(Player player, SensorDataManager dataManager) {
		player.setData(ParkourAttachmentTypes.SENSOR_DATA_MANAGER, dataManager);
	}

	/**
	 * 从玩家附件读取 SensorDataManager。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 挂载的 SensorDataManager 实例；若玩家尚未初始化传感器系统可能为 null
	 */
	public static SensorDataManager get(Player player) {
		return player.getData(ParkourAttachmentTypes.SENSOR_DATA_MANAGER);
	}

	/**
	 * 获取指定传感器类型的数据载体。
	 *
	 * @param sensorType 传感器类型，不能为 null
	 * @return 对应的 SensorData 实例，构造时已初始化全部水平方向，永不为 null
	 */
	public SensorData getData(SensorType sensorType) {
		return sensorDataMap.get(sensorType);
	}
}
