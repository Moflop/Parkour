package mod.arcomit.parkour.content.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 客户端跳跃状态追踪器。
 *
 * <p>精确追踪本地玩家跳跃的各个阶段：起跳瞬间、是否在空中（已起跳但未落地）、
 * 以及跳跃键释放后的宽限期。这些数据供垂挂（armhang）等跑酷状态判断
 * 玩家的跳跃时机是否允许进入特定动作。
 *
 * <p>仅客户端运行，追踪精确到 tick 级别。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientJumpStateHandler {
	public static final int JUMP_KEY_RELEASE_GRACE_PERIOD = 6;// 跳跃键松开后仍然判定为按下的宽限期

	/**
	 * 本地玩家起跳时记录跳跃时间戳和已起跳标记。
	 *
	 * <p>副作用：重置 {@code ticksSinceLastJump} 为 0，设置 {@code jumped}=true。
	 */
	@SubscribeEvent
	public static void onJump(LivingEvent.LivingJumpEvent event) {
		if (event.getEntity() instanceof LocalPlayer player) {
			JumpData jumpData = ParkourContext.get(player).jump();
			jumpData.setTicksSinceLastJump(0);// 设置距离上一次跳跃的Tick
			jumpData.setJumped(true);// 设置已起跳过（用于armhang）
		}
	}

	/**
	 * 本地玩家落地时清除已起跳标记。
	 *
	 * <p>仅在检测到 {@code player.onGround()} 为 true 且之前标记为已起跳时执行。
	 */
	@SubscribeEvent
	public static void onGound(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof LocalPlayer player)) {
			return;
		}
		JumpData jumpData = ParkourContext.get(player).jump();
		if (!player.onGround()) {
			return;
		}
		if (jumpData.isJumped()) {
			jumpData.setJumped(false);
		}
	}

	/**
	 * 追踪跳跃键输入状态，维护松开后的宽限计时。
	 *
	 * <p>按住跳跃键时重置宽限期为{@link #JUMP_KEY_RELEASE_GRACE_PERIOD}（6 tick），
	 * 松开后每 tick 递减。宽限期内其他跑酷动作仍视为"跳跃键正按下"，
	 * 避免因按键释放与逻辑评估之间的帧差导致误判。
	 */
	@SubscribeEvent
	public static void onJumpInput(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof LocalPlayer player) {
			ParkourContext context = ParkourContext.get(player);
			JumpData jumpData = context.jump();
			if (player.input.jumping) {
				// 如果一直按着跳跃键，重置宽限期
				jumpData.setJumpReleaseGraceTicks(JUMP_KEY_RELEASE_GRACE_PERIOD);
			} else {
				int grace = jumpData.getJumpReleaseGraceTicks();
				if (grace > 0) {
					jumpData.setJumpReleaseGraceTicks(grace - 1);
				}
			}
		}
	}
}
