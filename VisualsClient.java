package com.visualsmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    private KeyBinding menuKey;
    private boolean lastParen;
    private int prevSlot;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("visualsmod", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.visualsmod.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));
        ZoomHandler.zoomKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.visualsmod.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) Hud.hit();
            return ActionResult.PASS;
        });
    }

    private void tick(MinecraftClient c) {
        if (c.player == null) return;
        Hud.tick(c);

        // "(" has no physical key: on most layouts it's Shift + 9
        long w = c.getWindow().getHandle();
        boolean shift = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean paren = shift && GLFW.glfwGetKey(w, GLFW.GLFW_KEY_9) == GLFW.GLFW_PRESS;

        boolean open = false;
        while (menuKey.wasPressed()) open = true;
        if (paren && !lastParen) open = true;
        lastParen = paren;

        if (open && c.currentScreen == null) {
            c.player.getInventory().setSelectedSlot(prevSlot); // undo the hotbar hop from pressing 9
            c.setScreen(new MenuScreen());
        }
        prevSlot = c.player.getInventory().getSelectedSlot();
    }
}
