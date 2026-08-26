package com.vizor.screenrec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;

public final class Recorder {

    private static Process ffmpeg;
    private static OutputStream ffmpegIn;
    private static Thread writerThread;
    private static volatile boolean recording = false;

    private static int recWidth;
    private static int recHeight;
    private static long startTimeMs;

    private static final BlockingQueue<byte[]> QUEUE = new ArrayBlockingQueue<>(120);

    private Recorder() {
    }

    public static boolean isRecording() {
        return recording;
    }

    public static synchronized boolean start(int width, int height) {
        if (recording) return false;
        if (!findFfmpeg()) return false;

        recWidth = width;
        recHeight = height;
        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "recording_" + ts + ".mp4";
        java.io.File outDir = new java.io.File(
                FabricLoader.getInstance().getGameDir().toFile(), "recordings");
        outDir.mkdirs();
        java.io.File outFile = new java.io.File(outDir, fileName);

        try {
            String ffmpegPath = findFfmpegPath();
            String[] cmd = new String[]{
                ffmpegPath,
                "-y",
                "-f", "rawvideo",
                "-pix_fmt", "bgr0",
                "-s", width + "x" + height,
                "-r", "60",
                "-i", "-",
                "-c:v", "libx264",
                "-vf", "vflip",
                "-preset", "ultrafast",
                "-pix_fmt", "yuv420p",
                "-crf", "23",
                outFile.getAbsolutePath()
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            ffmpeg = p;
            ffmpegIn = p.getOutputStream();

            writerThread = new Thread(() -> {
                try {
                    InputStream err = p.getInputStream();
                    byte[] buf = new byte[1024];
                    while (err.read(buf) != -1) {
                    }
                } catch (IOException ignored) {
                }
            }, "ffmpeg-stderr");
            writerThread.setDaemon(true);
            writerThread.start();

            Thread pipeThread = new Thread(() -> {
                try {
                    while (recording) {
                        byte[] frame = QUEUE.poll(500, TimeUnit.MILLISECONDS);
                        if (frame != null) {
                            ffmpegIn.write(frame);
                            ffmpegIn.flush();
                        }
                    }
                } catch (IOException | InterruptedException ignored) {
                }
                try {
                    ffmpegIn.close();
                } catch (IOException ignored) {
                }
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {
                }
            }, "ffmpeg-pipe");
            pipeThread.setDaemon(true);
            pipeThread.start();

            recording = true;
            startTimeMs = System.currentTimeMillis();
            System.out.println("[screenrec] Запись начата: " + outFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.err.println("[screenrec] Ошибка запуска FFmpeg: " + e);
            stop();
            return false;
        }
    }

    public static synchronized void stop() {
        if (!recording) return;
        recording = false;
        QUEUE.clear();

        long ms = System.currentTimeMillis() - startTimeMs;
        int sec = (int) (ms / 1000);
        int min = sec / 60;
        sec = sec % 60;
        int frames = (int) ((ms / 1000.0) * 60);
        System.out.println("[screenrec] Запись остановлена. Длительность: " + min + "м " + sec + "с");

        try {
            if (ffmpegIn != null) {
                ffmpegIn.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (ffmpeg != null) {
                ffmpeg.destroy();
            }
        } catch (Exception ignored) {
        }
        ffmpeg = null;
        ffmpegIn = null;
    }

    public static void grabFrame(int width, int height, java.nio.ByteBuffer pixelBuffer) {
        if (!recording) return;
        if (width != recWidth || height != recHeight) return;

        int size = width * height * 4;
        byte[] frame = new byte[size];
        pixelBuffer.position(0);
        pixelBuffer.get(frame);

        if (!QUEUE.offer(frame)) {
            // очередь полна — пропускаем кадр чтобы не тормозить
        }
    }

    public static boolean findFfmpeg() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "ffmpeg");
            Process p = pb.start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String findFfmpegPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "ffmpeg");
            Process p = pb.start();
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception ignored) {
        }
        return "ffmpeg";
    }
}
