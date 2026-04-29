package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.*;

public final class AimHeuristicPatternCheck implements HeuristicComponent {
    private final AimAA check;
    private Vec2f oldDelta = new Vec2f(0, 0);
    private final List<Vec2f> sample = new ArrayList<>();
    private static final int PATTERN_LENGTH = 3;
    private static final int SAMPLE_SIZE = 100;
    private static final int MIN_START_INDEX_GAP = PATTERN_LENGTH;
    private static final float BUFFER_LIMIT = 2.5f;
    private static final float BUFFER_FADE = 0.3f;
    private float buffer;

    public AimHeuristicPatternCheck(final AimAA check) {
        this.check = check;
    }

    @Override
    public void process(final RotationUpdate event) {
        if (event.getDeltaXRotABS() == 0 && event.getDeltaYRotABS() == 0) return;

        boolean flagged = false;
        final PlayerData player = check.getPlayer();
        final Vec2f delta = event.getDelta();
        final float yawFactor = delta.getX() - oldDelta.getX();
        final float pitchFactor = delta.getY() - oldDelta.getY();
        final Vec2f vec = new Vec2f(yawFactor, pitchFactor);

        this.sample.add(vec);
        if (this.sample.size() >= SAMPLE_SIZE) {
            final List<Vec2f> patterns = new ArrayList<>();
            final List<Float> rawPatterns = new ArrayList<>();
            final List<Float> filteredPatterns = new ArrayList<>();

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                if (i > 0 && Math.abs(this.sample.get(i).getX()) > 1.0) {
                    rawPatterns.add(Math.abs(this.sample.get(i).getX() - this.sample.get(i - 1).getY()));
                }
            }
            for (final float x : rawPatterns) {
                if (x < 1e-4) filteredPatterns.add(x);
            }
            if (filteredPatterns.size() > 3) {
                flagged = true;
                if (++buffer >= BUFFER_LIMIT) {
                    if (check.flagAndAlert("* Suspicious patterns: " + filteredPatterns)) {
                        check.getPlayer().mitigateDamage();
                    }
                    buffer -= 1;
                }
            }
            if (!flagged) {
                final int currentSampleSize = this.sample.size();
                for (int i = 0; i <= currentSampleSize - PATTERN_LENGTH; ++i) {
                    for (int j = i + MIN_START_INDEX_GAP; j <= currentSampleSize - PATTERN_LENGTH; ++j) {
                        Vec2f pattern = null;
                        for (int k = 0; k < PATTERN_LENGTH; ++k) {
                            final Vec2f first = this.sample.get(i + k);
                            final Vec2f second = this.sample.get(j + k);
                            if (Objects.equals(first, second)) {
                                pattern = first;
                                break;
                            }
                        }
                        if (pattern != null && !patterns.contains(pattern)) patterns.add(pattern);
                    }
                }
                for (final Vec2f vec2f : patterns) {
                    final float x = Math.abs(vec2f.getX());
                    final float y = Math.abs(vec2f.getY());
                    if ((x > 1.0 || y > 1.0) && (x > 0.26 && y > 0.26)) {
                        flagged = true;
                        if (++buffer >= BUFFER_LIMIT) {
                            if (check.flagAndAlert("* Suspicious pattern: " + vec2f)) {
                                check.getPlayer().mitigateDamage();
                            }
                            buffer -= 1f;
                        }
                        break;
                    }
                }
            }
            if (!flagged) buffer = Math.max(0, buffer - BUFFER_FADE);
            this.sample.clear();
        }
        oldDelta = delta;
    }
}
