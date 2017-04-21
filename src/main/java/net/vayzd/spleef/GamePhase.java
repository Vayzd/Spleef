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

/**
 * This class represents phases for this game.
 */
@RequiredArgsConstructor
@Getter
public enum GamePhase {

    /**
     * Set when the plugin has been enabled successfully.
     * Zero players online, yet ready to accept new players.
     */
    SERVER_EMPTY(
            "",
            true,
            false
    ),

    /**
     * Switched to upon first contact with players. During this
     * phase the game will eventually begin to start once the
     * required player count for the game to start is reached.
     *
     * However, if all players quit during this phase and the
     * current player count is equal to {@code 0} the current
     * {@link GamePhase} is being / should be set to
     * {@code SERVER_EMPTY} again.
     */
    LOBBY(
            "",
            true,
            false
    ),

    /**
     * Once the lobby countdown reached its end, all players are
     * moved to the arena. Afterwards the starting countdown
     * will be initialized.
     *
     * From this point onwards the game can be seen as "in-game".
     */
    STARTING(
            "",
            false,
            true
    ),

    /**
     * Immediately after the starting countdown has finished players
     * are actually playing the game.
     *
     * Therefore the game phase should also be set to {@code PLAYING}.
     */
    PLAYING(
            "",
            false,
            true
    ),

    /**
     * Once a single player is left in the arena the game is about
     * to end.
     *
     * Players are being moved back to the lobby whilst the game
     * arena resets itself (TNT reset).
     */
    ENDING(
            "",
            false,
            true
    ),

    /**
     * During this phase database operations are being processed, such
     * as statistics and playtime updates.
     *
     * After everything is completed, all players are kicked and the
     * server attempts to restart via the configured start script in
     * the "spigot.yml" config file.
     */
    SHUTDOWN(
            "",
            false,
            true
    );

    private final String motd;
    private final boolean joinable;
    private final boolean spectateable;
}
