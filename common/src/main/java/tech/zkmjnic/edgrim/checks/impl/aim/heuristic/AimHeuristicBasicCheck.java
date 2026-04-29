package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Simplification;
import tech.zkmjnic.edgrim.utils.math.Statistics;
import tech.zkmjnic.edgrim.utils.math.Vec2;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AimHeuristicBasicCheck implements HeuristicComponent {
    private final AimAA check;
    private final List<Vec2> rawRotations;
    private int streak;
    private float vl, vlL2;
    private String reason = "";

    private static final float HEURISTIC_SYNC_ADD_VL = 125;
    private static final float HEURISTIC_AGGRESSIVE_ADD_VL = 50;
    private static final float HEURISTIC_AIM_ADD_VL = 100;
    private static final float HEURISTIC_CONSTANT_ADD_VL = 65;
    private static final float HEURISTIC_INTERPOLATION_ADD_VL = 55;
    private static final float PATTERN_SNAP_ADD_VL = 55;
    private static final float PATTERN_RANDOM_ADD_VL = 25;
    private static final int LOCAL_VL_LIMIT_COMPONENT = 400;
    private static final int LOCAL_VL_LIMIT_INTERPOLATION = 400;
    private static final float LOCAL_VL_FADE_COMPONENT = 5;
    private static final float LOCAL_VL_FADE_INTERPOLATION = 5;
    private static final int RANDOMIZER_FLAW_ADD_VL = 25;

    public AimHeuristicBasicCheck(final AimAA check) {
        this.check = check;
        this.rawRotations = new CopyOnWriteArrayList<>();
    }

    @Override
    public void process(final RotationUpdate event) {
        if (event.isCinematic2()) return;
        float absDeltaY = Math.abs(Math.abs(event.getTo().getPitch()) - Math.abs(event.getFrom().getPitch()));
        float absDeltaX = Math.abs(Math.abs(event.getTo().getYaw()) - Math.abs(event.getFrom().getYaw()));
        if (absDeltaY == 0 && absDeltaX == 0) return;

        Vec2f delta = event.getDelta();
        this.rawRotations.add(new Vec2(event.getTo().getYaw(), event.getTo().getPitch()));

        final PlayerData player = check.getPlayer();
        if (RANDOMIZER_FLAW_ADD_VL > 0 && ((delta.getY() > 1.5f || delta.getX() > 3.0f)
                && (event.getTo().getPitch() == 0
                || event.getTo().getPitch() % 0.01f == 0))) {
            if (check.flagAndAlert("* [Heuristic] Randomizer flaw")) {
                check.getPlayer().mitigateDamage();
            }
        }

        if (this.rawRotations.size() >= 10) checkDefaultAim();
    }

    private void checkDefaultAim() {
        final List<Vec2> rotations = this.rawRotations;
        Set<Double> yaws = new HashSet<>();
        {
            double oldYaw = rotations.get(0).x();
            for (Vec2 r : rotations) {
                yaws.add(Math.abs(r.x() - oldYaw));
                oldYaw = r.x();
            }
        }
        double oldYawResult = rotations.get(0).x();
        double oldPitchResult = rotations.get(0).y();
        double oldYawChange = Math.abs(rotations.get(0).x() - oldYawResult);
        double yawChangeFirst = Math.abs(rotations.get(0).x() - rotations.get(1).x());
        int machineKnownMovement = 0,
                constantRotations = 0, gcd = 0, aggressivePatternI = 0,
                aggressivePatternD = 0, aggressivePatternI2 = 0, aggressivePatternD2 = 0,
                robotizedAmount = 0, aggressiveAim = 0, infinitives = 0;

        for (Vec2 rotation : rotations) {
            double yawChange = Math.abs(rotation.x() - oldYawResult);
            double pitchChange = Math.abs(rotation.y() - oldPitchResult);
            double robotized = Math.abs(yawChange - yawChangeFirst);
            double diffBetweenYawChanges = yawChange - oldYawChange;
            double interpolation;

            if (robotized < 2 && yawChange > 2.5) robotizedAmount += 1;
            if (robotized < 0.99 && yawChange > 4) machineKnownMovement++;
            if (robotized < 0.02 && yawChange > 3) constantRotations++;
            if (robotized < 2 && yawChange > 3) aggressiveAim++;
            interpolation = Simplification.scaleVal(yawChange / robotized, 2);
            if (Double.isInfinite(interpolation) && yawChange > 0) {
                infinitives++;
                if (infinitives > 1 && yawChange < 0.4) {
                    infinitives--;
                }
            }
            if (yawChange == 0.1 || pitchChange == 0.1) gcd++;
            if (yawChange == 0.01 || pitchChange == 0.01) gcd++;
            if ((diffBetweenYawChanges > 0.01 && diffBetweenYawChanges < 2)) aggressivePatternI++;
            if ((diffBetweenYawChanges < -0.01 && diffBetweenYawChanges > -2)) aggressivePatternD++;
            if (diffBetweenYawChanges > 2) aggressivePatternI2++;
            if (diffBetweenYawChanges < -2) aggressivePatternD2++;
            oldYawResult = rotation.x();
            oldPitchResult = rotation.y();
            oldYawChange = yawChange;
        }

        final PlayerData player = check.getPlayer();
        final int sens = player.calculateSensitivity();
        if (sens > 65) {
            if (robotizedAmount > 8) addNewPunish("heuristic(sync)", HEURISTIC_SYNC_ADD_VL);
            if (aggressiveAim > 8) addNewPunish("heuristic(aggressive)", HEURISTIC_AGGRESSIVE_ADD_VL);
            if (machineKnownMovement > 7) addNewPunish("heuristic(aim)", HEURISTIC_AIM_ADD_VL);
            if (constantRotations > 3) addNewPunish("heuristic(constant)", HEURISTIC_CONSTANT_ADD_VL);
        } else {
            if (machineKnownMovement > 8) addNewPunish("heuristic(aim)", HEURISTIC_AIM_ADD_VL);
            if (constantRotations > 6) addNewPunish("heuristic(constant)", HEURISTIC_CONSTANT_ADD_VL);
        }
        if (infinitives > 1 && Math.abs(Statistics.getAverage(yaws)) > 3.2) {
            addNewPunishL2("heuristic(interpolation)", HEURISTIC_INTERPOLATION_ADD_VL);
        }
        if (gcd > 0) addNewPunish("pattern(gcd)", 1000);
        if (aggressivePatternI > 3 && aggressivePatternD > 3)
            addNewPunishL2("pattern(random)", PATTERN_RANDOM_ADD_VL);
        if (aggressivePatternI2 > 3 && aggressivePatternD2 > 3
                && (aggressivePatternI2 + aggressivePatternD2) > 8) {
            streak++;
            if (streak > 2) addNewPunish("pattern(snap)", PATTERN_SNAP_ADD_VL);
        } else streak = 0;

        if (this.vl > LOCAL_VL_LIMIT_COMPONENT) {
            if (check.flagAndAlert("* [Component] " + this.reason)) {
                check.getPlayer().mitigateDamage();
            }
            this.vl = 360;
        }
        if (this.vlL2 > LOCAL_VL_LIMIT_INTERPOLATION) {
            if (check.flagAndAlert("* [Flaw] Interpolation")) {
                check.getPlayer().mitigateDamage();
            }
            this.vlL2 -= 65;
        }
        if (this.vl > 0) this.vl -= LOCAL_VL_FADE_COMPONENT;
        if (this.vl > 400) this.vl -= 10;
        if (this.vlL2 > 0) this.vlL2 -= LOCAL_VL_FADE_INTERPOLATION;
        if (this.vlL2 > 380) this.vlL2 -= 10;

        this.rawRotations.clear();
    }

    private void addNewPunish(String reason, float vl) {
        this.reason = reason;
        this.vl += vl;
    }

    private void addNewPunishL2(String reason, float vl) {
        this.vlL2 += vl;
    }
}
