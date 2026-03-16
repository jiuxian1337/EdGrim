package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimDuplicateLook", description = "Detects duplicate look packets with no rotation change")
public class AimDuplicateLook extends Check implements RotationCheck {
    private boolean exempt;

    public AimDuplicateLook(EdGrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate || player.compensatedEntities.self.getRiding() != null) {
            exempt = true;
            return;
        }

        if (exempt) { // Exempt for a tick on teleport
            exempt = false;
            return;
        }

        if (rotationUpdate.getFrom().equals(rotationUpdate.getTo())) {
            flagAndAlert();
        }
    }
}
