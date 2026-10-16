package mod.arcomit.parkour.compat.smartkeyprompts;

import com.mafuyu404.smartkeyprompts.Config;
import com.mafuyu404.smartkeyprompts.init.HUD;
import com.mafuyu404.smartkeyprompts.init.KeyPrompt;
import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ClientParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.init.ClientParkourKeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Loaded explicitly on the client only after Smart Key Prompts has been found. */
public final class SmartKeyPromptsCompat {
    private static final Map<String, KeyPrompt> PROMPTS = new LinkedHashMap<>();
    private static List<String> activeIds = List.of();
    private static boolean tutorialKeyHeld;
    private static final ParkourTutorialRules.Tip TOGGLE_TIP = new ParkourTutorialRules.Tip(
            "toggle", List.of(new ParkourTutorialRules.Key(ParkourTutorialRules.Binding.TUTORIAL, false)), false);

    private SmartKeyPromptsCompat() {}

    public static void register() {
        // SKP consumes and clears its input cache at NORMAL priority in this event.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, SmartKeyPromptsCompat::tick);
    }

    /** Selected, bound tutorial actions; SKP retains control of its blacklist and visibility. */
    public static List<String> activePromptIds() {
        return activeIds;
    }

    private static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.player != null && mc.level != null && mc.screen == null
                && mc.getOverlay() == null && !mc.isPaused();
        // Consume outside the enabled gate so the same shortcut can restore the tutorial.
        boolean toggle = false;
        while (ClientParkourKeyBindings.TUTORIAL_KEY.consumeClick()) toggle = true;
        if (toggle && !tutorialKeyHeld && inGame) {
            boolean enabled = ClientParkourConfig.toggleTutorialPrompts();
            mc.player.displayClientMessage(Component.translatable(enabled
                    ? "message.parkour.tutorial.enabled" : "message.parkour.tutorial.disabled"), true);
        }
        // GLFW_REPEAT adds new clicks while held; one physical chord press toggles once.
        tutorialKeyHeld = ClientParkourKeyBindings.TUTORIAL_KEY.isDown();
        var next = new LinkedHashMap<String, KeyPrompt>();
        if (ClientParkourConfig.enableTutorialPrompts && inGame && mc.player.isAlive() && !mc.options.hideGui) {
            initializeHudDefault();
            var tips = new ArrayList<ParkourTutorialRules.Tip>();
            if (ClientParkourConfig.enableParkour) tips.addAll(ParkourTutorialRules.select(mc.player).tips());
            tips.add(TOGGLE_TIP);
            for (var tip : tips) {
                List<KeyMapping> bindings = tip.keys().stream().map(key -> mapping(mc, key.binding())).toList();
                if (tip.alternatives()) {
                    bindings = bindings.stream().filter(binding -> !binding.isUnbound()).toList();
                } else if (bindings.stream().anyMatch(KeyMapping::isUnbound)) {
                    continue;
                }
                if (bindings.isEmpty()) continue;

                String alias = String.join(tip.alternatives() ? " / " : " + ",
                        bindings.stream().map(binding -> binding.getTranslatedKeyMessage().getString()).distinct().toList());
                // SKP understands chords, but not OR expressions. An alternatives row uses
                // the held alternative for its pressed appearance and shows both in its alias.
                List<KeyMapping> physicalBindings = tip.alternatives()
                        ? List.of(bindings.stream().filter(KeyMapping::isDown).findFirst().orElse(bindings.getFirst()))
                        : bindings;
                String keys = physicalKeys(mc, physicalBindings);
                String description = Component.translatable(descriptionKey(mc, tip)).getString();
                KeyPrompt prompt = PROMPTS.get(tip.id());
                if (prompt == null || !keys.equals(prompt.key) || !description.equals(prompt.desc)) {
                    prompt = new KeyPrompt(ParkourMod.MODID, keys, description, true, alias);
                } else {
                    // SKP's render cache ignores alias changes in equals/hashCode and keeps
                    // references to these objects. Reusing them also handles language changes.
                    prompt.keyAlias = alias;
                }
                next.put(tip.id(), prompt);
            }
        }

        // Remove stale hints even while SKP's control key freezes its displayed list.
        // Leave prompts belonging to other mods and SKP's own controls untouched.
        HUD.KeyPromptCache.removeIf(prompt -> ParkourMod.MODID.equals(prompt.group));
        synchronized (HUD.KeyPromptList) {
            HUD.KeyPromptList.removeIf(prompt -> ParkourMod.MODID.equals(prompt.group)
                    && !next.containsValue(prompt));
        }
        PROMPTS.clear();
        PROMPTS.putAll(next);
        activeIds = List.copyOf(next.keySet());
        next.values().forEach(prompt -> prompt.toGroup(ParkourMod.MODID));
    }

    private static String descriptionKey(Minecraft mc, ParkourTutorialRules.Tip tip) {
        // These actions need a fresh jump press, or (for the climbing brake) jump released.
        // Read the current mapping so remapped keys and input released this tick are respected.
        boolean releaseJump = switch (tip.id()) {
            case "bar_jump", "wall_jump", "speed_vault_jump", "entity_jump", "climb_brake" -> mc.options.keyJump.isDown();
            default -> false;
        };
        return tip.translationKey() + (releaseJump ? ".release" : "");
    }

    private static void initializeHudDefault() {
        if (ClientParkourConfig.TUTORIAL_HUD_POSITION_INITIALIZED.get()) return;
        // SKP shares one HUD position across groups. Migrate its untouched default once,
        // preserving custom positions, scale, blacklist and all later user adjustments.
        Config current = Config.get();
        if (current.position() == Config.UNIT.getDefaultValue().position()) {
            Config.UNIT.setValue(new Config(current.scale(), 8, current.blacklist()));
            Config.UNIT.save();
        }
        ClientParkourConfig.TUTORIAL_HUD_POSITION_INITIALIZED.set(true);
        ClientParkourConfig.TUTORIAL_HUD_POSITION_INITIALIZED.save();
    }

    private static String physicalKeys(Minecraft mc, List<KeyMapping> bindings) {
        var names = new LinkedHashSet<String>();
        for (KeyMapping binding : bindings) {
            var modifiers = binding.getKeyModifier().codes();
            if (modifiers.length > 0) {
                InputConstants.Key modifier = Arrays.stream(modifiers).filter(candidate -> InputConstants.isKeyDown(
                        mc.getWindow().getWindow(), candidate.getValue())).findFirst().orElse(modifiers[0]);
                names.add(modifier.getName());
            }
            names.add(binding.getKey().getName());
        }
        return String.join("+", names);
    }

    private static KeyMapping mapping(Minecraft mc, ParkourTutorialRules.Binding binding) {
        return switch (binding) {
            case JUMP -> mc.options.keyJump;
            case SNEAK -> mc.options.keyShift;
            case SPRINT -> mc.options.keySprint;
            case SLIDE -> ClientParkourKeyBindings.SLIDE_KEY;
            case FORWARD -> mc.options.keyUp;
            case BACK -> mc.options.keyDown;
            case LEFT -> mc.options.keyLeft;
            case RIGHT -> mc.options.keyRight;
            case TUTORIAL -> ClientParkourKeyBindings.TUTORIAL_KEY;
        };
    }
}
