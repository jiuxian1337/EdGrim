package cc.watchneko.checks.impl.aim;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.impl.aim.processor.AimProcessor;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.data.Pair;
import cc.watchneko.utils.math.Statistics;
import cc.watchneko.utils.math.Vec2;
import cc.watchneko.utils.math.Vec2f;
import cc.watchneko.utils.math.Vec2i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CheckData(name = "AimZ", description = "entropy, distinct, randomizer, machine heart", decay = 0.05)
public final class AimZ extends Check implements RotationCheck {
    private final List<Vec2f> rawRotations;
    private final List<Vec2i> rotations2;
    private final List<Vec2> kireikoGeneric;
    private final List<Float> localBuffer;
    private double oldShannonYaw, oldShannonPitch;
    private boolean cinematicBatch;

    public AimZ(PlayerData player) {
        super(player);
        this.rawRotations = new CopyOnWriteArrayList<>();
        this.rotations2 = Collections.synchronizedList(new CopyOnWriteArrayList<>());
        this.kireikoGeneric = new CopyOnWriteArrayList<>();
        this.oldShannonYaw = 0;
        this.oldShannonPitch = 0;
        this.cinematicBatch = false;
        this.localBuffer = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 16; i++) this.localBuffer.add(0.0f);
    }

    private static double getDifference(double a, double b) {
        return Math.abs(Math.abs(a) - Math.abs(b));
    }

    @Override
    public void process(RotationUpdate update) {
        if (!player.actionManager.hasAttackedSince(3500L)) return;

        Vec2f delta = update.getDelta();
        this.rawRotations.add(delta);
        this.cinematicBatch |= update.isCinematic2();
        double gcdValue = Statistics.getGCDValue(0.5d) * 3;
        this.rotations2.add(new Vec2i(
                (int) (delta.x() / gcdValue),
                (int) (delta.y() / gcdValue)));
        if (this.rotations2.size() >= 10) {
            this.checkSpikes();
        }
        if (this.rawRotations.size() >= 10) this.checkRaw();
    }

    private void checkRaw() {
        if (this.cinematicBatch) {
            this.rawRotations.clear();
            this.cinematicBatch = false;
            return;
        }

        final int sens = player.calculateSensitivity();
        final AimProcessor aimProcessor = player.checkManager.getRotationCheck(AimProcessor.class);
        final int sensTemp = aimProcessor != null ? aimProcessor.totalSensitivityClient : 0;
        final List<Float> x = new ArrayList<>(), y = new ArrayList<>();
        for (Vec2f vec2 : this.rawRotations) {
            x.add(vec2.x());
            y.add(vec2.y());
        }
        final int disX = Statistics.getDistinct(x);
        final double shannonYaw = Statistics.getShannonEntropy(x);
        final double shannonPitch = Statistics.getShannonEntropy(y);
        final boolean valid = sens >= 60 && sens <= 150 && sensTemp >= 60 && sensTemp < 150;

        if (valid && getDifference(shannonYaw, oldShannonYaw) < 1e-5
                && getDifference(shannonPitch, oldShannonPitch) < 1e-5) {
            this.increaseBuffer(11, 1.0f);
            final int vlLimit = 30;
            if (this.localBuffer.get(11) > vlLimit) {
                if (flagAndAlert("* [Analysis] Perfect shannon entropy " + shannonYaw)) {
                    player.mitigateDamage();
                }
                this.localBuffer.set(11, (float) (vlLimit - 1));
            }
        } else {
            this.localBuffer.set(11, 0f);
        }

        if (valid && getDifference(shannonYaw, shannonPitch) < 1e-5) {
            this.increaseBuffer(12, 1.0f);
            final int vlLimit = 30;
            if (this.localBuffer.get(12) > vlLimit) {
                if (flagAndAlert("* [Analysis] Similar shannon entropy " + shannonYaw)) {
                    player.mitigateDamage();
                }
                this.localBuffer.set(12, (float) (vlLimit - 1));
            }
        } else {
            this.localBuffer.set(12, 0f);
        }

        if ((disX < 8 && Math.abs(Statistics.getAverage(x)) > 2.5)) {
            this.increaseBuffer(9, 1.7f);
            final float vlLimit = 4.0f;
            if (this.localBuffer.get(9) >= vlLimit) {
                if (flagAndAlert("* [Flaw] Invalid distinct")) {
                    player.mitigateDamage();
                }
                this.increaseBuffer(9, -0.5f);
            }
        } else {
            this.increaseBuffer(9, -0.35f);
        }

        this.oldShannonYaw = shannonYaw;
        this.oldShannonPitch = shannonPitch;
        this.rawRotations.clear();
        this.cinematicBatch = false;
    }

    private void checkSpikes() {
        List<Integer> gcdYaw = new ArrayList<>(), gcdPitch = new ArrayList<>();
        for (Vec2i vec2i : this.rotations2) {
            gcdYaw.add(vec2i.x());
            gcdPitch.add(vec2i.y());
        }
        this.rotations2.clear();
        if (gcdYaw.isEmpty()) return;

        List<Double> yawY = Statistics.getOutliers(gcdYaw).second();
        List<Double> pitchY = Statistics.getOutliers(gcdPitch).second();
        Vec2 kireikoGenericVec = new Vec2(
                (float) Statistics.getKireikoGeneric(gcdYaw),
                (float) Statistics.getKireikoGeneric(gcdPitch));

        // kireiko generic
        this.kireikoGeneric.add(kireikoGenericVec);
        if (this.kireikoGeneric.size() >= 7) {
            final List<Double> kx = new ArrayList<>(), ky = new ArrayList<>();
            for (Vec2 vec2 : this.kireikoGeneric) {
                kx.add((double) vec2.x());
                ky.add((double) vec2.y());
            }
            double xDev = Statistics.getStandardDeviation(kx);
            double yDev = Statistics.getStandardDeviation(ky);
            Pair<Double, Double> xSpikes = new Pair<>(Statistics.getMin(kx), Statistics.getMax(kx));
            Pair<Double, Double> ySpikes = new Pair<>(Statistics.getMin(ky), Statistics.getMax(ky));

            if (xDev > 5 && xDev < 22 && ySpikes.second() < 50) {
                this.increaseBuffer(5, (Statistics.getAverage(kx) < 6.0) ? 0 : (xDev < 10) ? 1.5f : 1.0f);
                if (this.localBuffer.get(5) >= 7.0f) {
                    this.localBuffer.set(5, 6.0f);
                }
            } else {
                this.increaseBuffer(5, (xDev < 40 || ySpikes.second() < 70) ? -0.4f : -0.8f);
            }
            this.kireikoGeneric.clear();
        }

        // randomizer flaw check
        {
            double devX = Statistics.getVariance(gcdYaw);
            double devY = Statistics.getVariance(gcdPitch);
            double min = Math.min(devX, devY);
            double max = Math.max(devX, devY);
            if (min < 0.09 && max > 35 && Statistics.getMin(gcdPitch) != 0.0
                    && player.calculateSensitivity() > 50) {
                this.increaseBuffer(4, 1.0f);
                final float vlLimit = 2.5f;
                if (this.localBuffer.get(4) > vlLimit) {
                    if (flagAndAlert("* [Analysis] Randomizer flaw")) {
                        player.mitigateDamage();
                    }
                    this.localBuffer.set(4, vlLimit - 1);
                }
            } else {
                this.increaseBuffer(4, -0.4f);
            }
        }
    }

    private void increaseBuffer(int index, float v) {
        float r = this.localBuffer.get(index) + v;
        this.localBuffer.set(index, Math.max(r, 0));
    }
}
