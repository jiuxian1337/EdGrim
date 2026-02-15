package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.analysis.analysisg.*;
import ac.grim.grimac.checks.impl.aim.ExemptType;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "AnalysisG", configName = "AnalysisG", decay = 0.85, description = "Multi-strategy rotation analysis for aim patterns")
public class AnalysisG extends AnalysisCheck implements PacketCheck {
    private final DetectionContext context;
    private final List<AimDetectionStrategy> detectionStrategies;
    private final ArrayList<Rotation> rotations = new ArrayList<>();

    public AnalysisG(GrimPlayer player) {
        super(player);
        this.detectionStrategies = initializeDetectionStrategies();
        this.context = new DetectionContext(player);
    }

    private List<AimDetectionStrategy> initializeDetectionStrategies() {
        return List.of(
                new AccelerationDetection(),
                new AimPathAnalysis(),
                new CorrelationAnalysis(),
                new CoordinationDetection(),
                new FrictionDetection()
        );
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isFlying(event.getPacketType())) {
            return;
        }
        if (isExempt(ExemptType.TELEPORT)) {
            resetDetectionStrategies();
            return;
        }
        if (!shouldModifyPackets()) {
            return;
        }
        float movedYaw = player.getYaw() - player.getLastYaw();
        float movedPitch = player.getPitch() - player.getLastPitch();
        rotations.add(new Rotation(movedYaw, movedPitch));
        if (rotations.size() > 100) {
            rotations.remove(0);
        }
        if (!hasAttackedSince(2000)) {
            resetDetectionStrategies();
            return;
        }
        if (player.getTarget() == null) {
            resetDetectionStrategiesChangeTarget();
            return;
        }
        if (player.getLastTarget() != player.getTarget()) {
            resetDetectionStrategiesChangeTarget();
            return;
        }
        if (player.getTarget().type != EntityTypes.PLAYER) {
            resetDetectionStrategiesChangeTarget();
            return;
        }
        updateDetectionContext();
        executeDetectionStrategies();
        saveLastRotationValues();
    }

    private void updateDetectionContext() {
        AimProcessor processor = player.checkManager.getRotationCheck(AimProcessor.class);
        context.setRotations(rotations);
        if (processor != null) {
            context.setDeltaYaw(processor.getDeltaYaw() % 360F);
            context.setDeltaPitch(processor.getDeltaPitch());
        } else {
            context.setDeltaYaw(0.0F);
            context.setDeltaPitch(0.0F);
        }
        if (player.getTarget() != null) {
            Location playerLocation = player.getLocation();
            Vector3d targetPos = player.getTarget().trackedServerPosition.getPos();
            double dx = targetPos.x - playerLocation.getX();
            double dz = targetPos.z - playerLocation.getZ();
            float optimalYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            context.setOptimalYaw(Location.normalizeYaw(optimalYaw));
        }
        context.updateMovementData(player.getYaw(), player.getPitch(), time());
        context.updateRotation(new Rotation(player.getYaw(), player.getPitch()));
        context.updateRotationDelta(new Rotation(player.getYaw() - player.getLastYaw(), player.getPitch() - player.getLastPitch()));
    }

    private void executeDetectionStrategies() {
        for (AimDetectionStrategy strategy : detectionStrategies) {
            strategy.detect(player, context);
        }
    }

    private void resetDetectionStrategies() {
        for (AimDetectionStrategy strategy : detectionStrategies) {
            strategy.reset();
        }
    }

    private void resetDetectionStrategiesChangeTarget() {
        for (AimDetectionStrategy strategy : detectionStrategies) {
            strategy.changeTarget();
        }
    }

    private void saveLastRotationValues() {
        context.setLastDeltaYaw(context.getDeltaYaw());
        context.setLastDeltaPitch(context.getDeltaPitch());
        context.setLastOptimalYaw(context.getOptimalYaw());
    }
}
