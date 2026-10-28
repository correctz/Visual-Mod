package com.visualsmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class MenuScreen extends Screen {
    private static final int W = 420, H = 250, SIDE = 96, PAGE = 5;
    private static int lastTab = 0;
    private static final String[] TABS = {"Visuals", "Combat", "Blocks", "Resource Packs"};
    private static final String[] PVP_WORDS = {"pvp", "bedwars", "16x", "fps", "lowfire", "low fire", "sword", "uhc", "crystal"};

    private int tab = lastTab, packPage = 0, cx, cy, cw;
    private boolean noPacks;
    private final long openedAt = System.nanoTime();

    public MenuScreen() { super(Text.literal("Visuals Menu")); }

    private int px() { return (width - W) / 2; }
    private int py() { return (height - H) / 2; }
    private float progress() {
        float p = Math.min(1f, (System.nanoTime() - openedAt) / 380_000_000f);
        return 1f - (1f - p) * (1f - p) * (1f - p); // ease-out cubic
    }

    @Override
    protected void init() {
        int x = px(), y = py();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(new Btn(x + 8, y + 44 + i * 28, SIDE - 16, 22, Text.literal(TABS[i]),
                    b -> { tab = idx; lastTab = idx; packPage = 0; clearAndInit(); }, tab == i));
        }
        cx = x + SIDE + 12; cy = y + 40; cw = (W - SIDE - 32) / 2;
        noPacks = false;
        switch (tab) {
            case 0 -> initVisuals();
            case 1 -> initCombat();
            case 2 -> initBlocks();
            default -> initPacks();
        }
    }

    // ---------- tabs ----------
    private void initVisuals() {
        ModConfig c = ModConfig.I;
        MinecraftClient mc = MinecraftClient.getInstance();
        toggle(0, "Smooth Zoom (C)", () -> c.zoom, v -> c.zoom = v);
        slider(1, "Zoom Strength", 0.1, 1, c.zoomStrength, v -> c.zoomStrength = (float) v);
        slider(2, "Zoom Smoothness", 0, 1, c.zoomSmooth, v -> c.zoomSmooth = (float) v);
        toggle(3, "Purple Vignette", () -> c.vignette, v -> c.vignette = v);
        slider(4, "Vignette Strength", 0, 1, c.vignetteStrength, v -> c.vignetteStrength = (float) v);
        toggle(5, "Screen Tint", () -> c.tint, v -> c.tint = v);
        slider(6, "Tint Strength", 0, 0.4, c.tintStrength, v -> c.tintStrength = (float) v);
        addDrawableChild(new Btn(slot(7)[0], slot(7)[1], cw, 22, Text.literal("Tint: " + Hud.TINT_NAMES[c.tintColor % 4]),
                b -> { c.tintColor = (c.tintColor + 1) % 4; b.setMessage(Text.literal("Tint: " + Hud.TINT_NAMES[c.tintColor])); }, false));
        toggle(8, "View Bobbing", () -> mc.options.getBobView().getValue(), v -> mc.options.getBobView().setValue(v));
        optSlider(9, "Hurt Cam", mc.options.getDamageTiltStrength());
        optSlider(10, "FOV Effects", mc.options.getFovEffectScale());
        optSlider(11, "Brightness", mc.options.getGamma());
    }

    private void initCombat() {
        ModConfig c = ModConfig.I;
        toggle(0, "CPS Counter", () -> c.cps, v -> c.cps = v);
        toggle(1, "Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
        toggle(2, "Combo Counter", () -> c.combo, v -> c.combo = v);
        toggle(3, "Armor Durability", () -> c.armor, v -> c.armor = v);
        toggle(4, "Totem Counter", () -> c.totems, v -> c.totems = v);
    }

    private void initBlocks() {
        ModConfig c = ModConfig.I;
        toggle(0, "Target Block Info", () -> c.targetBlock, v -> c.targetBlock = v);
        toggle(1, "Coordinates", () -> c.coords, v -> c.coords = v);
        toggle(2, "Facing Direction", () -> c.facing, v -> c.facing = v);
        toggle(3, "Light Level", () -> c.light, v -> c.light = v);
    }

    // name, tag, link
    private static final String[][] POPULAR = {
        {"Faithful 32x", "Visual", "https://modrinth.com/discover/resourcepacks?q=Faithful+32x"},
        {"Bare Bones", "Visual / FPS", "https://modrinth.com/discover/resourcepacks?q=Bare+Bones"},
        {"Fresh Animations", "Visual", "https://modrinth.com/discover/resourcepacks?q=Fresh+Animations"},
        {"Vanilla Tweaks", "PvP / Visual", "https://vanillatweaks.net/picker/resource-packs/"},
        {"Mizuno's 16 Craft", "PvP", "https://modrinth.com/discover/resourcepacks?q=Mizuno+16+Craft"},
        {"Low Fire", "PvP", "https://modrinth.com/discover/resourcepacks?q=low+fire"},
        {"Short Swords", "PvP", "https://modrinth.com/discover/resourcepacks?q=short+swords"},
        {"Soartex Fanver", "Visual", "https://modrinth.com/discover/resourcepacks?q=Soartex+Fanver"},
        {"Stay True", "Visual", "https://modrinth.com/discover/resourcepacks?q=Stay+True"},
        {"Default Dark Mode", "Visual", "https://modrinth.com/discover/resourcepacks?q=Default+Dark+Mode"},
        {"More PvP packs", "Search", "https://modrinth.com/discover/resourcepacks?q=pvp"},
        {"More Visual packs", "Search", "https://modrinth.com/discover/resourcepacks?q=realistic"},
    };
    private static int packView = 0; // 0 = popular, 1 = installed

    private void initPacks() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int full = cw * 2 + 8;
        addDrawableChild(new Btn(cx + full - 150, cy - 10, 72, 14, Text.literal("Popular"), b -> { packView = 0; packPage = 0; clearAndInit(); }, packView == 0));
        addDrawableChild(new Btn(cx + full - 74, cy - 10, 74, 14, Text.literal("Installed"), b -> { packView = 1; packPage = 0; clearAndInit(); }, packView == 1));
        int by = cy + 4 + PAGE * 24 + 6;
        int count;
        if (packView == 0) {
            count = POPULAR.length;
            int pages = (count + PAGE - 1) / PAGE;
            packPage = Math.min(packPage, pages - 1);
            for (int i = 0; i < PAGE; i++) {
                int idx = packPage * PAGE + i;
                if (idx >= count) break;
                String[] e = POPULAR[idx];
                addDrawableChild(new Btn(cx, cy + 4 + i * 24, full, 22, Text.literal(e[0] + "  -  " + e[1] + "   (open page)"),
                        b -> Util.getOperatingSystem().open(URI.create(e[2])), false));
            }
            addDrawableChild(new Btn(cx, by, cw, 22, Text.literal("< Prev"), b -> { packPage = Math.max(0, packPage - 1); clearAndInit(); }, false));
            addDrawableChild(new Btn(cx + cw + 8, by, cw, 22, Text.literal("Next >"), b -> { packPage = Math.min(pages - 1, packPage + 1); clearAndInit(); }, false));
            noPacks = false;
            return;
        }
        ResourcePackManager m = mc.getResourcePackManager();
        List<ResourcePackProfile> all = new ArrayList<>();
        for (ResourcePackProfile p : m.getProfiles()) if (p.getId().startsWith("file/")) all.add(p);
        all.sort((a, b) -> Boolean.compare(isPvp(b), isPvp(a)));
        noPacks = all.isEmpty();
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        packPage = Math.min(packPage, pages - 1);
        for (int i = 0; i < PAGE; i++) {
            int idx = packPage * PAGE + i;
            if (idx >= all.size()) break;
            ResourcePackProfile p = all.get(idx);
            String id = p.getId();
            boolean on = m.getEnabledIds().contains(id);
            String label = (isPvp(p) ? "\u2605 " : "") + mc.textRenderer.trimToWidth(p.getDisplayName().getString(), full - 60);
            addDrawableChild(new Btn(cx, cy + 4 + i * 24, full, 22, Text.literal(label + "   " + (on ? "ON" : "OFF")), b -> {
                if (m.getEnabledIds().contains(id)) m.disable(id); else m.enable(id);
                mc.options.refreshResourcePacks(m);
                clearAndInit();
            }, on));
        }
        addDrawableChild(new Btn(cx, by, cw - 30, 22, Text.literal("Open Pack Folder"),
                b -> Util.getOperatingSystem().open(mc.getResourcePackDir().toUri()), false));
        addDrawableChild(new Btn(cx + cw - 26, by, 24, 22, Text.literal("<"), b -> { packPage = Math.max(0, packPage - 1); clearAndInit(); }, false));
        addDrawableChild(new Btn(cx + cw + 2, by, 24, 22, Text.literal(">"), b -> { packPage = Math.min(pages - 1, packPage + 1); clearAndInit(); }, false));
        addDrawableChild(new Btn(cx + cw + 32, by, cw - 24, 22, Text.literal("Find PvP Packs"),
                b -> Util.getOperatingSystem().open(URI.create("https://modrinth.com/discover/resourcepacks?q=pvp")), false));
    }

    private static boolean isPvp(ResourcePackProfile p) {
        String n = (p.getDisplayName().getString() + p.getId()).toLowerCase();
        for (String k : PVP_WORDS) if (n.contains(k)) return true;
        return false;
    }

    // ---------- widget helpers ----------
    private int[] slot(int i) { return new int[]{cx + (i % 2) * (cw + 8), cy + (i / 2) * 26}; }

    private static Text label(String n, boolean on) { return Text.literal(n + ": " + (on ? "ON" : "OFF")); }

    private void toggle(int i, String n, BooleanSupplier get, Consumer<Boolean> set) {
        int[] p = slot(i);
        addDrawableChild(new Btn(p[0], p[1], cw, 22, label(n, get.getAsBoolean()), b -> {
            boolean v = !get.getAsBoolean();
            set.accept(v);
            b.setMessage(label(n, v));
            ((Btn) b).lit = v;
        }, get.getAsBoolean()));
    }

    private void slider(int i, String n, double min, double max, double cur, DoubleConsumer set) {
        int[] p = slot(i);
        addDrawableChild(new Sld(p[0], p[1], cw, 22, n, min, max, cur, set));
    }

    private void optSlider(int i, String n, SimpleOption<Double> opt) {
        slider(i, n, 0, 1, opt.getValue(), opt::setValue);
    }

    // ---------- rendering ----------
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* no vanilla blur */ }
    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float p = progress();
        ctx.fill(0, 0, width, height, ((int) (110 * p) << 24) | 0x05000A);
        int x = px(), y = py(), vis = (int) (H * p);
        ctx.enableScissor(x - 4, y - 4, x + W + 4, y + vis);
        ctx.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFFA855F7);                 // purple border
        ctx.fillGradient(x, y, x + W, y + H, 0xFF1C0A33, 0xFF040208);           // purple -> black
        ctx.fill(x, y, x + SIDE, y + H, 0xCC07030F);                              // sidebar
        ctx.fill(x + SIDE, y, x + SIDE + 1, y + H, 0x66A855F7);
        ctx.drawTextWithShadow(textRenderer, "VISUALS", x + 12, y + 14, 0xFFC084FC);
        ctx.drawTextWithShadow(textRenderer, "MENU", x + 12, y + 25, 0xFF7C3AED);
        ctx.drawTextWithShadow(textRenderer, TABS[tab].toUpperCase(), cx, y + 14, 0xFFE9D5FF);
        ctx.fill(cx, y + 28, x + W - 12, y + 29, 0x55A855F7);
        if (tab == 3) {
            ctx.drawTextWithShadow(textRenderer, packView == 0 ? "Popular PvP + visual packs" : "Installed (\u2605 = PvP-style)", cx, cy - 7, 0xFFA78BFA);
            if (packView == 1 && noPacks) ctx.drawTextWithShadow(textRenderer, "No packs installed yet - grab one from Popular, drop it in the folder.", cx, cy + 16, 0xFF9CA3AF);
        }
        ctx.drawTextWithShadow(textRenderer, "ESC to close", x + W - 12 - textRenderer.getWidth("ESC to close"), y + H - 14, 0xFF6B21A8);
        super.render(ctx, mx, my, delta);
        ctx.disableScissor();
    }

    @Override
    public void removed() {
        ModConfig.save();
        MinecraftClient.getInstance().options.write();
    }

    // ---------- custom widgets ----------
    static class Btn extends ButtonWidget {
        boolean lit;
        Btn(int x, int y, int w, int h, Text t, PressAction a, boolean lit) {
            super(x, y, w, h, t, a, DEFAULT_NARRATION_SUPPLIER);
            this.lit = lit;
        }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            boolean hov = isHovered();
            int bg = lit ? (hov ? 0xFF8B3FE0 : 0xFF6D28D9) : (hov ? 0xFF2A1450 : 0xFF150A28);
            c.fill(getX(), getY(), getX() + width, getY() + height, bg);
            c.fill(getX(), getY(), getX() + 2, getY() + height, lit ? 0xFFE9D5FF : 0xFF7C3AED);
            MinecraftClient mc = MinecraftClient.getInstance();
            c.drawCenteredTextWithShadow(mc.textRenderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        }
    }

    static class Sld extends SliderWidget {
        private final String name; private final double min, max; private final DoubleConsumer set;
        Sld(int x, int y, int w, int h, String name, double min, double max, double cur, DoubleConsumer set) {
            super(x, y, w, h, Text.empty(), Math.max(0, Math.min(1, (cur - min) / (max - min))));
            this.name = name; this.min = min; this.max = max; this.set = set;
            updateMessage();
        }
        @Override protected void updateMessage() {
            if (name == null) return;
            setMessage(Text.literal(name + ": " + (int) Math.round(value * 100) + "%"));
        }
        @Override protected void applyValue() { set.accept(min + (max - min) * value); }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            c.fill(getX(), getY(), getX() + width, getY() + height, isHovered() ? 0xFF2A1450 : 0xFF150A28);
            c.fill(getX(), getY(), getX() + (int) (width * value), getY() + height, 0x886D28D9);
            int kx = getX() + (int) ((width - 3) * value);
            c.fill(kx, getY(), kx + 3, getY() + height, 0xFFE9D5FF);
            MinecraftClient mc = MinecraftClient.getInstance();
            c.drawCenteredTextWithShadow(mc.textRenderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        }
    }
}
