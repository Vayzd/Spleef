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
package net.vayzd.spleef.datastore;

import com.zaxxer.hikari.*;
import net.vayzd.spleef.datastore.entry.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.java.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import static java.util.Collections.singletonList;

public final class SpleefDataStore implements DataStore {

    private final LinkedList<String> TABLE_SCHEMA = new LinkedList<>(singletonList(

            "CREATE TABLE IF NOT EXISTS `players`(" +
                    "`id` MEDIUMINT NOT NULL AUTO_INCREMENT, " +
                    "`uniqueId` VARCHAR(36) NOT NULL, " +
                    "`firstGame` INT NOT NULL, " +
                    "`lastGame` INT NOT NULL, " +
                    "`playtime` INT NOT NULL, " +
                    "`pointCount` MEDIUMINT NOT NULL, " +
                    "`gameCount` MEDIUMINT NOT NULL, " +
                    "`winCount` MEDIUMINT NOT NULL, " +
                    "`lossCount` MEDIUMINT NOT NULL, " +
                    "`shotCount` MEDIUMINT NOT NULL, " +
                    "`hitCount` MEDIUMINT NOT NULL, " +
                    "`jumpCount` MEDIUMINT NOT NULL, " +
                    "`doubleJumpCount` MEDIUMINT NOT NULL, " +
                    "PRIMARY KEY(`id`), UNIQUE(`uniqueId`), INDEX(`pointCount`), INDEX(`winCount`), INDEX(`shotCount`)" +
                    ") DEFAULT CHARSET=utf8;"
    ));
    private final AtomicLong threadCount = new AtomicLong(0);
    private final List<Thread> threadList = new ArrayList<>();
    private final ExecutorService queue;
    private final JavaPlugin plugin;
    private final Logger logger;
    private final HikariConfig config;
    private HikariDataSource dataSource;

    private SpleefDataStore(JavaPlugin plugin, String host, int port, String username, String password, String database,
                            int poolSize) throws SQLException {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        Logger.getLogger("com.zaxxer.hikari").setLevel(Level.OFF);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, database));
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        // multiply the async thread count by 2 so there are still connections
        // available which can be used by spigot async events or netty IO threads for instance
        config.setMaximumPoolSize(poolSize * 2);
        config.addDataSourceProperty("useConfigs", "maxPerformance");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        if (poolSize <= 1) {
            queue = Executors.newSingleThreadExecutor(task -> {
                Thread thread = Executors.defaultThreadFactory().newThread(task);
                thread.setName("Database Queue");
                thread.setDaemon(true);
                threadList.add(thread);
                return thread;
            });
        } else {
            queue = Executors.newFixedThreadPool(poolSize, task -> {
                Thread thread = Executors.defaultThreadFactory().newThread(task);
                thread.setName("Database Thread #" + threadCount.incrementAndGet());
                thread.setDaemon(true);
                threadList.add(thread);
                return thread;
            });
        }
        this.config = config;
    }

    @Override
    public void connect() {
        dataSource = new HikariDataSource(config);
        submitTask(() -> {
            try (Statement statement = getConnection().createStatement()) {
                for (String schema : TABLE_SCHEMA) {
                    statement.executeUpdate(schema);
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Unable to create default table schema!", ex);
            }
        });
    }

    @Override
    public void disconnect() {
        dataSource.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public SpleefSubject getSubject(UUID uniqueId) {
        AtomicReference<SpleefSubject> reference = new AtomicReference<>(null);
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(String.format(
                    "SELECT * FROM %s WHERE uniqueId=?",
                    table(SpleefSubject.class)
            ));
            statement.setString(1, uniqueId.toString());
            ResultSet set = statement.executeQuery();
            if (!set.isClosed() && set.next()) {
                SpleefSubject subject = new SpleefSubject();
                subject.readFrom(set);
                reference.set(subject);
            }
            set.close();
            statement.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING,
                    String.format("Unable to get subject with UUID='%s' from database!", uniqueId.toString()),
                    ex);
        }
        return reference.get();
    }

    @Override
    public void getSubject(UUID uniqueId, Completable<SpleefSubject> completable) {
        fulfill(completable, getSubject(uniqueId));
    }

    @Override
    public void insertSubject(SpleefSubject subject) {
        UUID uniqueId = check(subject.getUniqueId(), "UniqueId of subject can't be null");
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(String.format(
                    "INSERT INTO %s(uniqueId, firstGame, lastGame, playtime, pointCount, gameCount, winCount, " +
                            "lossCount, shotCount, hitCount, jumpCount, doubleJumpCount) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    table(SpleefSubject.class)
            ));
            statement.setString(1, uniqueId.toString());
            statement.setLong(2, subject.getFirstGame());
            statement.setLong(3, subject.getLastGame());
            statement.setLong(4, subject.getPlaytime());
            statement.setInt(5, subject.getPointCount());
            statement.setInt(6, subject.getGameCount());
            statement.setInt(7, subject.getWinCount());
            statement.setInt(8, subject.getLossCount());
            statement.setInt(9, subject.getShotCount());
            statement.setInt(10, subject.getHitCount());
            statement.setInt(11, subject.getJumpCount());
            statement.setInt(12, subject.getDoubleJumpCount());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING,
                    String.format("Unable to insert subject with UUID='%s' to database!", uniqueId.toString()),
                    ex);
        }
    }

    @Override
    public void updateSubject(SpleefSubject subject) {
        UUID uniqueId = check(subject.getUniqueId(), "UniqueId of subject can't be null");
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(String.format(
                    "UPDATE %s SET firstGame=?, lastGame=?, playtime=?, pointCount=?, gameCount=?, winCount=?, " +
                            "lossCount=?, hotCount=?, hitCount=?, jumpCount=?, doubleJumpCount=? WHERE uniqueId=?",
                    table(SpleefSubject.class)
            ));
            statement.setLong(1, subject.getFirstGame());
            statement.setLong(2, subject.getLastGame());
            statement.setLong(3, subject.getPlaytime());
            statement.setInt(4, subject.getPointCount());
            statement.setInt(5, subject.getGameCount());
            statement.setInt(6, subject.getWinCount());
            statement.setInt(7, subject.getLossCount());
            statement.setInt(8, subject.getShotCount());
            statement.setInt(9, subject.getHitCount());
            statement.setInt(10, subject.getJumpCount());
            statement.setInt(11, subject.getDoubleJumpCount());
            statement.setString(12, uniqueId.toString());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING,
                    String.format("Unable to update subject with UUID='%s' on database!", uniqueId.toString()),
                    ex);
        }
    }

    @Override
    public void updateSubject(SpleefSubject subject, Runnable uponCompletion) {
        submitTask(() -> {
            updateSubject(subject);
            if (uponCompletion != null) {
                uponCompletion.run();
            }
        });
    }

    private String table(Class<? extends DataStoreEntry> from) {
        return check(from, "DataStoreEntry can't be null")
                .getAnnotation(DataStoreTable.class).name();
    }

    private void submitTask(final Runnable task) {
        check(task, "Database task (Runnable) can't be null");
        if (threadList.contains(Thread.currentThread()) || !plugin.getServer().isPrimaryThread()) {
            task.run();
        } else {
            queue.execute(task);
        }
    }

    private <T> void fulfill(final Completable<T> completable, final T result) {
        submitTask(() -> {
            check(completable, "Completable<T> (async result) can't be null");
            completable.complete(result);
        });
    }

    private <T> T check(final T object, final String error) throws NullPointerException {
        if (object != null) {
            return object;
        } else {
            throw new NullPointerException(error);
        }
    }

    private static volatile AtomicReference<DataStore> dataStore = new AtomicReference<>(null);

    public static DataStore getDataStore(JavaPlugin plugin, FileConfiguration configuration) throws SQLException {
        if (dataStore.get() == null) {
            dataStore.set(new SpleefDataStore(
                    plugin,
                    configuration.getString("database.host"),
                    configuration.getInt("database.port"),
                    configuration.getString("database.username"),
                    configuration.getString("database.password"),
                    configuration.getString("database.database"),
                    configuration.getInt("pool-size")
            ));
        }
        return dataStore.get();
    }
}
