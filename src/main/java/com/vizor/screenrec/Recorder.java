package com.vizor.screenrec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import net.fabricmc.loader.api.FabricLoader;

public final class Recorder {

    private static Process ffmpeg;
    private static OutputStream ffmpegIn;
    private static Thread pipeThread;
    private static Thread errThread;
    private static volatile boolean recording = false;

    private static int recWidth;
    private static int recHeight;
    private static long startTimeMs;
    private static volatile long lastDurationMs;

    private static final BlockingQueue<byte[]> QUEUE = new ArrayBlockingQueue<>(120);
    private static final BlockingQueue<byte[]> FREE = new ArrayBlockingQueue<>(120);
    private static volatile String ffmpegPathCache;

    private Recorder() {
    }

    public static boolean isRecording() {
        return recording;
    }

    public static long getElapsedMs() {
        return recording ? System.currentTimeMillis() - startTimeMs : lastDurationMs;
    }

    public static long getLastDurationMs() {
        return lastDurationMs;
    }

    public static synchronized boolean start(int width, int height) {
        if (recording) return false;
        if (width <= 0 || height <= 0) return false;

        String ffmpegPath = findFfmpegPath();
        if (ffmpegPath == null) return false;

        recWidth = width;
        recHeight = height;

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File outDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "recordings");
        if (!outDir.mkdirs() && !outDir.isDirectory()) {
            System.err.println("[screenrec] Не удалось создать папку recordings");
            return false;
        }
        File outFile = new File(outDir, "recording_" + ts + ".mp4");

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

        try {
            QUEUE.clear();
            FREE.clear();

            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            ffmpeg = p;
            ffmpegIn = p.getOutputStream();

            // recording=true ДО старта потоков, иначе pipe-поток сразу выйдет
            // из while (recording) и закроет stdin, не записав ни одного кадра.
            recording = true;
            startTimeMs = System.currentTimeMillis();
            lastDurationMs = 0L;

            errThread = new Thread(() -> {
                try {
                    InputStream out = p.getInputStream();
                    byte[] buf = new byte[1024];
                    while (out.read(buf) != -1) {
                        // читаем, чтобы FFmpeg не блокировался на переполненном буфере
                    }
                } catch (IOException ignored) {
                }
            }, "screenrec-ffmpeg-log");
            errThread.setDaemon(true);
            errThread.start();

            pipeThread = new Thread(() -> {
                try {
                    while (true) {
                        byte[] frame = QUEUE.poll(200, TimeUnit.MILLISECONDS);
                        if (frame == null) {
                            if (!recording) break;
                            continue;
                        }
                        ffmpegIn.write(frame);
                        FREE.offer(frame);
                    }
                } catch (Exception ignored) {
                }
                try {
                    ffmpegIn.close();
                } catch (IOException ignored) {
                }
            }, "screenrec-ffmpeg-pipe");
            pipeThread.setDaemon(true);
            pipeThread.start();

            System.out.println("[screenrec] Запись начата: " + outFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.err.println("[screenrec] Ошибка запуска FFmpeg: " + e);
            recording = false;
            cleanup();
            return false;
        }
    }

    public static synchronized void stop() {
        if (!recording) {
            cleanup();
            return;
        }
        lastDurationMs = System.currentTimeMillis() - startTimeMs;
        recording = false;
        QUEUE.clear();

        try {
            if (pipeThread != null) {
                pipeThread.join(3000);
            }
        } catch (InterruptedException ignored) {
        }

        cleanup();

        long sec = getElapsedMs() / 1000L;
        System.out.println("[screenrec] Запись остановлена. Длительность: " + (sec / 60) + "м " + (sec % 60) + "с");
    }

    private static void cleanup() {
        try {
            if (ffmpegIn != null) {
                ffmpegIn.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (ffmpeg != null && !ffmpeg.waitFor(5, TimeUnit.SECONDS)) {
                ffmpeg.destroy();
                if (!ffmpeg.waitFor(2, TimeUnit.SECONDS)) {
                    ffmpeg.destroyForcibly();
                }
            }
        } catch (Exception ignored) {
        }
        ffmpeg = null;
        ffmpegIn = null;
        pipeThread = null;
        errThread = null;
        QUEUE.clear();
        FREE.clear();
    }

    public static void grabFrame(int width, int height, java.nio.ByteBuffer pixelBuffer) {
        if (!recording) return;
        if (width != recWidth || height != recHeight) return;

        int size = width * height * 4;
        byte[] frame = FREE.poll();
        if (frame == null || frame.length != size) {
            frame = new byte[size];
        }
        pixelBuffer.position(0);
        pixelBuffer.get(frame);

        if (!QUEUE.offer(frame)) {
            // очередь полна — возвращаем буфер в пул, кадр пропускаем
            FREE.offer(frame);
        }
    }

    public static boolean findFfmpeg() {
        return findFfmpegPath() != null;
    }

    private static String findFfmpegPath() {
        String cached = ffmpegPathCache;
        if (cached != null) return cached;

        String env = System.getenv("FFMPEG_PATH");
        if (env != null && new File(env).canExecute()) {
            ffmpegPathCache = env;
            return env;
        }

        String[] candidates = {
                "ffmpeg",
                "/usr/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "C:\\ffmpeg\\bin\\ffmpeg.exe"
        };
        for (String candidate : candidates) {
            try {
                Process p = new ProcessBuilder(candidate, "-version").start();
                if (p.waitFor(1000, TimeUnit.MILLISECONDS) && p.exitValue() == 0) {
                    ffmpegPathCache = candidate;
                    return candidate;
                }
                p.destroyForcibly();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
