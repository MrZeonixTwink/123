package ru.mixworld.core.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;

final class LuckPermsBridge {

    private LuckPermsBridge() {}

    /** Reads only the cached user data - does not block the tick. */
    static boolean check(UUID uuid, String node) {
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(uuid);
        return user != null
                && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
    }
}
