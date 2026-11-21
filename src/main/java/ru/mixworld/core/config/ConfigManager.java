package ru.mixworld.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.mixworld.core.lifecycle.LifecycleManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigManager implements LifecycleManager.Module {

    public record MixWorldConfig(String serverName, int autosaveMinutes) {
        public static MixWorldConfig defaults() {
            return new MixWorldConfig("MixWorld", 5);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dir;
    private final Path file;
    private volatile MixWorldConfig current = MixWorldConfig.defaults();

    public ConfigManager(Path dir) {
        this.dir = dir;
        this.file = dir.resolve("config.json");
    }

    /** O(1) read from the tick - just a volatile reference. */
    public MixWorldConfig get() {
        return current;
    }

    @Override
    public void load() throws IOException {
        Files.createDirectories(dir);
        if (Files.notExists(file)) {
            current = MixWorldConfig.defaults();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            MixWorldConfig loaded = GSON.fromJson(reader, MixWorldConfig.class);
            current = loaded != null ? loaded : MixWorldConfig.defaults();
        }
    }

    @Override
    public void save() throws IOException {
        Files.createDirectories(dir);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(current, writer);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
