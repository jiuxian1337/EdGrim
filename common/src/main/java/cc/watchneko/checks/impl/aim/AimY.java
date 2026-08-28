package cc.watchneko.checks.impl.aim;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.math.Statistics;
import cc.watchneko.utils.math.Vec2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CheckData(name = "AimY", description = "rotation analysis: linear, rank, long-term", decay = 0.05)
public final class AimY extends Check implements RotationCheck {
    private final List<Vec2f> rawRotations, limitedRotations;
    private final List<Float> longTermAnalysis;
    private final List<Float> localBuffer;
    private boolean query;

    public AimY(PlayerData player) {
        super(player);
        this.rawRotations = new CopyOnWriteArrayList<>();
        this.limitedRotations = new CopyOnWriteArrayList<>();
        this.longTermAnalysis = new ArrayList<>();
        this.localBuffer = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 16; i++) this.localBuffer.add(0.0f);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!player.actionManager.hasAttackedSince(3500L)) return;

        Vec2f delta = update.getDelta();
        this.rawRotations.add(delta);
        if (this.rawRotations.size() >= 100) this.checkRaw();

        float absDeltaX = Math.abs(delta.x());
        float absDeltaY = Math.abs(delta.y());
        if (absDeltaX > 1.35 || absDeltaY > 1.35 && absDeltaX > 0.32) {
            this.limitedRotations.add(delta);
            if (this.limitedRotations.size() >= 100) this.checkLimited();
        }
    }

    private void checkLimited() {
        final List<Float> x = new ArrayList<>(), y = new ArrayList<>();
        for (Vec2f vec2 : this.limitedRotations) {
            x.add(vec2.x());
            y.add(vec2.y());
        }

        // limited analysis
        final List<Float> yawStack = new ArrayList<>();
        int resultDistinct = 0;
        for (final float yaw : x) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDistinct += Statistics.getDistinct(Statistics.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }
        final float distinctRank = (float) resultDistinct / 60;
        longTermAnalysis.add(distinctRank);
        if (longTermAnalysis.size() >= 10) {
            final double avg = Statistics.getAverage(longTermAnalysis);
            double normal = 0;
            for (double d : longTermAnalysis) if (d > 0.97) normal++;
            if (avg < 0.95 && normal < 4) {
                if (++buffer > 5) {
                    if (flagAndAlert("* Analysis long-term (avg rank: " + avg + ", normal: " + normal + " /10)")) {
                        player.mitigateDamage();
                    }
                    buffer = 2;
                }
            }
            longTermAnalysis.clear();
        }
        this.limitedRotations.clear();
    }

    private void checkRaw() {
        final List<Float> x = new ArrayList<>(), xAbs = new ArrayList<>(), y = new ArrayList<>();
        for (Vec2f vec2 : this.rawRotations) {
            x.add(vec2.x());
            xAbs.add(vec2.x());
            y.add(vec2.y());
        }

        // score
        final List<Float> yawStack = new ArrayList<>();
        final List<Double> resultDeviation = new ArrayList<>();
        int resultDistinct = 0;
        for (final float yaw : x) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDeviation.add(Statistics.getStandardDeviation(Statistics.getJiffDelta(yawStack, 5)));
                resultDistinct += Statistics.getDistinct(Statistics.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }
        final List<Double> outliers5 = Statistics.getZScoreOutliers(resultDeviation, 0.5f);
        final float distinctRank = (float) resultDistinct / 60;

        // linear
        if (outliers5.isEmpty() || outliers5.size() == 1 && Math.abs(outliers5.get(0)) > 10 && Math.abs(outliers5.get(0)) < 100) {
            if (++buffer > 3) {
                if (!query) {
                    query = true;
                } else {
                    if (flagAndAlert("* Analysis Linear: Invalid outliers " + Arrays.toString(outliers5.toArray()))) {
                        player.mitigateDamage();
                    }
                    buffer = 1;
                }
            }
        } else {
            query = false;
        }

        // rank
        {
            final int sens = player.calculateSensitivity();
            final boolean valid = sens > 20 && sens < 140;
            if (distinctRank < 1.0 && distinctRank > 0.7 && Statistics.getAverage(xAbs) > 1.8 && valid) {
                if (this.localBuffer.get(1) < 0.01) {
                    if (distinctRank < 0.8) this.increaseBuffer(1, 0.2f);
                } else {
                    final float limit = 6.0f;
                    this.increaseBuffer(1, (distinctRank > 0.9) ? 0.08f : (distinctRank > 0.8) ? 2f : 3f);
                    if (this.localBuffer.get(1) >= limit) {
                        if (flagAndAlert("* Analysis Rank: Incorrect rank " + distinctRank)) {
                            player.mitigateDamage();
                        }
                        this.localBuffer.set(1, limit - 1);
                    }
                }
            } else {
                this.increaseBuffer(1, -2.25f);
            }
        }

        this.rawRotations.clear();
    }

    private void increaseBuffer(int index, float v) {
        float r = this.localBuffer.get(index) + v;
        this.localBuffer.set(index, Math.max(r, 0));
    }
}
