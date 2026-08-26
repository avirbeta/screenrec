package com.vizor.screenrec.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    private static ByteBuffer screenrec$pixelBuffer;

    @Inject(method = "render", at = @At("TAIL"))
    private void screenrec$grabFrame(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta,
            long nanoTime,
            boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmap,
            net.minecraft.util.math.Matrix4f projection,
            CallbackInfo ci) {

        if (!com.vizor.screenrec.Recorder.isRecording()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getFramebufferWidth();
        int h = client.getWindow().getFramebufferHeight();

        if (w <= 0 || h <= 0) return;

        int bufSize = w * h * 4;
        if (screenrec$pixelBuffer == null || screenrec$pixelBuffer.capacity() < bufSize) {
            screenrec$pixelBuffer = ByteBuffer.allocateDirect(bufSize);
        }

        screenrec$pixelBuffer.clear();
        screenrec$pixelBuffer.position(0);

        GL11.glReadPixels(0, 0, w, h, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenrec$pixelBuffer);

        com.vizor.screenrec.Recorder.grabFrame(w, h, screenrec$pixelBuffer);
    }
}
