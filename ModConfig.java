package com.visualsmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    public static ModConfig I = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("visualsmod.json"); }

    // Visuals
    public boolean zoom = true;
    public float zoomStrength = 0.75f;   // 0.1 - 1
    public float zoomSmooth = 0.7f;      // 0 - 1 (higher = silkier)
    public boolean vignette = true;
    public float vignetteStrength = 0.5f;
    public boolean tint = false;
    public float tintStrength = 0.12f;
    public int tintColor = 0;            // preset index

    // Combat (legit HUD only)
    public boolean cps = true, keystrokes = false, combo = true, armor = true, totems = false;

    // Blocks / world info
    public boolean targetBlock = true, coords = false, facing = false, light = false;

    public static void load() {
        try { if (Files.exists(path())) I = GSON.fromJson(Files.readString(path()), ModConfig.class); }
        catch (Exception ignored) { }
        if (I == null) I = new ModConfig();
    }
    public static void save() {
        try { Files.writeString(path(), GSON.toJson(I)); } catch (Exception ignored) { }
    }
}
