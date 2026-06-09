package mod.arcomit.parkour.content.behavior.wallclimb.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 爬墙动画修改器 —— 协调速度状态机、骨骼计算和骨骼覆写。
 *
 * <p>继承 {@link SpeedModifier} 以获得动画速度控制能力。
 * 每帧 setupAnim 计算身体偏航补偿值，tick 更新动画速度，get3DTransform 覆写 body 骨骼旋转。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class WallClimbPlayerAnimModifier extends SpeedModifier {

	private final Player player;
	private final WallClimbAnimState animState = new WallClimbAnimState();
	private int lastUpdateTick = -1;

	private WallClimbAnimMath.FrameData currentFrameData;

	/**
	 * @param player 动画所属玩家，不能为 null，初始动画速度为 1.0
	 */
	public WallClimbPlayerAnimModifier(Player player) {
		super(1.0f);
		this.player = player;
	}

	/**
	 * 每帧动画开始前计算身体偏航角补偿值，使后续 get3DTransform 使用。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		this.currentFrameData =
				WallClimbAnimMath.calculate(this.player, state.getPartialTick());
		super.setupAnim(state);
	}

	/**
	 * 每游戏 tick 根据玩家垂直位移更新动画播放速度，同一 tick 内不重复计算。
	 */
	@Override
	public void tick(AnimationData state) {
		if (this.player.tickCount != this.lastUpdateTick) {
			this.lastUpdateTick = this.player.tickCount;
			this.speed = this.animState.calculateLerpedSpeed(this.player, this.speed);
		}
		super.tick(state);
	}

	/**
	 * 在 "body" 骨骼上追加偏航角补偿，使身体视觉朝向与墙体法线一致。
	 *
	 * @param bone 待修改的骨骼，不能为 null
	 * @return 修改后的骨骼（与原 bone 为同一对象），currentFrameData 为 null 时原样返回
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (this.currentFrameData == null)
			return bone;

		if (bone.getName().equals("body")) {
			bone.rotY += this.currentFrameData.bodyYawDiff;
		}

		return bone;
	}
}
