package tech.zkmjnic.edgrim.checks.impl.analysis.analysisg;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;

public interface AimDetectionStrategy {
    void detect(EdGrimPlayer player, DetectionContext context);

    default void changeTarget() {
    }

    default void reset() {
    }

    String getCheckName();
}
