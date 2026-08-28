package cc.watchneko.checks.impl.aim.trajectory;

import cc.watchneko.player.PlayerData;

public interface AimDetectionStrategy {
    void detect(PlayerData profile, DetectionContext context);

    String getCheckName();

    void changeTarget();

    void reset();
}
