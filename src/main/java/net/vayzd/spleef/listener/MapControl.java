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

import lombok.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.util.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.*;

@RequiredArgsConstructor
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
    private final boolean pre_1_11;
    private final Queue<Location> toReset = new ConcurrentLinkedQueue<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (pre_1_11) {
            Projectile projectile = event.getEntity();
            BlockIterator iterator = new BlockIterator(
                    projectile.getWorld(),
                    projectile.getLocation().toVector(),
                    projectile.getVelocity().normalize(),
                    0.0D,
                    4
            );
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (!block.getType().equals(Material.TNT)) {
                    continue;
                }
                checkForRelatives(block);
            }
        } else {
            Block origin = event.getHitBlock();
            if (origin.getType().equals(Material.TNT)) {
                checkForRelatives(origin);
            }
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

    private void checkForRelatives(final Block origin) {
        toReset.add(origin.getLocation().clone());
        toCheck.forEach(blockFace -> {
            Block relative = origin.getRelative(blockFace);
            if (relative.getType().equals(Material.TNT)) {
                toReset.add(relative.getLocation().clone());
            }
        });
    }

}
