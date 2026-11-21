package ru.mixworld.core.lifecycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import ru.mixworld.core.MixWorldMod;
import ru.mixworld.core.config.ConfigManager;
import ru.mixworld.core.permission.PermissionService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * Mod lifecycle: server start/stop, single IO thread, module reload.
 * All disk reads/writes go through {@link #io()} so the server tick is never blocked.
 */
public final class LifecycleManager {

    /** A subsystem with on-disk data (balances, regions, players, ...). */
    public interface Module {
        default void load() throws IOException {}
        default void save() throws IOException {}
        default void onServerStarted(MinecraftServer server) {}
        default void onServerStopping(MinecraftServer server) {}
    }

    private final Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(MixWorldMod.MOD_ID);
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mixworld-io");
        t.setDaemon(true);
        return t;
    });

    private final ConfigManager config = new ConfigManager(dataDir);
    private final PermissionService permissions = new PermissionService();
    private final List<Module> modules = new CopyOnWriteArrayList<>(List.of(config));

    public void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            loadAll(); // server startup - blocking is fine here
            MixWorldMod.LOGGER.info("Modules loaded: {}", modules.size());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                modules.forEach(m -> m.onServerStarted(server)));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            modules.forEach(m -> m.onServerStopping(server));
            try {
                CompletableFuture.runAsync(this::saveAll, io).get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                MixWorldMod.LOGGER.error("Failed to save data on shutdown", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            io.shutdown();
            try {
                if (!io.awaitTermination(10, TimeUnit.SECONDS)) io.shutdownNow();
            } catch (InterruptedException e) {
                io.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    /** Async reload of all modules on the IO thread. */
    public CompletableFuture<Void> reloadAsync() {
        return CompletableFuture.runAsync(this::loadAll, io);
    }

    /** Async save of all modules on the IO thread. */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::saveAll, io);
    }

    private void loadAll() {
        for (Module m : modules) {
            try {
                m.load();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }
    }

    private void saveAll() {
        for (Module m : modules) {
            try {
                m.save();
            } catch (Exception e) {
                MixWorldMod.LOGGER.error("Failed to save {}", m.getClass().getSimpleName(), e);
            }
        }
    }

    public Executor io() { return io; }
    public ConfigManager config() { return config; }
    public PermissionService permissions() { return permissions; }
}
