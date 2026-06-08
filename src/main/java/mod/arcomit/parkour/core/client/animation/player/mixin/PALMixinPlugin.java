package mod.arcomit.parkour.core.client.animation.player.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 条件 Mixin 插件，检测 Player Animation Library (PAL) 是否已安装。
 * <p>
 * 由于跑酷模组的玩家动画完全依赖 PAL 作为底层骨骼动画引擎，
 * 若 PAL 未安装则所有针对 PAL 内部类的 Mixin 将无法找到目标类。
 * 本插件在 {@code shouldApplyMixin} 中检查 {@code player_animation_library} 的 Mod ID，
 * 仅当 PAL 已加载时才启用本包的 Mixin，从而兼容无 PAL 的客户端环境。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class PALMixinPlugin implements IMixinConfigPlugin {

	/**
	 * Mixin 配置加载回调。当前无初始化逻辑。
	 */
	@Override
	public void onLoad(String mixinPackage) {
	}

	/**
	 * @return 当前未指定自定义 refmap，返回 null 使用 Mixin 默认行为
	 */
	@Override
	public String getRefMapperConfig() {
		return null;
	}

	/**
	 * 核心条件判断：仅当 PAL 模组已安装时才应用本包内的所有 Mixin。
	 *
	 * @param targetClassName 目标类的全限定名
	 * @param mixinClassName  Mixin 类的全限定名
	 * @return PAL 已加载时返回 true，否则返回 false
	 */
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return LoadingModList.get().getModFileById("player_animation_library") != null;
	}

	@Override
	public void acceptTargets(Set<String> set, Set<String> set1) {
	}

	/**
	 * @return 当前由 JSON 配置文件声明 Mixin 列表，无需程序化返回，故返回 null
	 */
	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
	}

	@Override
	public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
	}

}
