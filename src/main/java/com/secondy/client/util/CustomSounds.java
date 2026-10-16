package com.secondy.client.util;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Свои звуки из папки {@code config/secondy_client/sounds/} (форматы WAV и OGG).
 * <p>
 * Декодирование и проигрывание идут в отдельном потоке через Java Sound, поэтому игра не подвисает
 * и не нужен ресурспак. Для каждого файла держится заранее открытый {@link Clip}: удар только
 * перематывает его на начало и запускает. Все поля ниже трогает только поток {@link #POOL}.
 */
public final class CustomSounds {
    /** Длиннее этого звук обрезается (хитсаунд — это доли секунды). */
    private static final int MAX_SECONDS = 5;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_CACHED = 16;

    private record Sample(AudioFormat format, byte[] pcm) {}

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "secondy-sounds");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, Sample> CACHE = new HashMap<>();
    private static Clip clip;
    private static String clipKey = "";
    private static String failedKey = "";

    private CustomSounds() {}

    // ───────────── Папка ─────────────

    public static Path folder() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("secondy_client").resolve("sounds");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) { }
        return dir;
    }

    /** Имена файлов .wav / .ogg в папке, по алфавиту. */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder())) {
            files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> {
                        String l = n.toLowerCase(Locale.ROOT);
                        return l.endsWith(".wav") || l.endsWith(".ogg");
                    })
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(out::add);
        } catch (IOException ignored) { }
        return out;
    }

    /** Открывает папку со звуками в проводнике. */
    public static void openFolder() {
        Path dir = folder();
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (os.contains("win")) pb = new ProcessBuilder("explorer.exe", dir.toString());
            else if (os.contains("mac")) pb = new ProcessBuilder("open", dir.toString());
            else pb = new ProcessBuilder("xdg-open", dir.toString());
            pb.start();
        } catch (IOException ignored) { }
    }

    // ───────────── Проигрывание ─────────────

    /** Заранее декодирует файл и открывает линию, чтобы первый удар не запаздывал. */
    public static void preload(String name, int volumePercent, Consumer<String> onError) {
        POOL.execute(() -> prepare(name, volumePercent, onError));
    }

    /** Проигрывает файл с громкостью 0..100 %. Повторный вызов перезапускает звук. */
    public static void play(String name, int volumePercent, Consumer<String> onError) {
        POOL.execute(() -> {
            if (!prepare(name, volumePercent, onError)) return;
            try {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            } catch (RuntimeException ignored) { }
        });
    }

    private static boolean prepare(String name, int volumePercent, Consumer<String> onError) {
        Path dir = folder().toAbsolutePath().normalize();
        Path path = dir.resolve(name).normalize();
        if (!path.startsWith(dir)) return false;

        String key = name;
        try {
            key = name + "|" + Files.getLastModifiedTime(path).toMillis() + "|" + volumePercent;
        } catch (IOException e) {
            if (!key.equals(failedKey)) {
                failedKey = key;
                onError.accept("Sound \"" + name + "\": file not found");
            }
            return false;
        }
        if (key.equals(clipKey) && clip != null) return true;
        if (key.equals(failedKey)) return false;

        try {
            String sampleKey = key.substring(0, key.lastIndexOf('|'));
            Sample s = CACHE.get(sampleKey);
            if (s == null) {
                if (Files.size(path) > MAX_FILE_BYTES) throw new IOException("file is larger than 10 MB");
                s = name.toLowerCase(Locale.ROOT).endsWith(".ogg") ? decodeOgg(path) : decodeWav(path);
                if (CACHE.size() >= MAX_CACHED) CACHE.clear();
                CACHE.put(sampleKey, s);
            }
            byte[] data = scale(s.pcm(), volumePercent);
            Clip c = AudioSystem.getClip();
            c.open(s.format(), data, 0, data.length);
            if (clip != null) clip.close();
            clip = c;
            clipKey = key;
            failedKey = "";
            return true;
        } catch (Exception | LinkageError t) {
            failedKey = key;
            onError.accept("Sound \"" + name + "\": " + describe(t));
            return false;
        }
    }

    private static String describe(Throwable t) {
        if (t instanceof UnsupportedAudioFileException) return "unsupported format (WAV or OGG required)";
        if (t instanceof IllegalArgumentException) return "unsupported format or audio device";
        if (t instanceof LinkageError) return "decoder unavailable in this build";
        String m = t.getMessage();
        return m == null || m.isBlank() ? t.getClass().getSimpleName() : m;
    }

    /** Volume: умножаем 16-битные сэмплы (little-endian) — работает на любом аудиоустройстве. */
    private static byte[] scale(byte[] pcm, int volumePercent) {
        float g = Math.max(0f, Math.min(1f, volumePercent / 100f));
        if (g >= 0.999f) return pcm;
        byte[] out = new byte[pcm.length];
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int v = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            v = Math.round(v * g);
            out[i] = (byte) v;
            out[i + 1] = (byte) (v >> 8);
        }
        return out;
    }

    // ───────────── Декодеры ─────────────

    /** WAV любого типа, который понимает JDK (PCM 8/16/24/32, float, a-law/µ-law) → PCM 16 бит. */
    private static Sample decodeWav(Path path) throws Exception {
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat f = raw.getFormat();
            if (f.getChannels() < 1 || f.getChannels() > 2) throw new IOException("mono or stereo audio required");
            AudioInputStream pcm = raw;
            if (f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || f.getSampleSizeInBits() != 16 || f.isBigEndian()) {
                AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
                        f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
                pcm = AudioSystem.getAudioInputStream(target, raw);
            }
            AudioFormat fmt = pcm.getFormat();
            int frame = fmt.getFrameSize();
            long max = (long) (fmt.getSampleRate() * MAX_SECONDS) * frame;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[frame * 2048];
            int n;
            while (bos.size() < max && (n = pcm.read(buf, 0, (int) Math.min(buf.length, max - bos.size()))) > 0) {
                bos.write(buf, 0, n);
            }
            byte[] data = bos.toByteArray();
            int len = data.length - data.length % frame;
            if (len == 0) throw new IOException("file is empty");
            if (len != data.length) data = java.util.Arrays.copyOf(data, len);
            return new Sample(fmt, data);
        }
    }

    /** OGG Vorbis через stb_vorbis из LWJGL (он уже есть в Minecraft). */
    private static Sample decodeOgg(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer mem = BufferUtils.createByteBuffer(bytes.length);
        mem.put(bytes).flip();
        IntBuffer error = BufferUtils.createIntBuffer(1);
        long dec = STBVorbis.stb_vorbis_open_memory(mem, error, null);
        if (dec == 0L) throw new IOException("failed to read OGG (code " + error.get(0) + ")");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(dec, info);
            int channels = info.channels();
            int rate = info.sample_rate();
            if (channels < 1 || channels > 2) throw new IOException("mono or stereo audio required");
            int total = STBVorbis.stb_vorbis_stream_length_in_samples(dec);
            if (total <= 0) throw new IOException("file is empty");
            int count = Math.min(total, rate * MAX_SECONDS);
            ShortBuffer pcm = BufferUtils.createShortBuffer(count * channels);
            int got = STBVorbis.stb_vorbis_get_samples_short_interleaved(dec, channels, pcm);
            if (got <= 0) throw new IOException("file is empty");
            byte[] out = new byte[got * channels * 2];
            for (int i = 0; i < got * channels; i++) {
                short s = pcm.get(i);
                out[i * 2] = (byte) s;
                out[i * 2 + 1] = (byte) (s >> 8);
            }
            return new Sample(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, channels * 2, rate, false), out);
        } finally {
            STBVorbis.stb_vorbis_close(dec);
        }
    }
}
