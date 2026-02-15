package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.Pair2;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CheckData(name = "AimS", configName = "AimS", decay = 0.65, description = "Detects abnormal horizontal rotation variance during attacks")
public final class AimS extends EdAimCheck {
    private final EvictingList<Pair2<Double, Double>> rotations = new EvictingList<>(10);
    private final EvictingList<Pair2<Integer, Integer>> rotationsG = new EvictingList<>(10);

    public AimS(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (hasAttackedSince(1200L)) {
            double deltaYaw = update.getProcessor().getDeltaYaw();
            double deltaPitch = update.getProcessor().getDeltaPitch();
            double gcdValue = MathUtil.getGCDValueStatistic(0.5) * 3;
            rotations.add(new Pair2<>(deltaYaw, deltaPitch));
            rotationsG.add(new Pair2<>((int) (deltaYaw / gcdValue), (int) (deltaYaw / gcdValue)));
            if (rotations.isFull()) {
                List<Double> x = new ArrayList<>(), y = new ArrayList<>();
                List<Integer> xG = new ArrayList<>(), yG = new ArrayList<>();
                for (Pair2<Double, Double> vec2 : rotations) {
                    x.add(vec2.getX());
                    y.add(vec2.getY());
                }
                for (Pair2<Integer, Integer> vec2 : rotationsG) {
                    xG.add(vec2.getX());
                    yG.add(vec2.getY());
                }

                double devX = MathUtil.getVariance(xG);
                double devY = MathUtil.getVariance(yG);
                double min = Math.min(devX, devY);
                double max = Math.max(devX, devY);
                if ((min < 0.09 && max > 35 && Collections.min(yG) != 0.0)) {
                    if (buffer++ > 4) {
                        if (flagAndAlert("low= " + min + "\nmax= " + max)) {
                            if (violations > 5) mitigateDamage();
                        }
                    } else {
                        rewardBufferAndVL();
                    }
                }
            }
        }
    }
}
