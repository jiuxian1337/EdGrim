package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.EvictingQueue;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

@CheckData(name = "AimM", configName = "AimM", decay = 0.85, description = "Detects repeated invalid pitch divisor rows")
public final class AimM extends EdAimCheck {
    private EvictingQueue<Boolean> invalidDivisorList;
    private EvictingQueue<Double> rotationList;
    private double minAverageRot;
    private double maxInvalidRows;
    private double minRowLength;
    private double maxBuffer;
    private int sampleSize;
    private int mitigateVL;

    public AimM(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (hasAttackedSince(150L)) {
            double divisorY = rotationUpdate.getProcessor().divisorY;
            double deltaY = rotationUpdate.getProcessor().getDeltaPitch();

            if (Math.abs(rotationUpdate.getTo().getPitch()) == 90) {
                return;
            }

            if (isExempt(
                    ExemptType.TELEPORT,
                    ExemptType.SERVER_SENT_PULLBACK,
                    ExemptType.SERVER_SENT_ROTATE,
                    ExemptType.ELYTRA_FLYING,
                    ExemptType.VEHICLE) || player.packetStateData.horseInteractCausedForcedRotation
                    || !isMoving()) {
                return;
            }

            invalidDivisorList.add(divisorY < MathUtil.MINIMUM_DIVISOR);
            rotationList.add(deltaY);
            if (invalidDivisorList.size() >= sampleSize) {
                double averageRot = MathUtil.getAverageDouble(rotationList);
                if (getRowCount() > maxInvalidRows && averageRot > minAverageRot) {
                    if (buffer++ > maxBuffer) {
                        if (flagAndAlert(String.format("r= %d\na= %.2f", getRowCount(), averageRot))) {
                            buffer *= 0.65;
                            if (violations > mitigateVL) {
                                mitigateDamage();
                            }
                        }
                    }
                } else {
                    rewardBufferAndVL();
                }
            }
        }
    }

    private int getRowCount() {
        int rowCount = 0;
        int currentTrueCount = 0;

        for (Boolean b : invalidDivisorList) {
            if (b) {
                currentTrueCount++;
            } else {
                if (currentTrueCount >= minRowLength) {
                    rowCount++;
                }
                currentTrueCount = 0;
            }
        }

        if (currentTrueCount >= minRowLength) {
            rowCount++;
        }

        return rowCount;
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getIntElse(getConfigName() + ".buffer", 5);
        sampleSize = config.getIntElse(getConfigName() + ".sample-size", 25);
        maxInvalidRows = config.getDoubleElse(getConfigName() + ".max-invalid-rows", 2);
        minRowLength = config.getDoubleElse(getConfigName() + ".min-row-length", 3);
        minAverageRot = config.getDoubleElse(getConfigName() + ".min-average-rot", 0.4D);
        mitigateVL = config.getIntElse(getConfigName() + ".mitigate-vl", 6);
        invalidDivisorList = new EvictingQueue<>(sampleSize);
        rotationList = new EvictingQueue<>(sampleSize);
    }
}
