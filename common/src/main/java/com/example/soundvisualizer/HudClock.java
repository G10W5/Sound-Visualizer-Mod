package com.example.soundvisualizer;

/**
 * Animation clock that stops while the game is paused, so indicators
 * don't silently fade away behind the pause menu.
 */
public final class HudClock {
    private static long lastNanos = System.nanoTime();
    private static double gameMs = 0.0;
    private static float frameSeconds = 0.0f;

    private HudClock() {
    }

    /** Advance once per rendered frame. */
    public static void tick(boolean paused) {
        long now = System.nanoTime();
        double dtMs = Math.min(250.0, (now - lastNanos) / 1_000_000.0);
        lastNanos = now;
        frameSeconds = paused ? 0.0f : (float) (dtMs / 1000.0);
        if (!paused) {
            gameMs += dtMs;
        }
    }

    public static long now() {
        return (long) gameMs;
    }

    /** Seconds elapsed during the last frame (0 while paused). */
    public static float frameSeconds() {
        return frameSeconds;
    }
}
