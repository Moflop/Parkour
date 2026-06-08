package mod.arcomit.parkour.core.client.animation.camera;

import com.google.gson.annotations.SerializedName;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * 映射 Blockbench 导出的原始摄像机动画 JSON 结构。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class CameraAnimationDefinition {
	/**
	 * 动画总时长，单位秒。来自 Blockbench 的 animation_length 字段。
	 */
	@SerializedName("animation_length")
	public float animationLength;

	/**
	 * 是否循环播放。Blockbench 导出为字符串 "true"/"false"。
	 */
	@SerializedName("loop")
	public String loop;

	/**
	 * 骨骼动画数据容器，包含摄像机骨骼。
	 */
	@SerializedName("bones")
	public Bones bones;

	/**
	 * 骨骼分组，仅保留摄像机动画所需的 camera 骨骼。
	 */
	public static class Bones {
		@SerializedName("camera")
		public Camera camera;
	}

	/**
	 * 摄像机骨骼的关键帧数据。
	 */
	public static class Camera {
		/**
		 * 旋转关键帧。键为时间戳字符串（如 "0.0"），值为 [pitch, yaw, roll] 三轴旋转角度。
		 */
		@SerializedName("rotation")
		public Map<String, float[]> rotation;

		/**
		 * 位移关键帧。键为时间戳字符串（如 "0.0"），值为 [x, y, z] 三轴偏移量。
		 */
		@SerializedName("position")
		public Map<String, float[]> position;
	}
}
