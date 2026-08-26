package com.vizor.screenrec;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class ScreenRecMod implements ClientModInitializer {

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.screenrec.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.screenrec"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                toggle(client);
            }
        });

        System.out.println("[screenrec] Загружен. Нажми R для записи экрана.");
    }

    private static void toggle(MinecraftClient client) {
        if (Recorder.isRecording()) {
            Recorder.stop();
            if (client.player != null) {
                client.player.sendMessage(
                        new LiteralText("§c[Запись] §fОстановлено"), true);
            }
        } else {
            if (!Recorder.findFfmpeg()) {
                if (client.player != null) {
                    client.player.sendMessage(
                            new LiteralText("§c[Запись] FFmpeg не найден! Установи FFmpeg."), true);
                }
                return;
            }
            int w = client.getWindow().getFramebufferWidth();
            int h = client.getWindow().getFramebufferHeight();
            boolean ok = Recorder.start(w, h);
            if (client.player != null) {
                if (ok) {
                    client.player.sendMessage(
                            new LiteralText("§a[Запись] §fЗапись начата (R для стопа)"), true);
                } else {
                    client.player.sendMessage(
                            new LiteralText("§c[Запись] Ошибка запуска"), true);
                }
            }
        }
    }
}
