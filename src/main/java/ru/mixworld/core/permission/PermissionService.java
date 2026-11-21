package ru.mixworld.core.permission;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Permission checks via LuckPerms (optional).
 * Console/RCON always pass; players pass only through LuckPerms nodes.
 */
public final class PermissionService {

    private final boolean luckPerms = FabricLoader.getInstance().isModLoaded("luckperms");

    public boolean has(ServerCommandSource source, String node) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return true;
        }
        // LuckPermsBridge is a separate class so there is no NoClassDefFoundError without LuckPerms
        return luckPerms && LuckPermsBridge.check(player.getUuid(), node);
    }
}
