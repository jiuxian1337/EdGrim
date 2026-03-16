package tech.zkmjnic.edgrim.checks.impl.analysis;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.ExemptType;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.EvictingList;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CheckData(name = "AnalysisA", configName = "AnalysisA", decay = 0.92, description = "Multi-window rotation smoothness and jitter analysis")
public final class AnalysisA extends AnalysisCheck implements RotationCheck {
    private static final int FIRST_DERIV_WINDOW = 120;
    private static final int SECOND_DERIV_WINDOW = 120;
    private static final int SUS_HIST_SIZE = 200;
    private static final int LONG_TERM_SMOOTH_WINDOW = 300;
    private static final int SUSPICION_WINDOW = 150;
    private static final int MECH_SMOOTH_WINDOW = 100;
    private static final int SHORT_WINDOW_SIZE = 10;
    private static final int MID_WINDOW_SIZE = 20;
    private static final int LONG_WINDOW_SIZE = 40;
    private static final double MAX_K = 1.2;
    private static final double MIN_STD = 0.5;
    private static final double BIG_TO_SMALL_RATIO = 20.0;
    private static final double BIG_TO_SMALL_MINVAL = 0.4;
    private static final double BIG_TO_SMALL_MAXVAL = 5.0;
    private static final double MECH_SMOOTH_MAX_DIFF = 0.5;
    private static final double MECH_SMOOTH_AVG_THRESHOLD = 3.0;
    private static final double MECH_JITTER_CV_THRESHOLD = 0.20;
    private static final double MECH_JITTER_MEAN_THRESHOLD = 1.0;
    private static final double THR_MIN = 1.0;
    private static final double THR_MAX = 1.20;
    private static final double INC_SMALL_PITCH1 = 0.15;
    private static final double INC_SMALL_PITCH2 = 0.25;

    private final double[] susHistory = new double[SUS_HIST_SIZE];
    private double postFlagDecay = 0.35;
    private double thresholdStdMult = 2.0;
    private int smoothWindow = 80;
    private double bufferDecay = 0.965;
    private double bufferFlagThreshold = 4.0;
    private double smallMoveAngle = 0.5;
    private int smallPitchWindow = 60;
    private double smallPitchMax = 0.30;
    private double pitchRatio1 = 0.70;
    private double pitchRatio2 = 0.85;
    private int jitterWindow = 15;
    private int jitterFlipThreshold = 6;
    private EvictingList<Double> yawFirstDeriv;
    private EvictingList<Double> pitchFirstDeriv;
    private EvictingList<Double> yawSecondDeriv;
    private EvictingList<Double> pitchSecondDeriv;
    private EvictingList<Double> suspicionSeries;
    private EvictingList<Double> recentYaw;
    private EvictingList<Double> recentPit;
    private EvictingList<Double> longTermSmooth;
    private int frameIndex;
    private EvictingList<Double> shortYaw;
    private EvictingList<Double> shortPit;
    private EvictingList<Double> midYaw;
    private EvictingList<Double> midPit;
    private EvictingList<Double> longYaw;
    private EvictingList<Double> longPit;
    private double lastYawDelta;
    private double lastPitchDelta;
    private double prevYawDiff;
    private double prevPitchDiff;
    private int smallMoveFrameCount;
    private int yawFlipCount;
    private int pitchFlipCount;
    private int jitterWindowCounter;
    private int sameDeltaFrameCount;
    private Double lastYawForJitter;
    private Double lastPitchForJitter;

    public AnalysisA(EdGrimPlayer player) {
        super(player);
        initWindows();
    }

    @Override
    public void process(RotationUpdate update) {
        if (player.getDeltaXZ() < 0.02 || !hasAttackedSince(650L) || hasExemptions() || !shouldModifyPackets()) {
            buffer *= 0.80;
            return;
        }
        int sens = player.calculateSensitivity();
        AimProcessor processor = update.getProcessor();
        if (processor == null) {
            return;
        }
        int sensTemp = processor.totalSensitivityClient;
        boolean validSensitivity = sens >= 50 && sens <= 150 && sensTemp >= 60 && sensTemp < 150;
        if (!validSensitivity) {
            buffer *= 0.95;
            return;
        }
        double dY = processor.getDeltaYaw();
        double dP = processor.getDeltaPitch();
        if (Math.abs(dY) >= 18 || Math.abs(dP) >= 20 || Math.abs(dP) > 8 || player.predictedVelocity.isJump()) {
            buffer *= 0.85;
            return;
        }
        if (player.getTarget() == null) {
            return;
        }
        if (player.getTarget().getPossibleCollisionBoxes().distance(player.getBoundingBox()) > 0.85) {
            return;
        }
        double[] d2 = updateDerivatives(dY, dP);
        double smooth = updateSmoothScore(dY, dP);
        double inc = combineIncrements(dY, dP, d2[0], d2[1], smooth)
                + checkSmallPitchDominance()
                + checkMechanicalSmoothing()
                + checkMechanicalJitter();
        if (buffer > bufferFlagThreshold - 0.25) {
            inc += 0.04;
        }
        buffer = buffer * bufferDecay + Math.min(0.5, inc);
        if (buffer > bufferFlagThreshold && flagAndAlert(debugLine(dY, dP, d2, smooth))) {
            buffer *= postFlagDecay;
        }
    }

    private double[] updateDerivatives(double dY, double dP) {
        double dy2 = lastYawDelta != 0 ? Math.abs(dY - lastYawDelta) : 0;
        double dp2 = lastPitchDelta != 0 ? Math.abs(dP - lastPitchDelta) : 0;
        yawFirstDeriv.add(dY);
        pitchFirstDeriv.add(dP);
        yawSecondDeriv.add(dy2);
        pitchSecondDeriv.add(dp2);
        lastYawDelta = dY;
        lastPitchDelta = dP;
        return new double[]{dy2, dp2};
    }

    private double updateSmoothScore(double dY, double dP) {
        recentYaw.add(Math.abs(dY));
        recentPit.add(Math.abs(dP));
        double sm = MathUtil.stdDev(recentYaw, MathUtil.mean(recentYaw)) + MathUtil.stdDev(recentPit, MathUtil.mean(recentPit));
        longTermSmooth.add(Double.isFinite(sm) ? sm : 0.0);
        prevYawDiff = dY;
        prevPitchDiff = dP;
        return sm;
    }

    private double combineIncrements(double dY, double dP, double y2, double p2, double sm) {
        List<Double> list = new ArrayList<>();
        list.add(checkMaxMin(shortYaw, shortPit, SHORT_WINDOW_SIZE));
        list.add(checkMaxMin(midYaw, midPit, MID_WINDOW_SIZE) * 0.8);
        list.add(checkMaxMin(longYaw, longPit, LONG_WINDOW_SIZE) * 0.6);
        list.add(checkJitterAndSmallMove(dY, dP));
        double yawStd = Math.max(MathUtil.stdDev(yawFirstDeriv, MathUtil.mean(yawFirstDeriv)), MIN_STD);
        double pitStd = Math.max(MathUtil.stdDev(pitchFirstDeriv, MathUtil.mean(pitchFirstDeriv)), MIN_STD);
        double sus = 0.6 * (sigmoid(Math.min(y2 / yawStd, MAX_K) * 2.5) + sigmoid(Math.min(p2 / pitStd, MAX_K) * 2.5));
        suspicionSeries.add(sus);
        susHistory[frameIndex % SUS_HIST_SIZE] = sus;
        frameIndex++;
        double thr = calcDynThr();
        if (sus > thr) {
            double diff = sus - thr;
            list.add(diff > 0.15 ? 0.25 : diff > 0.07 ? 0.15 : 0.08);
        }
        return combine(list);
    }

    private double checkSmallPitchDominance() {
        if (recentPit.size() < smallPitchWindow) {
            return 0.0;
        }
        int yawFrames = 0;
        int silent = 0;
        for (int i = recentPit.size() - smallPitchWindow; i < recentPit.size(); i++) {
            double p = recentPit.get(i);
            double y = recentYaw.get(i);
            if (Math.abs(y) > 1) {
                yawFrames++;
                if (Math.abs(p) < smallPitchMax) {
                    silent++;
                }
            }
        }
        if (yawFrames == 0) {
            return 0.0;
        }
        double ratio = silent / (double) yawFrames;
        if (ratio >= pitchRatio2) {
            return INC_SMALL_PITCH2;
        }
        if (ratio >= pitchRatio1) {
            return INC_SMALL_PITCH1;
        }
        return 0.0;
    }

    private double checkMechanicalSmoothing() {
        if (longTermSmooth.size() < MECH_SMOOTH_WINDOW) {
            return 0.0;
        }
        List<Double> sub = tail(longTermSmooth, MECH_SMOOTH_WINDOW);
        double max = Collections.max(sub);
        double min = Collections.min(sub);
        double avg = MathUtil.mean(sub);
        return (max - min) < MECH_SMOOTH_MAX_DIFF && avg > MECH_SMOOTH_AVG_THRESHOLD ? 0.15 : 0.0;
    }

    private double checkMechanicalJitter() {
        if (shortYaw.size() < SHORT_WINDOW_SIZE || shortPit.size() < SHORT_WINDOW_SIZE) {
            return 0.0;
        }
        double mY = MathUtil.mean(shortYaw);
        double mP = MathUtil.mean(shortPit);
        double sY = MathUtil.stdDev(shortYaw, mY);
        double sP = MathUtil.stdDev(shortPit, mP);
        double cvY = mY == 0.0 ? 0.0 : sY / mY;
        double cvP = mP == 0.0 ? 0.0 : sP / mP;
        double inc = 0.0;
        if (mY > MECH_JITTER_MEAN_THRESHOLD && cvY < MECH_JITTER_CV_THRESHOLD) {
            inc += 0.15;
        }
        if (mP > MECH_JITTER_MEAN_THRESHOLD && cvP < MECH_JITTER_CV_THRESHOLD) {
            inc += 0.15;
        }
        return inc;
    }

    private double checkJitterAndSmallMove(double dY, double dP) {
        double inc = 0.0;
        boolean small = Math.abs(dY) < smallMoveAngle && Math.abs(dP) < smallMoveAngle;
        if (small && ++smallMoveFrameCount == 15) {
            inc += 0.20;
        }
        if (!small) {
            smallMoveFrameCount = 0;
        }
        boolean flipYaw = Math.signum(dY) != Math.signum(prevYawDiff);
        boolean flipPit = Math.signum(dP) != Math.signum(prevPitchDiff);
        if (small && flipYaw) {
            yawFlipCount++;
        }
        if (small && flipPit) {
            pitchFlipCount++;
        }
        if (++jitterWindowCounter >= jitterWindow) {
            if (yawFlipCount >= jitterFlipThreshold) {
                inc += 0.15;
            }
            if (pitchFlipCount >= jitterFlipThreshold) {
                inc += 0.15;
            }
            yawFlipCount = 0;
            pitchFlipCount = 0;
            jitterWindowCounter = 0;
        }
        if (lastYawForJitter != null && lastPitchForJitter != null) {
            if (Math.abs(dY - lastYawForJitter) < 1e-4 && Math.abs(dP - lastPitchForJitter) < 1e-4) {
                if (++sameDeltaFrameCount >= 4) {
                    inc += 0.25;
                    sameDeltaFrameCount = 0;
                }
            } else {
                sameDeltaFrameCount = 0;
            }
        }
        lastYawForJitter = dY;
        lastPitchForJitter = dP;
        prevYawDiff = dY;
        prevPitchDiff = dP;
        return inc;
    }

    private double checkMaxMin(EvictingList<Double> y, EvictingList<Double> p, int sz) {
        if (y.size() < sz || p.size() < sz) {
            return 0.0;
        }
        List<Double> m = new ArrayList<>(y);
        m.addAll(p);
        m.removeIf(v -> v <= 1e-6);
        if (m.size() < 2) {
            return 0.0;
        }
        double max = Collections.max(m);
        double min = Collections.min(m);
        if (min < BIG_TO_SMALL_MINVAL || max < BIG_TO_SMALL_MAXVAL) {
            return 0.0;
        }
        return max / (min + 1e-9) > BIG_TO_SMALL_RATIO ? 0.15 : 0.0;
    }

    private double calcDynThr() {
        int n = Math.min(frameIndex, SUS_HIST_SIZE);
        if (n < 2) {
            return THR_MIN;
        }
        List<Double> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(susHistory[i]);
        }
        double avg = MathUtil.mean(list);
        double std = MathUtil.stdDev(list, avg);
        return Math.max(THR_MIN, Math.min(avg + thresholdStdMult * std, THR_MAX));
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double combine(List<Double> values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum;
    }

    private String debugLine(double dY, double dP, double[] d2, double sm) {
        return String.format("dY= %.2f\ndP= %.2f\ny2= %.2f\np2= %.2f\nsm= %.2f\ns= %s\nbuf=%.2f",
                dY, dP, d2[0], d2[1], sm, player.calculateSensitivity(), buffer);
    }

    private List<Double> tail(EvictingList<Double> src, int window) {
        int n = src.size();
        return new ArrayList<>(src.subList(Math.max(0, n - window), n));
    }

    private boolean hasExemptions() {
        if (player.getTarget() == null) {
            return true;
        }
        return isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE)
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.getTarget().type != EntityTypes.PLAYER;
    }

    private void initWindows() {
        yawFirstDeriv = new EvictingList<>(FIRST_DERIV_WINDOW);
        pitchFirstDeriv = new EvictingList<>(FIRST_DERIV_WINDOW);
        yawSecondDeriv = new EvictingList<>(SECOND_DERIV_WINDOW);
        pitchSecondDeriv = new EvictingList<>(SECOND_DERIV_WINDOW);
        recentYaw = new EvictingList<>(smoothWindow);
        recentPit = new EvictingList<>(smoothWindow);
        longTermSmooth = new EvictingList<>(LONG_TERM_SMOOTH_WINDOW);
        shortYaw = new EvictingList<>(SHORT_WINDOW_SIZE);
        shortPit = new EvictingList<>(SHORT_WINDOW_SIZE);
        midYaw = new EvictingList<>(MID_WINDOW_SIZE);
        midPit = new EvictingList<>(MID_WINDOW_SIZE);
        longYaw = new EvictingList<>(LONG_WINDOW_SIZE);
        longPit = new EvictingList<>(LONG_WINDOW_SIZE);
        suspicionSeries = new EvictingList<>(SUSPICION_WINDOW);
    }
}
