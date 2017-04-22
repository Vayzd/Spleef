/*
 * This file is part of Vayzd-Spleef, licenced under the MIT Licence (MIT)
 *
 * Copyright (c) Vayzd Network <https://www.vayzd.net/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.vayzd.spleef;

import lombok.*;
import net.vayzd.spleef.datastore.*;
import net.vayzd.spleef.listener.*;
import net.vayzd.spleef.player.*;
import org.bukkit.plugin.java.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Getter
public class SpleefPlugin extends JavaPlugin {

    private final ConcurrentMap<UUID, SpleefSpectator> spectatorMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playtimeMap = new HashMap<>();
    private final AtomicInteger onlineCount = new AtomicInteger(0);
    private DataStore dataStore;
    private MapControl mapControl;
    @Getter(AccessLevel.NONE)
    private final AtomicReference<GamePhase> phaseReference = new AtomicReference<>(null);

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            getLogger().info("Connecting to database...");
            dataStore = SpleefDataStore.getDataStore(this, getConfig());
            dataStore.connect();
            getLogger().info("Success!");
        } catch (SQLException ex) {
            getLogger().warning("Unable to connect to database! Disabling...");
            setEnabled(false);
        }
        mapControl = new MapControl(!getServer().getVersion().contains("1.11"));
        //register listener
        getServer().getPluginManager().registerEvents(mapControl, this);
        //once everything is loaded -> set phase to SERVER_EMPTY
        setGamePhase(GamePhase.SERVER_EMPTY);
    }

    @Override
    public void onDisable() {
        //for now reset map when plugin is being disabled
        mapControl.resetMap();
        dataStore.disconnect();
    }

    public final GamePhase getGamePhase() {
        return phaseReference.get();
    }

    public void setGamePhase(GamePhase newGamePhase) {
        phaseReference.set(newGamePhase);
    }

    public boolean isGamePhaseEqualTo(GamePhase phaseToCheck) {
        return phaseToCheck.equals(getGamePhase());
    }
}
