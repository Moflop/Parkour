package mod.arcomit.parkour.content.behavior.wallslide.client.animation.player;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 负责维护墙壁滑行的高级状态机（如起步判定、背对姿态权重等）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlideAnimState {
	public boolean isFirstTick = true;
	public boolean isBackPose = false;
	public float prevBackWeight = 0f;
	public float currentBackWeight = 0f;

	/**
	 * 每 tick 更新滑墙动画状态。
	 *
	 * <p>根据玩家身体偏航角与墙体法线的夹角判定当前是面朝墙还是背对墙：
	 * 夹角大于 130 度切换为背对姿态，小于 110 度切回面朝姿态（滞回区间防止抖动）。 第一帧直接锁定目标姿态以避免动画突变。backWeight 以 0.25 步长平滑过渡。
	 *
	 * @param player 目标玩家，不能为 null
	 */
	public void tick(Player player) {
		this.prevBackWeight = this.currentBackWeight;

		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		int dirIndex = wallMovementData.getSlideRaw();

		if (dirIndex >= 0 && dirIndex <= 5) {
			Direction wallDir = Direction.from3DDataValue(dirIndex);
			if (wallDir != null) {
				Vec3 intoWallVec = new Vec3(-wallDir.getStepX(), 0,
						-wallDir.getStepZ());
				float targetWorldYaw = (float) Math.toDegrees(
						Math.atan2(-intoWallVec.x, intoWallVec.z)) + 180f;
				float yawToWall = Mth.wrapDegrees(targetWorldYaw - player.yBodyRot);
				float absYaw = Math.abs(yawToWall);

				// 【核心保留】：动画加载第一帧直接锁定目标姿势
				if (this.isFirstTick) {
					this.isFirstTick = false;
					if (absYaw > 130f) {
						this.isBackPose = true;
						this.currentBackWeight = 1.0f;
						this.prevBackWeight = 1.0f;
					} else {
						this.isBackPose = false;
						this.currentBackWeight = 0.0f;
						this.prevBackWeight = 0.0f;
					}
				} else {
					// 正常迟滞判定
					if (!this.isBackPose && absYaw > 130f) {
						this.isBackPose = true;
					} else if (this.isBackPose && absYaw < 110f) {
						this.isBackPose = false;
					}
				}
			}
		}

		float targetWeight = this.isBackPose ? 1.0f : 0.0f;
		float transitionSpeed = 0.25f;

		if (this.currentBackWeight < targetWeight) {
			this.currentBackWeight =
					Math.min(1.0f, this.currentBackWeight + transitionSpeed);
		} else if (this.currentBackWeight > targetWeight) {
			this.currentBackWeight =
					Math.max(0.0f, this.currentBackWeight - transitionSpeed);
		}
	}
}
