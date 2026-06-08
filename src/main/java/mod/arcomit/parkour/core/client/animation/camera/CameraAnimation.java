package mod.arcomit.parkour.core.client.animation.camera;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.TreeMap;

/**
 * 解析后的摄像机动画数据模型。 在加载阶段提前将时间戳字符串解析为浮点数，以确保播放时的高性能。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class CameraAnimation {
	private final ResourceLocation id;
	private final float length;
	private final boolean loop;
	private final TreeMap<Float, float[]> rotations;
	private final TreeMap<Float, float[]> positions;

	/**
	 * 从原始定义构造已解析的动画模型。
	 *
	 * @param id         动画的唯一标识符，通过资源包路径和动画名组合生成
	 * @param definition 从 JSON 反序列化得到的原始动画定义，不可为 null
	 */
	public CameraAnimation(ResourceLocation id, CameraAnimationDefinition definition) {
		this.id = id;
		this.length = definition.animationLength;
		this.loop = "true".equalsIgnoreCase(definition.loop);
		this.rotations = new TreeMap<>();
		this.positions = new TreeMap<>();

		if (definition.bones != null && definition.bones.camera != null) {
			parseKeyframes(definition.bones.camera.rotation, this.rotations);
			parseKeyframes(definition.bones.camera.position, this.positions);
		}
	}

	/**
	 * 将源关键帧 Map 中的时间戳字符串解析为浮点数后存入目标 TreeMap。
	 * 无法解析的键会被静默忽略。
	 *
	 * @param source 键为时间戳字符串（如 "0.0"、"1.5"），值为旋转/位移数组
	 * @param target 解析后的输出容器，按时间升序排列
	 */
	private void parseKeyframes(Map<String, float[]> source, TreeMap<Float, float[]> target) {
		if (source == null) {
			return;
		}
		for (Map.Entry<String, float[]> entry : source.entrySet()) {
			try {
				float time = Float.parseFloat(entry.getKey());
				target.put(time, entry.getValue());
			} catch (NumberFormatException e) {
				// 忽略无效的时间戳
			}
		}
	}

	/**
	 * @return 动画的唯一标识符，基于资源包路径和动画名拼接生成
	 */
	public ResourceLocation getId() {
		return id;
	}

	/**
	 * @return 动画总时长，单位秒
	 */
	public float getLength() {
		return length;
	}

	/**
	 * @return 播放完毕后是否从头循环
	 */
	public boolean isLoop() {
		return loop;
	}

	/**
	 * @return 旋转关键帧数据，键为时间点（秒），值为 [pitch, yaw, roll] 三轴旋转角度；无数据时返回空 Map
	 */
	public TreeMap<Float, float[]> getRotations() {
		return rotations;
	}

	/**
	 * @return 位移关键帧数据，键为时间点（秒），值为 [x, y, z] 三轴偏移量；无数据时返回空 Map
	 */
	public TreeMap<Float, float[]> getPositions() {
		return positions;
	}
}
