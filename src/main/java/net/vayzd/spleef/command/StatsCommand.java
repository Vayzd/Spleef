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
package net.vayzd.spleef.command;

import com.google.common.base.*;
import lombok.*;
import net.vayzd.spleef.*;
import net.vayzd.spleef.datastore.entry.*;
import net.vayzd.spleef.player.*;
import org.bukkit.*;
import org.bukkit.entity.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static java.lang.String.*;

@RequiredArgsConstructor
public class StatsCommand implements PlayerCommand {

    private final SpleefPlugin plugin;

    @Override
    public void onCommand(Player player, String[] args) {
        if (args.length < 1) {
            plugin.sendPrefixedMessage(player, "§cUse: /stats (username)");
            return;
        }
        AtomicReference<String> username = new AtomicReference<>(args[0]);
        try {
            String argument = username.get();
            Preconditions.checkNotNull(argument, "Username argument is null!");
            Preconditions.checkArgument(
                    argument.length() >= 3,
                    "Invalid username! Username has less than 3 characters!"
            );
            Preconditions.checkArgument(
                    argument.length() <= 16,
                    "Invalid username! Username has more than 16 characters!"
            );
        } catch (NullPointerException ex) {
            plugin.sendPrefixedMessage(player, "§cNutze: /stats (username)");
            return;
        } catch (IllegalArgumentException ex) {
            plugin.sendPrefixedMessage(player, ChatColor.RED + ex.getMessage());
            return;
        }
        plugin.getDataStore().getSubject(username.get(), (SpleefSubject subject) -> {
            if (subject == null) {
                plugin.sendPrefixedMessage(player, format(
                        "§c%s has never played before!",
                        username.get())
                );
                return;
            }
            player.sendMessage(getOutputList(subject).toArray(new String[]{}));
        });
    }

    private LinkedList<String> getOutputList(SpleefSubject subject) {
        LinkedList<String> output = new LinkedList<>();
        output.add("§a");
        subject.getStatisticMap().forEach((key, value) -> output.add(key + value));
        output.add("§a");
        return output;
    }
}
