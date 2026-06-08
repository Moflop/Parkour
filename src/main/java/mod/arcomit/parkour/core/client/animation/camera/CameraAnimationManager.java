package mod.arcomit.parkour.core.client.animation.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.TreeMap;

/**
 * 摄像机动画播放器，驱动 FOV、旋转和位移的缓动插值。
 * <p>
 * 同时维护动画时间轴——响应实时流逝推进进度、检测循环边界、处理游戏暂停时的时间冻结。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class CameraAnimationManager {
	public static final CameraAnimationManager INSTANCE = new CameraAnimationManager();

	private static final float[] EMPTY_FRAME = new float[] {0.0f, 0.0f, 0.0f};

	private CameraAnimation currentAnimation = null;
	private boolean isPlaying = false;
	private long lastUpdateTimeMs = 0;
	private float currentAnimationTime = 0.0f;

	private CameraAnimationManager() {
	}

	/**
	 * 播放指定动画。若目标动画不存在则输出错误日志并忽略。
	 *
	 * @param animationId 已注册的摄像机动画标识符，通过 CameraAnimationRegistry 加载
	 */
	public void play(ResourceLocation animationId) {
		CameraAnimation animation =
				CameraAnimationRegistry.INSTANCE.getAnimation(animationId);
		if (animation != null) {
			this.currentAnimation = animation;
			this.isPlaying = true;
			this.currentAnimationTime = 0.0f;
			this.lastUpdateTimeMs = System.currentTimeMillis();
		} else {
			System.err.println("尝试播放不存在的摄像机动画: " + animationId);
		}
	}

	/**
	 * 立即停止当前动画并清空播放状态。
	 */
	public void stop() {
		this.isPlaying = false;
		this.currentAnimation = null;
	}

	/**
	 * 每帧推进动画时间轴，同时处理游戏暂停时的时钟冻结。
	 * <p>
	 * 当游戏未暂停时，以实际流逝的秒数推进进度；
	 * 若游戏暂停（ESC 菜单等），则丢弃该段流逝时间，防止恢复时快进。
	 * 循环动画在播放完毕后自动回到周期开头继续播放。
	 */
	public void tick() {
		if (!isPlaying || currentAnimation == null) {
			return;
		}

		long now = System.currentTimeMillis();
		long deltaMs = now - lastUpdateTimeMs;

		// 无论是否暂停，每次都必须把 lastUpdateTimeMs 更新为现在，
		// 这样暂停期间度过的时间就会被"丢弃"，防止解除暂停时瞬间快进。
		this.lastUpdateTimeMs = now;

		if (!Minecraft.getInstance().isPaused()) {
			currentAnimationTime += deltaMs / 1000.0f;
		}

		if (currentAnimationTime > currentAnimation.getLength()) {
			if (currentAnimation.isLoop()) {
				// 循环播放：减去一个周期的长度，这样能保证循环动画绝对不会掉帧或产生误差
				currentAnimationTime -= currentAnimation.getLength();
			} else {
				stop();
			}
		}
	}

	/**
	 * 在当前时间点对旋转关键帧做线性插值。
	 *
	 * @return 长度为 3 的 float 数组 [pitch, yaw, roll]，不在播放时返回全零数组
	 */
	public float[] getCurrentRotation() {
		if (!isPlaying || currentAnimation == null) {
			return EMPTY_FRAME;
		}
		return interpolateFrames(currentAnimation.getRotations());
	}

	/**
	 * 在当前时间点对位移关键帧做线性插值。
	 *
	 * @return 长度为 3 的 float 数组 [x, y, z]，不在播放时返回全零数组
	 */
	public float[] getCurrentPosition() {
		if (!isPlaying || currentAnimation == null) {
			return EMPTY_FRAME;
		}
		return interpolateFrames(currentAnimation.getPositions());
	}

	/**
	 * 根据当前动画时间在两个相邻关键帧之间做线性插值。
	 *
	 * @param frames 按时间升序排列的关键帧 Map，键为时间点（秒）
	 * @return 插值后的三元 float 数组，关键帧为空时返回全零数组
	 */
	private float[] interpolateFrames(TreeMap<Float, float[]> frames) {
		if (frames.isEmpty()) {
			return EMPTY_FRAME;
		}

		Map.Entry<Float, float[]> floor = frames.floorEntry(currentAnimationTime);
		Map.Entry<Float, float[]> ceiling = frames.ceilingEntry(currentAnimationTime);

		if (floor == null) {
			return ceiling != null ? ceiling.getValue() : EMPTY_FRAME;
		}
		if (ceiling == null || floor.getKey().equals(ceiling.getKey())) {
			return floor.getValue();
		}

		float progress =
				(currentAnimationTime - floor.getKey()) / (ceiling.getKey() - floor.getKey());
		float[] start = floor.getValue();
		float[] end = ceiling.getValue();

		return new float[] {Mth.lerp(progress, start[0], end[0]),
				Mth.lerp(progress, start[1], end[1]),
				Mth.lerp(progress, start[2], end[2])};
	}

	public boolean isPlaying() {
		return isPlaying;
	}
}
