package cc.watchneko.checks.impl.aim;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.math.Statistics;
import cc.watchneko.utils.math.Vec2f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@CheckData(name = "AimAB", description = "IQR, bot patterns, zFactor, improbable", decay = 0.05)
public final class AimAB extends Check implements RotationCheck {
    private final List<Vec2f> rawRotations;
    private final List<Double> shannonAnalysis;
    private final List<Float> localBuffer;

    public AimAB(PlayerData player) {
        super(player);
        this.rawRotations = new CopyOnWriteArrayList<>();
        this.shannonAnalysis = new CopyOnWriteArrayList<>();
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
        if (this.rawRotations.size() >= 25) this.checkRaw();
    }

    private void checkRaw() {
        float total = 0;
        {
            List<Float> x = new ArrayList<>(), y = new ArrayList<>();
            for (Vec2f vec2 : this.rawRotations) {
                x.add(vec2.x());
                y.add(vec2.y());
            }
            final List<Double> zFactorYaw = Statistics.getZScoreOutliers(x, 2.0);
            final List<Float> jiffYaw = Statistics.getJiffDelta(x, 5);
            final List<Float> jiffPitch = Statistics.getJiffDelta(y, 5);
            final List<Float> jiffOmni = new ArrayList<>();
            for (int i = 0; i < Math.min(jiffYaw.size(), jiffPitch.size()); i++) {
                jiffOmni.add(jiffYaw.get(i) / jiffPitch.get(i));
            }

            // omni IQR check
            {
                int infs = 0;
                for (float j : jiffOmni) if (Float.isInfinite(j)) infs++;
                final double iqr = Statistics.getIQR(jiffOmni);
                if (iqr > 12.5 && iqr < 96 && infs > 0) {
                    this.increaseBuffer(8, (iqr > 20) ? 1.4f : 0.8f);
                    if (this.localBuffer.get(8) > 11.0f) {
                        if (flagAndAlert("* [Statistics] IQR " + iqr)) {
                            player.mitigateDamage();
                        }
                        this.localBuffer.set(8, 11.0f - 2f);
                    }
                } else if (iqr < 13 || infs == 0) {
                    if (iqr < 7) {
                        this.increaseBuffer(8, -5.0f);
                    } else {
                        this.increaseBuffer(8, -3.5f);
                    }
                }
            }

            // Kolmogorov Smirnov Test
            final double kTest = Statistics.kolmogorovSmirnovTest(Statistics.getJiffDelta(x, 6), Function.identity());
            if (kTest > 10 && Math.abs(Statistics.getAverage(x)) < 13) {
                total++;
            }

            // Shannon entropy analysis
            shannonAnalysis.add(Statistics.getShannonEntropy(jiffYaw));
            if (shannonAnalysis.size() > 9) {
                shannonAnalysis.clear();
            }

            // bot pattern check
            int jiffPatterns = 0;
            for (int i = 0; i < jiffYaw.size(); i++) {
                float f = jiffYaw.get(i);
                if (!String.valueOf(f).contains("E") || f == 0) continue;
                for (int r = 0; r < jiffYaw.size(); r++) {
                    if (r == i) continue;
                    if (f == jiffYaw.get(r))
                        jiffPatterns++;
                }
            }
            if (jiffPatterns > 2 && Statistics.getAverage(x) > 3.0
                    && jiffPatterns != 6 && jiffPatterns != 12 && jiffPatterns != 4) {
                if (flagAndAlert("* [Statistics] AimBot pattern " + jiffPatterns)) {
                    player.mitigateDamage();
                }
            }

            // zFactor check
            boolean positive = false, negative = false;
            for (double d : zFactorYaw) {
                if (d > 10) positive = true;
                if (d < -10) negative = true;
            }
            if (zFactorYaw.size() == 2 && positive && negative
                    && Statistics.getMax(zFactorYaw) < 55) {
                this.increaseBuffer(0, 1.5f);
                if (this.localBuffer.get(0) > 4) total++;
                if (this.localBuffer.get(0) > 7.0f) {
                    if (flagAndAlert("* [Statistics] Suspicious zFactor " + zFactorYaw)) {
                        player.mitigateDamage();
                    }
                    this.localBuffer.set(0, 7.0f - 1f);
                }
            } else {
                this.increaseBuffer(0, -1.2f);
            }
        }

        // total improbable check
        {
            if (total < 2.0) {
                this.increaseBuffer(10, -2f);
            } else if (total > 2.0) {
                this.increaseBuffer(10, 5f);
                if (this.localBuffer.get(10) >= 15.0f) {
                    if (flagAndAlert("* [Statistics] Improbable " + this.localBuffer.get(10))) {
                        player.mitigateDamage();
                    }
                    this.increaseBuffer(10, -2.0f);
                }
            }
        }

        this.rawRotations.clear();
    }

    private void increaseBuffer(int index, float v) {
        float r = this.localBuffer.get(index) + v;
        this.localBuffer.set(index, Math.max(r, 0));
    }
}
