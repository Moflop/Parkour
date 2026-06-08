package mod.arcomit.parkour.content.event;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * 跳跃前触发，取消后实体不会执行跳跃动作。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class LivingJumpCancellableEvent extends LivingEvent implements ICancellableEvent {
	public LivingJumpCancellableEvent(LivingEntity entity) {
		super(entity);
	}
}
