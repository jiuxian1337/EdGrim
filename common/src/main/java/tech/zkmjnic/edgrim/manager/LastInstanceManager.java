package tech.zkmjnic.edgrim.manager;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;
import tech.zkmjnic.edgrim.utils.data.LastInstance;

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
