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
package net.vayzd.spleef.listener;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.*;

public class MapControl implements Listener {

    private final LinkedList<BlockFace> toCheck = new LinkedList<>(asList(
            BlockFace.NORTH,
            BlockFace.NORTH_EAST,
            BlockFace.EAST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH,
            BlockFace.SOUTH_WEST,
            BlockFace.WEST,
            BlockFace.NORTH_WEST
    ));
    private final Queue<Location> toReset = new ConcurrentLinkedQueue<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        final Block origin = event.getHitBlock();
        if (origin.getType().equals(Material.TNT)) {
            toCheck.forEach(blockFace -> {
                Block relative = origin.getRelative(blockFace);
                if (relative.getType().equals(Material.TNT)) {
                    Location location = relative.getLocation().clone();
                    if (!toReset.contains(location)) {
                        toReset.add(location);
                    }
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Location location = event.getBlock().getLocation();
        if (!toReset.contains(location)) {
            toReset.add(location.clone());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(true);
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.setCancelled(true);
        event.blockList().clear();
    }

    public void resetMap() { //invoke method when game ends. custom event to listen upon?
        synchronized (toReset) {
            Iterator<Location> iterator = toReset.iterator();
            while (iterator.hasNext()) {
                Location location = iterator.next();
                if (location.getBlock().getType().equals(Material.AIR)) {
                    location.getBlock().setType(Material.TNT);
                    iterator.remove();
                }
            }
        }
    }
}
