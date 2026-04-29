package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.Pair;
import tech.zkmjnic.edgrim.utils.math.Statistics;

import java.util.*;

public final class AimHeuristicInconsistentCheck implements HeuristicComponent {
    private final AimAA check;
    private float lastDeltaYaw = 0.0f, lastDeltaPitch = 0.0f;
    private final List<Float> samplesYaw = new ArrayList<>();
    private final List<Float> samplesPitch = new ArrayList<>();
    private static final float BUFFER_LIMIT = 2;
    private float buffer;

    public AimHeuristicInconsistentCheck(final AimAA check) {
        this.check = check;
    }

    @Override
    public void process(final RotationUpdate event) {
        if (event.getDeltaXRotABS() == 0 && event.getDeltaYRotABS() == 0) return;

        final PlayerData player = check.getPlayer();
        final float deltaYaw = Math.abs(event.getDeltaYRotABS());
        final float deltaPitch = Math.abs(event.getDeltaXRotABS());

        final AimProcessor aimProcessor = player.checkManager.getRotationCheck(AimProcessor.class);
        final int totalSensitivityClient = aimProcessor != null ? aimProcessor.totalSensitivityClient : 0;
        final boolean invalidSensitivity =
                player.calculateSensitivity() < 75
                        || player.calculateSensitivity() > 175
                        || totalSensitivityClient < 75
                        || totalSensitivityClient > 170;

        if (event.isCinematic2() || invalidSensitivity) return;

        final float differenceYaw = Math.abs(deltaYaw - lastDeltaYaw);
        final float differencePitch = Math.abs(deltaPitch - lastDeltaPitch);

        final float joltX = Math.abs(deltaYaw - differenceYaw);
        final float joltY = Math.abs(deltaPitch - differencePitch);

        samplesYaw.add((float) Statistics.roundToPlace(joltX, 2));
        samplesPitch.add((float) Statistics.roundToPlace(joltY, 2));

        if (samplesYaw.size() + samplesPitch.size() >= 60) {
            if (!(joltX == 0.0 || joltY == 0.0)) {
                final Pair<List<Double>, List<Double>> outliersYaw = Statistics.getOutliers(samplesYaw);
                final Pair<List<Double>, List<Double>> outliersPitch = Statistics.getOutliers(samplesPitch);

                final int duplicatesX = Statistics.getDuplicates(samplesYaw);
                final int duplicatesY = Statistics.getDuplicates(samplesPitch);
                final int duplicatesSum = duplicatesX + duplicatesY;
                final int outliersX = outliersYaw.first().size() + outliersYaw.second().size();
                final int outliersY = outliersPitch.first().size() + outliersPitch.second().size();

                if ((duplicatesSum <= 3 && outliersX < 10 && outliersY < 7) && ++buffer >= BUFFER_LIMIT) {
                    if (check.flagAndAlert("* Inconsistent rotations (" + outliersX + ", " + outliersY
                            + ", duplicates: " + duplicatesSum + ") [Too low values]")) {
                        check.getPlayer().mitigateDamage();
                    }
                } else if (((outliersX == 0 || outliersY == 0) && (outliersX > 1 || outliersY > 1)
                        && duplicatesSum <= 3) && ++buffer >= BUFFER_LIMIT) {
                    if (check.flagAndAlert("* Inconsistent rotations (" + outliersX + ", " + outliersY
                            + ", duplicates: " + duplicatesSum + ") [Zero value]")) {
                        check.getPlayer().mitigateDamage();
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5f);
                }
            }
            samplesYaw.clear();
            samplesPitch.clear();
        }

        this.lastDeltaYaw = deltaYaw;
        this.lastDeltaPitch = deltaPitch;
    }
}
