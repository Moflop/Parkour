package mod.arcomit.parkour.content.behavior.wallrun.client.animation.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 跑墙动画的独立状态机，维护动画相位和振幅的实时更新与平滑过渡。
 *
 * <p>本地玩家通过 WASD 输入强度判定运动量，远程玩家通过坐标位移推算。
 * 相位匀速推进驱动四肢正弦摆动，振幅在运动中渐增至 1.0、静止时渐减至 0。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallRunAnimState {
    /** 上一帧的动画相位 */
    public float animPhaseO = 0f;
    /** 当前动画相位，持续累加以驱动正弦波摆动 */
    public float animPhase = 0f;
    /** 上一帧的振幅 */
    public float amplitudeO = 0f;
    /** 当前振幅，范围 [0, 1]，控制四肢摆动幅度 */
    public float amplitude = 0f;

    /**
     * 每客户端帧推进动画状态：备份上一帧数据，根据移动强度更新相位和振幅。
     *
     * <p>相位以固定步长 0.6662 累加；振幅以 0.2 权重向目标值平滑插值。
     *
     * @param player 目标玩家，不能为 null
     */
    public void tick(Player player) {
		this.animPhaseO = this.animPhase;
		this.amplitudeO = this.amplitude;

		float movementIntensity = 0f;
		if (player instanceof LocalPlayer localPlayer) {
			movementIntensity = Math.max(Math.abs(localPlayer.input.forwardImpulse),
					Math.abs(localPlayer.input.leftImpulse));
		} else {
			double dx = player.getX() - player.xo;
			double dz = player.getZ() - player.zo;
			movementIntensity = (float) Math.sqrt(dx * dx + dz * dz) * 10.0f;
		}

		if (movementIntensity > 0.05f) {
			this.animPhase += 0.6662f;
			this.amplitude = Mth.lerp(0.2f, this.amplitude, 1.0f);
		} else {
			this.amplitude = Mth.lerp(0.2f, this.amplitude, 0f);
		}
	}
}
