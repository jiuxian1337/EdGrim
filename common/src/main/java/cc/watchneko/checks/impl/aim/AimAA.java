package cc.watchneko.checks.impl.aim;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.impl.aim.heuristic.*;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;

import java.util.HashSet;
import java.util.Set;

@CheckData(name = "AimAA", description = "multi-component heuristic aim check", decay = 0.05)
public final class AimAA extends Check implements RotationCheck {
    private final Set<HeuristicComponent> components;
    private long lastAttack;

    public AimAA(PlayerData player) {
        super(player);
        this.lastAttack = System.currentTimeMillis() + 3500;
        this.components = new HashSet<>();
        this.components.add(new AimHeuristicBasicCheck(this));
        this.components.add(new AimHeuristicConstantCheck(this));
        this.components.add(new AimHeuristicInvalidCheck(this));
        this.components.add(new AimHeuristicInconsistentCheck(this));
        this.components.add(new AimHeuristicPatternCheck(this));
        this.components.add(new AimHeuristicFactorCheck(this));
        this.components.add(new AimHeuristicSmoothCheck(this));
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (getPlayer().packetStateData.lastPacketWasTeleport
                || getPlayer().vehicleData.wasVehicleSwitch
                || getPlayer().packetStateData.horseInteractCausedForcedRotation
                || getPlayer().packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || getPlayer().compensatedEntities.self.getRiding() != null) {
            return;
        }
        if (!player.actionManager.hasAttackedSince(3500L)) return;

        for (HeuristicComponent component : components) {
            component.process(rotationUpdate);
        }
    }

    public void trackAttack() {
        this.lastAttack = System.currentTimeMillis();
    }
}
