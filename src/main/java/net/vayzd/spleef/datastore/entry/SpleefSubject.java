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
package net.vayzd.spleef.datastore.entry;

import com.google.common.collect.*;
import lombok.*;
import net.vayzd.spleef.datastore.*;

import java.sql.*;
import java.util.*;

@DataStoreTable(name = "subjects")
@Getter
@Setter
public class SpleefSubject extends DataStoreEntry {

    private UUID uniqueId = null;
    private long firstGame = 0L,
            lastGame = 0L,
            playtime = 1L;
    private int pointCount = 0,
            gameCount = 0,
            winCount = 0,
            lossCount = 0,
            shotCount = 0,
            hitCount = 0,
            jumpCount = 0,
            doubleJumpCount = 0;

    @Override
    public void readFrom(ResultSet set) throws SQLException {
        setUniqueId(UUID.fromString(set.getString("uniqueId")));
        setFirstGame(set.getLong("firstGame"));
        setLastGame(set.getLong("lastGame"));
        setPlaytime(set.getLong("playtime"));
        setPointCount(set.getInt("pointCount"));
        setGameCount(set.getInt("gameCount"));
        setWinCount(set.getInt("winCount"));
        setLossCount(set.getInt("lossCount"));
        setHitCount(set.getInt("hitCount"));
        setJumpCount(set.getInt("jumpCount"));
        setDoubleJumpCount(set.getInt("doubleJumpCount"));
    }

    public ImmutableMap<String, Integer> getStatisticMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("pointCount", pointCount);
        map.put("gameCount", gameCount);
        map.put("winCount", winCount);
        map.put("lossCount", lossCount);
        map.put("shotCount", shotCount);
        map.put("hitCount", hitCount);
        map.put("jumpCount", jumpCount);
        map.put("doubleJumpCount", doubleJumpCount);
        return ImmutableMap.copyOf(map);
    }
}
