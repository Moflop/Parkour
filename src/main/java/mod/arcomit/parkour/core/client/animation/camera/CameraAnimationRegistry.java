package mod.arcomit.parkour.core.client.animation.camera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 摄像机动画的集中注册与生命周期管理。
 * <p>
 * 实现 {@link ResourceManagerReloadListener}，在每次资源包重载时扫描 {@code camera_animations} 目录下所有 .json 文件，解析为
 * {@link CameraAnimation} 并缓存。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class CameraAnimationRegistry implements ResourceManagerReloadListener {
	public static final CameraAnimationRegistry INSTANCE = new CameraAnimationRegistry();
	private static final Gson GSON = new Gson();

	private static final String ANIMATION_DIRECTORY = "camera_animations";
	private final Map<ResourceLocation, CameraAnimation> animations = new HashMap<>();

	private CameraAnimationRegistry() {
	}

	/**
	 * 资源包重载回调：清空已有动画缓存，重新从所有数据包中扫描并解析动画定义。 动画 ID 格式为 {@code modid:camera_animations/文件路径/动画名}。
	 * 解析失败的文件会打印堆栈并跳过。
	 */
	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		animations.clear();
		Map<ResourceLocation, Resource> resources =
				resourceManager.listResources(ANIMATION_DIRECTORY,
						path -> path.getPath().endsWith(".json"));

		for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
			ResourceLocation fileLocation = entry.getKey();
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

				if (!root.has("animations")) {
					continue;
				}

				JsonObject animationsObject = root.getAsJsonObject("animations");
				for (String animationName : animationsObject.keySet()) {
					CameraAnimationDefinition definition =
							GSON.fromJson(animationsObject.get(
											animationName),
									CameraAnimationDefinition.class);

					// 生成唯一标识符，格式为 "modid:camera_animations/文件名/动画名"
					String validPath =
							fileLocation.getPath() + "/" + animationName.toLowerCase();
					ResourceLocation animationId =
							ResourceLocation.fromNamespaceAndPath(
									fileLocation.getNamespace(),
									validPath);

					animations.put(animationId, new CameraAnimation(animationId,
							definition));
				}
			} catch (Exception e) {
				System.err.println("加载摄像机动画失败: " + fileLocation);
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param id 动画标识符，格式为 {@code modid:camera_animations/路径/动画名}
	 * @return 对应的已解析动画，未注册时返回 null
	 */
	public CameraAnimation getAnimation(ResourceLocation id) {
		return animations.get(id);
	}
}
