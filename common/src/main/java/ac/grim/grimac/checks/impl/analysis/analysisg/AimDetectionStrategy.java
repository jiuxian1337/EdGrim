package ac.grim.grimac.checks.impl.analysis.analysisg;

import ac.grim.grimac.player.GrimPlayer;

public interface AimDetectionStrategy {
    void detect(GrimPlayer player, DetectionContext context);

    default void changeTarget() {
    }

    default void reset() {
    }

    String getCheckName();
}
