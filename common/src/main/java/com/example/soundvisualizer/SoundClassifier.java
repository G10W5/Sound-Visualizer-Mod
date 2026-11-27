package com.example.soundvisualizer;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.util.Set;

/**
 * Maps a sound event to a HUD category. Vanilla entity sounds are resolved by
 * entity name; modded / unknown sounds fall back to their {@link SoundSource}.
 */
public final class SoundClassifier {
    private static final Set<String> HOSTILE_MOBS = Set.of(
            "zombie", "zombie_villager", "husk", "drowned", "skeleton", "stray", "bogged",
            "wither_skeleton", "creeper", "spider", "cave_spider", "enderman", "endermite",
            "silverfish", "ghast", "blaze", "witch", "slime", "magma_cube", "wither",
            "ender_dragon", "phantom", "pillager", "vindicator", "evoker", "illusioner",
            "ravager", "vex", "piglin", "piglin_brute", "zombified_piglin", "hoglin", "zoglin",
            "guardian", "elder_guardian", "shulker", "breeze", "warden", "creaking", "parched",
            "camel_husk", "hostile");

    private static final Set<String> FRIENDLY_MOBS = Set.of(
            "pig", "cow", "mooshroom", "chicken", "sheep", "horse", "donkey", "mule",
            "skeleton_horse", "zombie_horse", "llama", "trader_llama", "cat", "ocelot", "wolf",
            "fox", "bee", "parrot", "panda", "polar_bear", "turtle", "axolotl", "allay", "frog",
            "tadpole", "camel", "armadillo", "sniffer", "iron_golem", "snow_golem", "villager",
            "wandering_trader", "rabbit", "goat", "strider", "dolphin", "cod", "salmon",
            "tropical_fish", "pufferfish", "squid", "glow_squid", "bat", "happy_ghast",
            "nautilus", "copper_golem", "friendly");

    private static final Set<String> PLAYER_ENTITIES = Set.of(
            "player", "item", "experience_orb", "experience_bottle", "fishing_bobber", "arrow",
            "trident", "splash_potion", "lingering_potion", "ender_pearl", "egg", "snowball",
            "wind_charge");

    private SoundClassifier() {
    }

    public static boolean isFootstep(String namespace, String path) {
        return namespace.equals("presence_footsteps") || path.startsWith("pf/")
                || path.endsWith(".step") || path.contains(".step.") || path.contains("footstep")
                || path.startsWith("step.");
    }

    public static SoundCategory classify(Identifier id, SoundSource source) {
        String ns = id.getNamespace();
        String path = id.getPath().toLowerCase();

        // Entity sounds: "entity.<name>.<event>"
        if (path.startsWith("entity.")) {
            int end = path.indexOf('.', 7);
            String entity = end > 0 ? path.substring(7, end) : path.substring(7);
            if (HOSTILE_MOBS.contains(entity)) return SoundCategory.HOSTILE; // includes hostile footsteps
            if (FRIENDLY_MOBS.contains(entity)) {
                return isFootstep(ns, path) ? SoundCategory.NEUTRAL : SoundCategory.FRIENDLY;
            }
            if (PLAYER_ENTITIES.contains(entity)) return SoundCategory.PLAYER;
            if (entity.equals("generic") || entity.equals("tnt") || entity.equals("lightning_bolt")
                    || entity.equals("firework_rocket")) {
                return SoundCategory.BLOCKS;
            }
        }

        if (isFootstep(ns, path)) return SoundCategory.NEUTRAL;

        if (path.startsWith("ambient.") || path.startsWith("weather.") || path.startsWith("music")
                || path.endsWith(".ambient") || path.contains(".ambient.")) {
            return SoundCategory.AMBIENT;
        }

        if (path.startsWith("item.armor") || path.startsWith("item.elytra") || path.startsWith("item.shield")
                || path.startsWith("item.crossbow") || path.startsWith("item.totem")) {
            return SoundCategory.PLAYER;
        }

        if (path.startsWith("block.")) return SoundCategory.BLOCKS;

        // Modded / unknown: trust the sound source
        return switch (source) {
            case HOSTILE -> SoundCategory.HOSTILE;
            case PLAYERS -> SoundCategory.PLAYER;
            case NEUTRAL -> SoundCategory.FRIENDLY;
            case BLOCKS -> SoundCategory.BLOCKS;
            case AMBIENT, WEATHER, RECORDS -> SoundCategory.AMBIENT;
            default -> SoundCategory.NEUTRAL;
        };
    }
}
