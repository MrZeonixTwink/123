package ru.mixworld.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mixworld.core.command.MixWorldCommand;
import ru.mixworld.core.lifecycle.LifecycleManager;

public final class MixWorldMod implements ModInitializer {

    public static final String MOD_ID = "mixworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static LifecycleManager lifecycle;

    @Override
    public void onInitialize() {
        lifecycle = new LifecycleManager();
        lifecycle.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MixWorldCommand.register(dispatcher, lifecycle));

        LOGGER.info("MixWorld Core initialized");
    }

    public static LifecycleManager lifecycle() {
        return lifecycle;
    }
}
