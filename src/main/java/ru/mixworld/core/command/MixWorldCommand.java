package ru.mixworld.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import ru.mixworld.core.MixWorldMod;
import ru.mixworld.core.lifecycle.LifecycleManager;

public final class MixWorldCommand {

    private MixWorldCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LifecycleManager lifecycle) {
        dispatcher.register(CommandManager.literal("mixworld")
                .requires(src -> lifecycle.permissions().has(src, "mixworld.command"))
                .then(CommandManager.literal("reload")
                        .requires(src -> lifecycle.permissions().has(src, "mixworld.admin.reload"))
                        .executes(ctx -> reload(ctx, lifecycle))));
    }

    private static int reload(CommandContext<ServerCommandSource> ctx, LifecycleManager lifecycle) {
        ServerCommandSource source = ctx.getSource();
        long start = System.nanoTime();

        lifecycle.reloadAsync().whenComplete((ignored, error) ->
                // back to the server thread to send the message
                source.getServer().execute(() -> {
                    if (error != null) {
                        MixWorldMod.LOGGER.error("Reload failed", error);
                        source.sendError(Text.literal("[MixWorld] Reload failed, check the console."));
                        return;
                    }
                    long ms = (System.nanoTime() - start) / 1_000_000L;
                    source.sendFeedback(() -> Text.literal("[MixWorld] Reloaded in " + ms + " ms."), true);
                }));

        return Command.SINGLE_SUCCESS;
    }
}
