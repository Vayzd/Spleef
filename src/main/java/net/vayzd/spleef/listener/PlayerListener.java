package net.vayzd.spleef.listener;

import lombok.*;
import net.vayzd.spleef.*;
import net.vayzd.spleef.datastore.entry.*;
import net.vayzd.spleef.player.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.*;

import java.util.*;
import java.util.concurrent.atomic.*;

@RequiredArgsConstructor
public class PlayerListener implements Listener {

    private final AtomicBoolean isWaiting = new AtomicBoolean(true);
    private final SpleefPlugin plugin;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        final UUID uniqueId = event.getUniqueId();
        if (plugin.getGamePhase().isJoinable()) {
            if (plugin.getOnlineCount().get() >= plugin.getServer().getMaxPlayers()) {
                event.disallow(Result.KICK_OTHER, "§cThe server is full!"); // TODO: 22.04.17 configurable messages
                return;
            }
            SpleefSubject subject = plugin.getDataStore().getSubject(uniqueId);
            if (subject == null) {
                subject = new SpleefSubject();
                subject.setUniqueId(uniqueId);
                subject.setName(event.getName());
                plugin.getDataStore().insertSubject(subject);
            }
            plugin.getOnlineCount().incrementAndGet();
        } else if (plugin.getGamePhase().isSpectateable()) {
            plugin.getSpectatorMap().putIfAbsent(uniqueId, new SpleefSpectator());
            event.allow();
        } else {
            event.disallow(Result.KICK_OTHER, "§cAccess denied!"); // TODO: 22.04.17 configurable messages
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) { // in case another plugin cancels the login
            plugin.getOnlineCount().decrementAndGet();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("Hi"); // TODO: 22.04.17 configurable messages
        if (plugin.getGamePhase().equals(GamePhase.SERVER_EMPTY)) {
            plugin.setGamePhase(GamePhase.LOBBY);
        }
        if (plugin.getOnlineCount().get() >= 2 && isWaiting.get()) { // TODO: 22.04.17 make minimum player count configurable
            isWaiting.set(false);
            // TODO: 22.04.17 initialize lobby countdown
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uniqueId = event.getPlayer().getUniqueId();
        if (plugin.getGamePhase().isSpectateable() && plugin.getSpectatorMap().containsKey(uniqueId)) {
            plugin.getSpectatorMap().remove(uniqueId);
        }
        plugin.getOnlineCount().decrementAndGet();
    }
}
