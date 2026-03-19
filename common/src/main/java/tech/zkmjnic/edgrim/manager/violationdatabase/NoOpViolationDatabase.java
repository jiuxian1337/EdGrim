package tech.zkmjnic.edgrim.manager.violationdatabase;

import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.List;
import java.util.UUID;

public final class NoOpViolationDatabase implements ViolationDatabase {
    public static final NoOpViolationDatabase INSTANCE = new NoOpViolationDatabase();
    private NoOpViolationDatabase() {}

    @Override public void connect()    { }
    @Override public void disconnect() {}
    @Override public void logAlert(PlayerData p, String grimVersion, String v, String c, int vl) {}
    @Override public int getLogCount(UUID player) { return 0; }
    @Override public List<Violation> getViolations(UUID p, int page, int lim) { return List.of(); }
}
