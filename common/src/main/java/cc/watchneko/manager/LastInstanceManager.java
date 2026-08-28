package cc.watchneko.manager;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.type.PostPredictionCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.PredictionComplete;
import cc.watchneko.utils.data.LastInstance;

import java.util.ArrayList;
import java.util.List;

public class LastInstanceManager extends Check implements PostPredictionCheck {
    private final List<LastInstance> instances = new ArrayList<>();

    public LastInstanceManager(PlayerData player) {
        super(player);
    }

    public void addInstance(LastInstance instance) {
        instances.add(instance);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        for (LastInstance instance : instances) {
            instance.tick();
        }
    }
}
