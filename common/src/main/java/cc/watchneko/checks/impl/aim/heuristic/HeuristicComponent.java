package cc.watchneko.checks.impl.aim.heuristic;

import cc.watchneko.utils.anticheat.update.RotationUpdate;

public interface HeuristicComponent {
    void process(final RotationUpdate event);
}
