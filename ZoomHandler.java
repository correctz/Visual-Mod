package com.visualsmod;

import net.minecraft.client.option.KeyBinding;

public class ZoomHandler {
    public static KeyBinding zoomKey;
    private static float current = 1f;
    private static long last = System.nanoTime();

    /** FOV multiplier, eased every frame so zoom in/out is silky. */
    public static float multiplier() {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1_000_000_000f, 0.1f);
        last = now;
        ModConfig c = ModConfig.I;
        float target = (c.zoom && zoomKey != null && zoomKey.isPressed()) ? 1f - 0.9f * c.zoomStrength : 1f;
        float speed = 3f + (1f - c.zoomSmooth) * 22f; // smoother = slower approach
        current += (target - current) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(current - target) < 0.0005f) current = target;
        return current;
    }
}
