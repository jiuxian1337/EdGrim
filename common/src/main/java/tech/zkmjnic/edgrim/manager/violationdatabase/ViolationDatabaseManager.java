package tech.zkmjnic.edgrim.manager.violationdatabase;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.plugin.GrimPlugin;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.init.ReloadableInitable;
import tech.zkmjnic.edgrim.manager.init.start.StartableInitable;
import tech.zkmjnic.edgrim.manager.violationdatabase.mysql.MySQLViolationDatabase;
import tech.zkmjnic.edgrim.manager.violationdatabase.sqlite.SQLiteViolationDatabase;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ViolationDatabaseManager implements StartableInitable, ReloadableInitable {

    private final GrimPlugin plugin;
    @Getter
    private boolean enabled = false;
    @Getter
    private boolean loaded = false;

    private @NonNull ViolationDatabase database;

    public ViolationDatabaseManager(GrimPlugin plugin) {
        this.plugin = plugin;
        this.database = NoOpViolationDatabase.INSTANCE;
    }

    @Override
    public void start() {
        load();
    }

    @Override
    public void reload() {
        load();
    }

    public void load() {
        ConfigManager cfg = EdGrimAPI.INSTANCE.getConfigManager().getConfig();
        this.enabled = cfg.getBooleanElse("history.enabled", false);
        String rawType = this.enabled ? cfg.getStringElse("history.database.type", "SQLITE").toUpperCase() : "NOOP";

        switch (rawType) {
            case "SQLITE" -> {
                if (!(database instanceof SQLiteViolationDatabase)) {
                    database.disconnect();
                    try {
                        // Init sqlite
                        Class.forName("org.sqlite.JDBC");
                        this.database = new SQLiteViolationDatabase(plugin);
                        database.connect();
                        loaded = true;
                    } catch (ClassNotFoundException e) {
                        LogUtil.error(
                                """
                                        Could not load SQLite driver for /edgrim history database.
                                        Download the minecraft-sqlite-jdbc mod/plugin for SQLite support, or change history.database.type
                                        Alternatively set history.enabled=false to remove this message if /edgrim history support is not desired"""
                        );
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    } catch (SQLException e) {
                        LogUtil.error(e);
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    }
                }
            }

            case "MYSQL" -> {
                String host = cfg.getStringElse("history.database.host", "localhost:3306");
                String db = cfg.getStringElse("history.database.database", "EdGrim");
                String user = cfg.getStringElse("history.database.username", "root");
                String pwd = cfg.getStringElse("history.database.password", "password");

                if (database instanceof MySQLViolationDatabase mysql
                        && mysql.sameConfig(host, db, user, pwd)) {
                    break;                          // nothing changed -> keep pool
                }
                database.disconnect();
                database = new MySQLViolationDatabase(plugin, host, db, user, pwd);
                try {
                    database.connect();
                    loaded = true;
                } catch (SQLException e) {
                    LogUtil.error(e);
                    this.database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }

            default -> {                            // NOOP or invalid
                if (!(database instanceof NoOpViolationDatabase)) {
                    database.disconnect();
                    database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }
        }
    }

    public void logAlert(PlayerData player, String verbose, String checkName, int vls) {
        String grimVersion = EdGrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        EdGrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(plugin, () -> database.logAlert(player, grimVersion, verbose, checkName, vls));
    }

    public int getLogCount(UUID player) {
        return database.getLogCount(player);
    }

    public List<Violation> getViolations(UUID player, int page, int limit) {
        return database.getViolations(player, page, limit);
    }

}
