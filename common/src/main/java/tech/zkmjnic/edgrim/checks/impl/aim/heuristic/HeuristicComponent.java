package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

public interface HeuristicComponent {
    void process(final RotationUpdate event);
}
