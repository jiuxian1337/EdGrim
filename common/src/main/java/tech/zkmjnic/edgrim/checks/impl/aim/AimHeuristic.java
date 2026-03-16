package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.Tuple;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "AimHeuristic", configName = "AimHeuristic", decay = 0.75, description = "MX heuristic aim checks migrated")
public final class AimHeuristic extends EdAimCheck {
    private static final int BASIC_SAMPLE_SIZE = 10;
    private static final int PATTERN_SAMPLE_SIZE = 100;
    private static final int PATTERN_LENGTH = 3;
    private static final int MIN_PATTERN_START_INDEX_GAP = PATTERN_LENGTH;
    private static final double CONSTANT_MODULO_THRESHOLD = 60.0;
    private static final double CONSTANT_LINEAR_THRESHOLD = 0.1;
    private static final double INTERPOLATION_CLEAR_NORMAL_FLOOR = 3.0D;
    private static final float CONSTANT_MIN_DELTA = 0.1f;
    private static final float CONSTANT_MAX_DELTA = 20.0f;
    private static final float INVALID_PITCH = 90f + 1e-6f;

    private final List<Float> toYawHistory = new ArrayList<>(BASIC_SAMPLE_SIZE);
    private final List<Float> toPitchHistory = new ArrayList<>(BASIC_SAMPLE_SIZE);

    private int basicStreak;
    private float basicInterpolationBuffer;

    private float constantLastDeltaYaw;
    private float constantLastDeltaPitch;
    private float constantBuffer1;
    private float constantBuffer2;
    private float constantBuffer3;

    private int invalidBuffer;

    private float inconsistentLastDeltaYaw;
    private float inconsistentLastDeltaPitch;
    private final List<Float> inconsistentSamplesYaw = new ArrayList<>(60);
    private final List<Float> inconsistentSamplesPitch = new ArrayList<>(60);
    private float inconsistentBuffer;

    private Vec2f patternOldDelta = new Vec2f(0, 0);
    private final List<Vec2f> patternSamples = new ArrayList<>(PATTERN_SAMPLE_SIZE);
    private float patternBuffer;

    private boolean factorLastIsNoRotation;
    private double factorLastHash;
    private float factorBuffer;
    private int factorTicksToReset;
    private final List<Double> factorStack = new ArrayList<>(3);

    private final List<Double> smoothAngles = new ArrayList<>(20);
    private int smoothBuffer;

    private boolean basicComponentEnabled = true;
    private boolean randomizerFlawEnabled = true;
    private double interpolationAverageThreshold = 6.5D;
    private float interpolationBufferLimit = 4.0f;
    private float interpolationBufferDecay = 0.75f;

    private int constant1NeedVl = 8;
    private int constant2NeedVl = 6;
    private int constant3NeedVl = 6;

    private boolean invalidCheckEnabled = true;

    private boolean inconsistentCheckEnabled = true;
    private float inconsistentBufferLimit = 2.0f;

    private boolean patternCheckEnabled = true;
    private float patternBufferLimit = 2.5f;
    private float patternBufferFade = 0.3f;

    private boolean factorCheckEnabled = true;
    private float factorBufferLimit = 2.5f;
    private int factorResetTicksLimit = 2500;

    private boolean smoothCheckEnabled = true;

    public AimHeuristic(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        basicComponentEnabled = config.getBooleanElse("AimHeuristic.basic-component.enabled", true);
        randomizerFlawEnabled = config.getBooleanElse("AimHeuristic.basic-component.randomizer-flaw", true);
        interpolationAverageThreshold = config.getDoubleElse("AimHeuristic.basic-component.interpolation-average-threshold", 6.5D);
        interpolationBufferLimit = (float) config.getDoubleElse("AimHeuristic.basic-component.interpolation-buffer", 4.0D);
        interpolationBufferDecay = (float) config.getDoubleElse("AimHeuristic.basic-component.interpolation-buffer-decay", 0.75D);

        constant1NeedVl = config.getIntElse("AimHeuristic.constant-check.constant-1-buffer", 8);
        constant2NeedVl = config.getIntElse("AimHeuristic.constant-check.constant-2-buffer", 6);
        constant3NeedVl = config.getIntElse("AimHeuristic.constant-check.constant-3-buffer", 6);

        invalidCheckEnabled = config.getBooleanElse("AimHeuristic.invalid-check.enabled", true);

        inconsistentCheckEnabled = config.getBooleanElse("AimHeuristic.inconsistent-check.enabled", true);
        inconsistentBufferLimit = (float) config.getDoubleElse("AimHeuristic.inconsistent-check.buffer", 2.0);

        patternCheckEnabled = config.getBooleanElse("AimHeuristic.pattern-check.enabled", true);
        patternBufferLimit = (float) config.getDoubleElse("AimHeuristic.pattern-check.buffer", 2.5);
        patternBufferFade = (float) config.getDoubleElse("AimHeuristic.pattern-check.buffer-fade", 0.3);

        factorCheckEnabled = config.getBooleanElse("AimHeuristic.factor-check.enabled", true);
        factorBufferLimit = (float) config.getDoubleElse("AimHeuristic.factor-check.buffer", 2.5);
        factorResetTicksLimit = config.getIntElse("AimHeuristic.factor-check.ticks-to-reset", 2500);

        smoothCheckEnabled = config.getBooleanElse("AimHeuristic.smooth-check.enabled", true);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (!hasAttackedSince(3500L)) return;
        if (rotationUpdate.isCinematic()) return;
        if (isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE, ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE, ExemptType.RESPAWN)) {
            return;
        }

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        final float toPitch = rotationUpdate.getTo().getPitch();
        if (basicComponentEnabled && randomizerFlawEnabled
                && ((deltaPitch > 1.5f || deltaYaw > 3.0f)
                && (toPitch == 0.0f || (toPitch % 0.01f) == 0.0f))) {
            flagHeuristic("t=RandomizerFlaw dy=" + deltaYaw + " dp=" + deltaPitch + " toPitch=" + toPitch);
        }

        if (basicComponentEnabled) {
            basicTick(rotationUpdate);
        }
        constantTick(rotationUpdate);
        invalidTick(rotationUpdate);
        inconsistentTick(rotationUpdate);
        patternTick(rotationUpdate);
        factorTick(rotationUpdate);
        smoothTick(rotationUpdate);
    }

    private void basicTick(RotationUpdate rotationUpdate) {
        toYawHistory.add(rotationUpdate.getTo().getYaw());
        toPitchHistory.add(rotationUpdate.getTo().getPitch());
        if (toYawHistory.size() < BASIC_SAMPLE_SIZE) return;

        final List<Float> yawDeltas = new ArrayList<>(BASIC_SAMPLE_SIZE);
        {
            float oldYaw = toYawHistory.get(0);
            for (float yaw : toYawHistory) {
                yawDeltas.add(Math.abs(yaw - oldYaw));
                oldYaw = yaw;
            }
        }

        float oldYawResult = toYawHistory.get(0);
        float oldPitchResult = toPitchHistory.get(0);
        float oldYawChange = Math.abs(toYawHistory.get(0) - oldYawResult);
        float yawChangeFirst = Math.abs(toYawHistory.get(0) - toYawHistory.get(1));

        int machineKnownMovement = 0;
        int constantRotations = 0;
        int gcd = 0;
        int aggressivePatternI = 0;
        int aggressivePatternD = 0;
        int aggressivePatternI2 = 0;
        int aggressivePatternD2 = 0;
        int robotizedAmount = 0;
        int aggressiveAim = 0;
        int infinitives = 0;

        for (int i = 0; i < BASIC_SAMPLE_SIZE; i++) {
            float yaw = toYawHistory.get(i);
            float pitch = toPitchHistory.get(i);

            float yawChange = Math.abs(yaw - oldYawResult);
            float pitchChange = Math.abs(pitch - oldPitchResult);
            float robotized = Math.abs(yawChange - yawChangeFirst);
            float diffBetweenYawChanges = yawChange - oldYawChange;

            if (robotized < 2.0f && yawChange > 2.5f) robotizedAmount += 1;
            if (robotized < 0.99f && yawChange > 4.0f) machineKnownMovement++;
            if (robotized < 0.02f && yawChange > 3.0f) constantRotations++;
            if (robotized < 2.0f && yawChange > 3.0f) aggressiveAim++;

            double interpolation = MathUtil.scaleVal(yawChange / robotized, 2);
            if (Double.isInfinite(interpolation) && yawChange > 0.0f) {
                infinitives++;
                if (infinitives > 1 && yawChange < 0.4f) {
                    infinitives--;
                }
            }

            if (yawChange == 0.1f || pitchChange == 0.1f) gcd++;
            if (yawChange == 0.01f || pitchChange == 0.01f) gcd++;

            if ((diffBetweenYawChanges > 0.01f && diffBetweenYawChanges < 2.0f)) aggressivePatternI++;
            if ((diffBetweenYawChanges < -0.01f && diffBetweenYawChanges > -2.0f)) aggressivePatternD++;
            if (diffBetweenYawChanges > 2.0f) aggressivePatternI2++;
            if (diffBetweenYawChanges < -2.0f) aggressivePatternD2++;

            oldYawResult = yaw;
            oldPitchResult = pitch;
            oldYawChange = yawChange;
        }

        final int sens = calculateSensitivity();
        final int clientSens = rotationUpdate.getProcessor().totalSensitivityClient;
        final double averageYaw = Math.abs(MathUtil.getAverage(yawDeltas));
        if (sens > 65) {
            if (robotizedAmount > 8) flagHeuristic("t=BasicComponent reason=heuristic(sync) robot=" + robotizedAmount + " sens=" + sens + " cs=" + clientSens);
            if (aggressiveAim > 8) flagHeuristic("t=BasicComponent reason=heuristic(aggressive) aim=" + aggressiveAim + " sens=" + sens + " cs=" + clientSens);
            if (machineKnownMovement > 7) flagHeuristic("t=BasicComponent reason=heuristic(aim) machine=" + machineKnownMovement + " sens=" + sens + " cs=" + clientSens);
            if (constantRotations > 3) flagHeuristic("t=BasicComponent reason=heuristic(constant) count=" + constantRotations + " sens=" + sens + " cs=" + clientSens);
        } else {
            if (machineKnownMovement > 8) flagHeuristic("t=BasicComponent reason=heuristic(aim) machine=" + machineKnownMovement + " sens=" + sens + " cs=" + clientSens);
            if (constantRotations > 6) flagHeuristic("t=BasicComponent reason=heuristic(constant) count=" + constantRotations + " sens=" + sens + " cs=" + clientSens);
        }

        final boolean suspiciousInterpolation = infinitives > 1 && averageYaw > interpolationAverageThreshold;
        final boolean clearNormalInterpolation = infinitives == 0
                && averageYaw >= Math.max(INTERPOLATION_CLEAR_NORMAL_FLOOR, interpolationAverageThreshold - 2.0D);

        if (suspiciousInterpolation) {
            basicInterpolationBuffer = Math.min(
                    basicInterpolationBuffer + ((averageYaw > (interpolationAverageThreshold + 2.0D)) ? 1.25f : 1.0f),
                    interpolationBufferLimit + 1.0f
            );
            if (basicInterpolationBuffer >= interpolationBufferLimit) {
                if (flagHeuristic("t=BasicInterpolation reason=heuristic(interpolation) inf=" + infinitives + " avg=" + averageYaw + " buf=" + basicInterpolationBuffer + " sens=" + sens + " cs=" + clientSens)) {
                    basicInterpolationBuffer = Math.max(0.0f, interpolationBufferLimit - 1.5f);
                }
            }
        } else if (clearNormalInterpolation) {
            basicInterpolationBuffer = Math.max(0.0f, basicInterpolationBuffer - interpolationBufferDecay);
        }

        if (gcd > 0) flagHeuristic("t=BasicComponent reason=pattern(gcd) gcd=" + gcd + " sens=" + sens + " cs=" + clientSens);
        if (aggressivePatternI > 3 && aggressivePatternD > 3) {
            flagHeuristic("t=BasicInterpolation reason=pattern(random) inc=" + aggressivePatternI + " dec=" + aggressivePatternD + " sens=" + sens + " cs=" + clientSens);
        }
        if (aggressivePatternI2 > 3 && aggressivePatternD2 > 3 && (aggressivePatternI2 + aggressivePatternD2) > 8) {
            basicStreak++;
            if (basicStreak > 2) {
                flagHeuristic("t=BasicComponent reason=pattern(snap) streak=" + basicStreak + " bigInc=" + aggressivePatternI2 + " bigDec=" + aggressivePatternD2 + " sens=" + sens + " cs=" + clientSens);
            }
        } else {
            basicStreak = 0;
        }

        toYawHistory.clear();
        toPitchHistory.clear();
    }

    private void constantTick(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        final int sens = calculateSensitivity();
        final int clientSens = rotationUpdate.getProcessor().totalSensitivityClient;
        final boolean sensitivityTooLow = (sens < 50 && sens > -1) || (clientSens < 50);

        final double divisorYaw = MathUtil.getGcd((long) (deltaYaw * MathUtil.EXPANDER), (long) (constantLastDeltaYaw * MathUtil.EXPANDER));
        final double divisorPitch = MathUtil.getGcd((long) (deltaPitch * MathUtil.EXPANDER), (long) (constantLastDeltaPitch * MathUtil.EXPANDER));
        final double constantYaw = divisorYaw / MathUtil.EXPANDER;
        final double constantPitch = divisorPitch / MathUtil.EXPANDER;

        {
            final long expandedPitch = (long) (MathUtil.EXPANDER * deltaPitch);
            final long expandedLastPitch = (long) (MathUtil.EXPANDER * constantLastDeltaPitch);
            final long gcd = MathUtil.getGcd(expandedPitch, expandedLastPitch);
            final boolean validAngles = deltaYaw > 0.25f && deltaPitch > 0.25f && deltaPitch < CONSTANT_MAX_DELTA && deltaYaw < CONSTANT_MAX_DELTA;
            final boolean invalid = gcd < 131072L;

            if (invalid && validAngles && !sensitivityTooLow) {
                constantBuffer1 = Math.min(constantBuffer1 + 1.0f, 200.0f);
                if (constantBuffer1 > constant1NeedVl + 2) {
                    if (flagAndAlert("t=Constant1 gcd=" + gcd + " dy=" + deltaYaw + " dp=" + deltaPitch + " sens=" + sens + " cs=" + clientSens)) {
                        mitigateDamage();
                        constantBuffer1 = 4.0f;
                    }
                }
            } else if (constantBuffer1 > 0.0f) {
                constantBuffer1 -= 2.0f;
            }
        }

        {
            final boolean validDelta = deltaYaw > CONSTANT_MIN_DELTA && deltaPitch > CONSTANT_MIN_DELTA && deltaYaw < CONSTANT_MAX_DELTA && deltaPitch < CONSTANT_MAX_DELTA;
            if (validDelta && constantYaw != 0.0 && constantPitch != 0.0) {
                final double currentX = deltaYaw / constantYaw;
                final double currentY = deltaPitch / constantPitch;
                final double previousX = constantLastDeltaYaw / constantYaw;
                final double previousY = constantLastDeltaPitch / constantPitch;

                final double moduloX = currentX % previousX;
                final double moduloY = currentY % previousY;

                final double floorModuloX = Math.abs(Math.floor(moduloX) - moduloX);
                final double floorModuloY = Math.abs(Math.floor(moduloY) - moduloY);

                final boolean invalidX = moduloX > CONSTANT_MODULO_THRESHOLD && floorModuloX > CONSTANT_LINEAR_THRESHOLD;
                final boolean invalidY = moduloY > CONSTANT_MODULO_THRESHOLD && floorModuloY > CONSTANT_LINEAR_THRESHOLD;

                if (invalidX && invalidY && !sensitivityTooLow) {
                    constantBuffer2 = Math.min(constantBuffer2 + 1.0f, 200.0f);
                    if (constantBuffer2 > constant2NeedVl) {
                        if (flagAndAlert("t=Constant2 mx=" + moduloX + " my=" + moduloY + " fx=" + floorModuloX + " fy=" + floorModuloY + " sens=" + sens + " cs=" + clientSens)) {
                            mitigateDamage();
                            constantBuffer2 = 4.0f;
                        }
                    }
                } else if (constantBuffer2 > 0.0f) {
                    constantBuffer2 -= 2.0f;
                }
            }
        }

        {
            final boolean validDelta = deltaYaw > CONSTANT_MIN_DELTA && deltaPitch > CONSTANT_MIN_DELTA && deltaYaw < CONSTANT_MAX_DELTA && deltaPitch < CONSTANT_MAX_DELTA;
            if (validDelta && constantYaw != 0.0 && constantPitch != 0.0) {
                final double currentX = deltaYaw / constantYaw;
                final double currentY = deltaPitch / constantPitch;
                final double previousX = constantLastDeltaYaw / constantYaw;
                final double previousY = constantLastDeltaPitch / constantPitch;

                final double moduloX = currentX % previousX;
                final double moduloY = currentY % previousY;

                final double floorModuloX = Math.abs(Math.floor(moduloX) - moduloX);
                final double floorModuloY = Math.abs(Math.floor(moduloY) - moduloY);

                final boolean invalidX = moduloX > 60.0 && floorModuloX > 0.1;
                final boolean invalidY = moduloY > 60.0 && floorModuloY > 0.1;

                if (invalidX && invalidY && !sensitivityTooLow) {
                    constantBuffer3 = Math.max(constantBuffer3 + ((deltaPitch < 1.0f || deltaPitch > 13.0f) ? 2.0f : 1.0f), 0.0f);
                    final float limit = constant3NeedVl + 1.0f;
                    if (constantBuffer3 > ((sens < 70) ? (limit + 1.0f) : limit)) {
                        if (flagAndAlert("t=Constant3 mx=" + moduloX + " my=" + moduloY + " dy=" + deltaYaw + " dp=" + deltaPitch + " sens=" + sens + " cs=" + clientSens)) {
                            mitigateDamage();
                            constantBuffer3 = 0.0f;
                        }
                    }
                } else if (constantBuffer3 > 0.0f) {
                    constantBuffer3 -= 2.0f;
                }
            }
        }

        constantLastDeltaYaw = deltaYaw;
        constantLastDeltaPitch = deltaPitch;
    }

    private void invalidTick(RotationUpdate rotationUpdate) {
        if (!invalidCheckEnabled) return;

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        if (isExponentiallySmall(deltaPitch) && deltaPitch > 0.0f && deltaYaw > 0.5f) {
            invalidBuffer += 20;
            if (invalidBuffer > 70) {
                flagHeuristic("t=InvalidPitch dy=" + deltaYaw + " dp=" + deltaPitch);
            }
        } else {
            invalidBuffer--;
        }

        if (rotationUpdate.getTo().getPitch() > INVALID_PITCH) {
            flagHeuristic("t=UnlimitedPitch toPitch=" + rotationUpdate.getTo().getPitch() + " dp=" + deltaPitch);
        }
    }

    private static boolean isExponentiallySmall(final Number number) {
        double v = number.doubleValue();
        return v < 1 && (Double.toString(v).contains("E") || v == 0.0);
    }

    private void inconsistentTick(RotationUpdate rotationUpdate) {
        if (!inconsistentCheckEnabled) return;

        final int sens = calculateSensitivity();
        final int clientSens = rotationUpdate.getProcessor().totalSensitivityClient;
        final boolean invalidSensitivity = sens < 75 || sens > 175 || clientSens < 75 || clientSens > 170;
        if (invalidSensitivity) return;

        final float deltaYaw = Math.abs(rotationUpdate.getProcessor().getDeltaYaw());
        final float deltaPitch = Math.abs(rotationUpdate.getProcessor().getDeltaPitch());
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        final float differenceYaw = Math.abs(deltaYaw - inconsistentLastDeltaYaw);
        final float differencePitch = Math.abs(deltaPitch - inconsistentLastDeltaPitch);

        final float joltX = Math.abs(deltaYaw - differenceYaw);
        final float joltY = Math.abs(deltaPitch - differencePitch);

        inconsistentSamplesYaw.add((float) MathUtil.roundToPlace(joltX, 2));
        inconsistentSamplesPitch.add((float) MathUtil.roundToPlace(joltY, 2));

        if ((inconsistentSamplesYaw.size() + inconsistentSamplesPitch.size()) >= 60) {
            if (!(joltX == 0.0f || joltY == 0.0f)) {
                Tuple<List<Double>, List<Double>> outliersYaw = MathUtil.getOutliers(inconsistentSamplesYaw);
                Tuple<List<Double>, List<Double>> outliersPitch = MathUtil.getOutliers(inconsistentSamplesPitch);

                final int duplicatesX = MathUtil.getDuplicates(inconsistentSamplesYaw);
                final int duplicatesY = MathUtil.getDuplicates(inconsistentSamplesPitch);
                final int duplicatesSum = duplicatesX + duplicatesY;
                final int outliersX = outliersYaw.getX().size() + outliersYaw.getY().size();
                final int outliersY = outliersPitch.getX().size() + outliersPitch.getY().size();

                if ((duplicatesSum <= 3 && outliersX < 10 && outliersY < 7) && inconsistentBuffer++ >= inconsistentBufferLimit) {
                    flagHeuristic("t=Inconsistent low outX=" + outliersX + " outY=" + outliersY + " dup=" + duplicatesSum + " sens=" + sens + " cs=" + clientSens);
                } else if (((outliersX == 0 || outliersY == 0) && (outliersX > 1 || outliersY > 1) && duplicatesSum <= 3) && inconsistentBuffer++ >= inconsistentBufferLimit) {
                    flagHeuristic("t=Inconsistent zero outX=" + outliersX + " outY=" + outliersY + " dup=" + duplicatesSum + " sens=" + sens + " cs=" + clientSens);
                } else {
                    inconsistentBuffer -= 0.5f;
                }
            }
            inconsistentSamplesYaw.clear();
            inconsistentSamplesPitch.clear();
        }

        inconsistentLastDeltaYaw = deltaYaw;
        inconsistentLastDeltaPitch = deltaPitch;
    }

    private void patternTick(RotationUpdate rotationUpdate) {
        if (!patternCheckEnabled) return;

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        final Vec2f delta = rotationUpdate.getDelta();
        final float yawFactor = delta.getX() - patternOldDelta.getX();
        final float pitchFactor = delta.getY() - patternOldDelta.getY();
        final Vec2f vec = new Vec2f(yawFactor, pitchFactor);
        patternSamples.add(vec);

        if (patternSamples.size() >= PATTERN_SAMPLE_SIZE) {
            boolean flagged = false;
            final List<Float> rawPatterns = new ArrayList<>();
            final List<Float> filteredPatterns = new ArrayList<>();

            for (int i = 0; i < PATTERN_SAMPLE_SIZE; i++) {
                if (i > 0 && Math.abs(patternSamples.get(i).getX()) > 1.0f) {
                    rawPatterns.add(Math.abs(patternSamples.get(i).getX() - patternSamples.get(i - 1).getY()));
                }
            }

            for (final float x : rawPatterns) {
                if (x < 1e-4f) filteredPatterns.add(x);
            }

            if (filteredPatterns.size() > 3) {
                flagged = true;
                if (patternBuffer++ >= patternBufferLimit) {
                    if (flagHeuristic("t=Pattern suspicious filtered=" + filteredPatterns)) {
                        patternBuffer -= 1.0f;
                    }
                }
            }

            if (!flagged) {
                final List<Vec2f> patterns = new ArrayList<>();
                final int currentSampleSize = patternSamples.size();

                for (int i = 0; i <= currentSampleSize - PATTERN_LENGTH; ++i) {
                    for (int j = i + MIN_PATTERN_START_INDEX_GAP; j <= currentSampleSize - PATTERN_LENGTH; ++j) {
                        Vec2f pattern = null;
                        for (int k = 0; k < PATTERN_LENGTH; ++k) {
                            final Vec2f first = patternSamples.get(i + k);
                            final Vec2f second = patternSamples.get(j + k);
                            if (first.equals(second)) {
                                pattern = first;
                                break;
                            }
                        }
                        if (pattern != null && !patterns.contains(pattern)) patterns.add(pattern);
                    }
                }

                for (final Vec2f p : patterns) {
                    final float x = Math.abs(p.getX());
                    final float y = Math.abs(p.getY());
                    if ((x > 1.0f || y > 1.0f) && (x > 0.26f && y > 0.26f)) {
                        flagged = true;
                        if (patternBuffer++ >= patternBufferLimit) {
                            if (flagHeuristic("t=Pattern repeat vec=" + p)) {
                                patternBuffer -= 1.0f;
                            }
                        }
                        break;
                    }
                }
            }

            if (!flagged) patternBuffer = Math.max(0.0f, patternBuffer - patternBufferFade);
            patternSamples.clear();
        }

        patternOldDelta = delta;
    }

    private void factorTick(RotationUpdate rotationUpdate) {
        if (!factorCheckEnabled) return;

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();

        final boolean noRotation = deltaYaw == 0.0f && deltaPitch == 0.0f;
        if (noRotation) {
            if (!factorLastIsNoRotation) addFactorSample(0.0);
            factorCheck();
            factorLastIsNoRotation = true;
            return;
        }

        addFactorSample(MathUtil.scaleVal(deltaYaw, 2));
        factorCheck();
        factorLastIsNoRotation = false;
    }

    private void addFactorSample(double value) {
        factorStack.add(value);
        if (factorStack.size() > 3) {
            factorStack.remove(0);
        }
    }

    private void factorCheck() {
        if (factorStack.size() != 3) return;

        double hash = factorStack.get(0) + factorStack.get(1) + factorStack.get(2);
        if (hash == factorLastHash) return;

        double centre = factorStack.get(1);
        boolean hugeRotation = centre > 35.0;
        if (hugeRotation && centre != 360.0) {
            double compare = 1.2;
            boolean invalid = (factorStack.get(0) < compare && factorStack.get(2) < compare)
                    || (factorStack.get(0) > 55 && factorStack.get(1) < 2 && factorStack.get(2) > 55)
                    || (MathUtil.getMax(factorStack) > 70 && MathUtil.getMin(factorStack) < compare && MathUtil.getDistinct(factorStack) != 3);
            if (invalid) {
                float localVl = (centre > 160.0) ? 3.0f : (centre < 60.0) ? 1.0f : 2.0f;
                factorBuffer += localVl;
                if (factorBuffer >= factorBufferLimit) {
                    if (flagHeuristic("t=Factor centre=" + centre + " s=" + factorStack)) {
                        factorBuffer = factorBufferLimit - 1.0f;
                    }
                }
            }
        } else {
            factorTicksToReset++;
            if (factorTicksToReset >= factorResetTicksLimit) {
                factorTicksToReset = 0;
                factorBuffer = 0.0f;
            }
        }
        factorLastHash = hash;
    }

    private void smoothTick(RotationUpdate rotationUpdate) {
        if (!smoothCheckEnabled) return;

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        if (deltaYaw == 0.0f && deltaPitch == 0.0f) return;

        Vec2f delta = rotationUpdate.getDelta();
        double angle = MathUtil.getAngleInDegrees(delta) % 90;

        if ((deltaPitch > 1.5f && deltaYaw > 0.32f) || deltaYaw > 1.5f) {
            smoothAngles.add(angle);
        }

        if (smoothAngles.size() >= 20) {
            List<Float> jiff = MathUtil.getJiffDelta(smoothAngles, 1);
            float prev = 999f;
            float prePrev = 999f;
            for (float f : jiff) {
                if (f == 0.0f && prev == 0.0f && prePrev == 0.0f) {
                    if (++smoothBuffer > 5) {
                        flagHeuristic("t=Smooth m=" + jiff);
                    } else {
                        rewardBufferAndVL();
                    }
                    break;
                }
                prePrev = prev;
                prev = f;
            }
            smoothAngles.clear();
        }
    }

    private boolean flagHeuristic(String verbose) {
        if (flagAndAlert(verbose)) {
            mitigateDamage();
            return true;
        }
        return false;
    }
}
