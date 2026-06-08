package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 跑酷音效注册表。
 *
 * <p>通过 {@link DeferredRegister} 注册所有跑酷动作对应的 {@link SoundEvent}：
 * 滑铲、落地翻滚、蹬墙跳、垂挂、撑墙跳、速过。音效文件位于
 * {@code assets/parkour/sounds/} 下。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS =
			DeferredRegister.create(Registries.SOUND_EVENT, ParkourMod.MODID);

	public static final Supplier<SoundEvent> SLIDE = SOUNDS.register("slide",
			() -> SoundEvent.createVariableRangeEvent(ParkourMod.prefix("slide")));

	public static final Supplier<SoundEvent> LANDING_ROLL = SOUNDS.register("landing_roll",
			() -> SoundEvent.createVariableRangeEvent(
					ParkourMod.prefix("landing_roll")));

	public static final Supplier<SoundEvent> WALL_JUMP = SOUNDS.register("wall_jump",
			() -> SoundEvent.createVariableRangeEvent(ParkourMod.prefix("wall_jump")));

	public static final Supplier<SoundEvent> ARMHANG = SOUNDS.register("armhang",
			() -> SoundEvent.createVariableRangeEvent(ParkourMod.prefix("armhang")));

	public static final Supplier<SoundEvent> SUPPORT_WALL_JUMP =
			SOUNDS.register("support_wall_jump",
					() -> SoundEvent.createVariableRangeEvent(
							ParkourMod.prefix("support_wall_jump")));

	public static final Supplier<SoundEvent> SPEED_VAULT = SOUNDS.register("speed_vault",
			() -> SoundEvent.createVariableRangeEvent(
					ParkourMod.prefix("speed_vault")));

	public static void register(IEventBus bus) {
		SOUNDS.register(bus);
	}
}
