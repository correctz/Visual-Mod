package com.visualsmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Legit, information-only HUD. Nothing here gives a gameplay advantage. */
public class Hud {
    private static final ArrayDeque<Long> lmb = new ArrayDeque<>(), rmb = new ArrayDeque<>();
    private static boolean lastL, lastR;
    private static int combo, lastHurt;
    private static long lastHit;
    public static final int[] TINTS = {0xFF9A3C, 0x4FA8FF, 0xA040FF, 0xFF5FA2};
    public static final String[] TINT_NAMES = {"Warm", "Cool", "Purple", "Pink"};

    public static void hit() { combo++; lastHit = System.currentTimeMillis(); }

    public static void tick(MinecraftClient c) {
        long now = System.currentTimeMillis();
        boolean l = c.options.attackKey.isPressed(), r = c.options.useKey.isPressed();
        if (l && !lastL) lmb.add(now);
        if (r && !lastR) rmb.add(now);
        lastL = l; lastR = r;
        while (!lmb.isEmpty() && now - lmb.peekFirst() > 1000) lmb.pollFirst();
        while (!rmb.isEmpty() && now - rmb.peekFirst() > 1000) rmb.pollFirst();
        int h = c.player.hurtTime;
        if (h > 0 && lastHurt == 0) combo = 0;
        lastHurt = h;
        if (combo > 0 && now - lastHit > 2500) combo = 0;
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.world == null || c.options.hudHidden) return;
        ModConfig cfg = ModConfig.I;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        TextRenderer tr = c.textRenderer;

        if (cfg.vignette) vignette(ctx, w, h, cfg.vignetteStrength);
        if (cfg.tint) {
            int a = (int) (cfg.tintStrength * 255);
            ctx.fill(0, 0, w, h, (a << 24) | TINTS[cfg.tintColor % TINTS.length]);
        }

        List<String> lines = new ArrayList<>();
        if (cfg.cps) lines.add("CPS: " + lmb.size() + " | " + rmb.size());
        if (cfg.combo && combo > 1) lines.add("Combo: " + combo);
        if (cfg.totems) {
            int n = 0;
            for (int i = 0; i < c.player.getInventory().size(); i++) {
                ItemStack s = c.player.getInventory().getStack(i);
                if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
            }
            lines.add("Totems: " + n);
        }
        if (cfg.coords) lines.add(String.format("XYZ: %.1f / %.1f / %.1f", c.player.getX(), c.player.getY(), c.player.getZ()));
        if (cfg.facing) {
            String f = c.player.getHorizontalFacing().asString();
            lines.add("Facing: " + Character.toUpperCase(f.charAt(0)) + f.substring(1));
        }
        if (cfg.light) lines.add("Light: " + c.world.getLightLevel(c.player.getBlockPos()));
        if (cfg.targetBlock && c.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            String name = c.world.getBlockState(bhr.getBlockPos()).getBlock().getName().getString();
            double d = Math.sqrt(c.player.squaredDistanceTo(Vec3d.ofCenter(bhr.getBlockPos())));
            lines.add(String.format("Block: %s (%.1fm)", name, d));
        }
        int y = 6;
        for (String s : lines) { ctx.drawTextWithShadow(tr, s, 6, y, 0xFFE9D5FF); y += 11; }

        if (cfg.armor) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int ay = 6;
            for (EquipmentSlot sl : slots) {
                ItemStack st = c.player.getEquippedStack(sl);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, w - 22, ay);
                if (st.isDamageable()) {
                    String d = String.valueOf(st.getMaxDamage() - st.getDamage());
                    ctx.drawTextWithShadow(tr, d, w - 26 - tr.getWidth(d), ay + 4, 0xFFE9D5FF);
                }
                ay += 18;
            }
        }

        if (cfg.keystrokes) {
            int x = 6, ky = h / 2 - 20;
            key(ctx, tr, "W", x + 20, ky, c.options.forwardKey.isPressed());
            key(ctx, tr, "A", x, ky + 20, c.options.leftKey.isPressed());
            key(ctx, tr, "S", x + 20, ky + 20, c.options.backKey.isPressed());
            key(ctx, tr, "D", x + 40, ky + 20, c.options.rightKey.isPressed());
        }
    }

    private static void key(DrawContext ctx, TextRenderer tr, String s, int x, int y, boolean down) {
        ctx.fill(x, y, x + 18, y + 18, down ? 0xCCA855F7 : 0x88000000);
        ctx.drawCenteredTextWithShadow(tr, s, x + 9, y + 5, 0xFFFFFFFF);
    }

    private static void vignette(DrawContext ctx, int w, int h, float strength) {
        int t = Math.max(8, (int) (Math.min(w, h) * 0.22f));
        int n = 22;
        float step = (float) t / n;
        for (int i = 0; i < n; i++) {
            float f = 1f - (float) i / n;
            int a = (int) (strength * 200 * f * f);
            if (a <= 0) continue;
            int col = (a << 24) | 0x7A1FD6;
            int a0 = Math.round(i * step), a1 = Math.round((i + 1) * step);
            ctx.fill(0, a0, w, a1, col);               // top
            ctx.fill(0, h - a1, w, h - a0, col);       // bottom
            ctx.fill(a0, 0, a1, h, col);               // left
            ctx.fill(w - a1, 0, w - a0, h, col);       // right
        }
    }
}
