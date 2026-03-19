package tech.zkmjnic.edgrim.checks.impl.aim.trajectory;

import tech.zkmjnic.edgrim.player.PlayerData;

public interface AimDetectionStrategy {
    void detect(PlayerData profile, DetectionContext context);

    String getCheckName();

    void changeTarget();

    void reset();
}
