package tech.zkmjnic.edgrim.manager.violationdatabase;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ViolationDatabase {

    void connect() throws SQLException;

    void logAlert(EdGrimPlayer player, String grimVersion, String verbose, String checkName, int vls);

    int getLogCount(UUID player);

    List<Violation> getViolations(UUID player, int page, int limit);

    void disconnect();

}
