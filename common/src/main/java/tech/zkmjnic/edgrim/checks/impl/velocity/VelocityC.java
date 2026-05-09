package tech.zkmjnic.edgrim.checks.impl.velocity;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;
import tech.zkmjnic.edgrim.utils.math.OptifineFastMath;
import tech.zkmjnic.edgrim.utils.math.VanillaMath;
import tech.zkmjnic.edgrim.utils.nmsutil.Collisions;

import java.util.OptionalInt;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "VelocityC (Horizontal)", configName = "VelocityC", decay = 0.85)
public class VelocityC extends Check implements PostPredictionCheck {

    private static final float[][] KEY_COMBOS = {
            {1.0F, -1.0F},
            {1.0F, 0.0F},
            {1.0F, 1.0F},
            {0.0F, -1.0F},
            {0.0F, 0.0F},
            {0.0F, 1.0F},
            {-1.0F, -1.0F},
            {-1.0F, 0.0F},
            {-1.0F, 1.0F}
    };

    private static final boolean[] BOOL_OPTIONS = {true, false};

    private boolean attack;
    private double kbZ;
    private double kbX;
    private int ticks;
    private int velocitySinceTick;
    private double buffer;
    private boolean allowJumpReset;
    private double minVelocity;
    public VelocityC(PlayerData player) {
        super(player);
    }

    private static double hypot(double x, double z) {
        return Math.hypot(x, z);
    }

    private static float sin(float a, boolean fastMath) {
        return fastMath ? OptifineFastMath.sin(a) : VanillaMath.sin(a);
    }

    private static float cos(float a, boolean fastMath) {
        return fastMath ? OptifineFastMath.cos(a) : VanillaMath.cos(a);
    }

    private static double[] moveFlying(double motionX, double motionZ, float strafe, float forward, float friction, float yaw, boolean fastMath) {
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = (float) Math.sqrt(f);
            if (f < 1.0F) {
                f = 1.0F;
            }
            f = friction / f;
            strafe = strafe * f;
            forward = forward * f;

            float f1 = sin(yaw * (float) Math.PI / 180.0F, fastMath);
            float f2 = cos(yaw * (float) Math.PI / 180.0F, fastMath);
            motionX += strafe * f2 - forward * f1;
            motionZ += forward * f2 + strafe * f1;
        }
        return new double[]{motionX, motionZ};
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                int entityId = interact.getEntityId();
                var target = player.compensatedEntities.getEntity(entityId);
                if (target != null && !target.isDead && target.type == EntityTypes.PLAYER) {
                    attack = true;
                }
            }
        }

        if (!isFlying(event.getPacketType())) {
            return;
        }

        velocitySinceTick++;
        if (velocitySinceTick == 1 && player.likelyKB != null) {
            kbX = player.likelyKB.vector.getX();
            kbZ = player.likelyKB.vector.getZ();
        }

        if (shouldResetState()) {
            resetState();
        }

        if (kbX != 0.0 && kbZ != 0.0) {
            double clientKB = hypot(player.actualMovement.getX(), player.actualMovement.getZ());
            double deltaX = player.actualMovement.getX();
            double deltaZ = player.actualMovement.getZ();
            boolean ground = player.lastOnGround;
            double startX = kbX;
            double startZ = kbZ;
            float friction = player.friction;

            Double min = null;
            double bestX = kbX;
            double bestZ = kbZ;

            for (boolean sprint : BOOL_OPTIONS) {
                for (boolean jump : BOOL_OPTIONS) {
                    for (boolean using : BOOL_OPTIONS) {
                        for (boolean sneaking : BOOL_OPTIONS) {
                            for (float[] combo : KEY_COMBOS) {
                                float strafe = combo[0];
                                float forward = combo[1];

                                double predictedX = startX;
                                double predictedZ = startZ;

                                if (sprint && forward != 1.0F) {
                                    continue;
                                }

                                if (attack) {
                                    if (!player.actionManager.hasAttackedSince(100L)) {
                                        continue;
                                    }
                                    predictedX *= 0.6;
                                    predictedZ *= 0.6;
                                }

                                if (using) {
                                    strafe *= 0.2F;
                                    forward *= 0.2F;
                                }

                                if (sneaking) {
                                    strafe *= 0.3F;
                                    forward *= 0.3F;
                                }

                                if (jump && sprint && ground && allowJumpReset) {
                                    float radians = player.xRot * ((float) Math.PI / 180);
                                    predictedX -= sin(radians, false) * 0.2F;
                                    predictedZ += cos(radians, false) * 0.2F;
                                }

                                float speed = sprint ? (getAttributeSpeed() * 1.3f) : getAttributeSpeed();
                                if (ground) {
                                    float f = 0.16277136f / (friction * friction * friction);
                                    speed *= f;
                                } else {
                                    speed = sprint ? 0.026f : 0.02f;
                                }

                                strafe *= 0.98f;
                                forward *= 0.98f;

                                boolean fastMath = !player.isVanillaMath();
                                double[] predicts = moveFlying(predictedX, predictedZ, strafe, forward, speed, player.xRot, fastMath);

                                predictedX = predicts[0];
                                predictedZ = predicts[1];

                                double offsetX = deltaX - predictedX;
                                double offsetZ = deltaZ - predictedZ;
                                double offsetH = hypot(offsetX, offsetZ);

                                if (min == null || offsetH < min) {
                                    min = offsetH;
                                    bestX = predictedX;
                                    bestZ = predictedZ;
                                }
                            }
                        }
                    }
                }
            }

            kbX = bestX;
            kbZ = bestZ;

            double dKbX = deltaX / kbX;
            double dKbZ = deltaZ / kbZ;

            double kbH = hypot(kbX, kbZ);
            double ptc = clientKB / kbH * 100;
            double diff = Math.abs(kbH - clientKB);
            boolean exempt = player.packetStateData.lastPacketWasTeleport || player.horizontalCollision;
            boolean rev = dKbZ < -0.1 || dKbX < -0.1;

            double allowed = 0.002 + (player.totalFlyingPacketsSent <= 2 ? 0.03 : 0);

            if (!exempt && ((ptc < minVelocity && diff > allowed) || (ptc >= 300) || (rev && diff > allowed))) {
                buffer++;
                if (buffer > 2) {
                    if (flagAndAlertWithSetback(String.format("ptc= %.5f%ndiff= %.5f%na= %s%nr= %s", ptc, diff, attack, rev))) {
                        player.mitigateDamage();
                        resetState();
                        buffer *= 0.85;
                        if (ptc < 40) {
                            player.getSetbackTeleportUtil().executeViolationSetback();
                        }
                        if (ptc == 0) {
                            player.getSetbackTeleportUtil().executeForceResync();
                        }
                        return;
                    }
                }
            } else {
                rewardBufferAndVL();
            }

            kbX *= ground ? friction : 0.91f;
            kbZ *= ground ? friction : 0.91f;

            if (kbX == 0.0 || kbZ == 0.0 || Math.abs(kbX) < 0.005 || Math.abs(kbZ) < 0.005 || ++ticks > 6) {
                resetState();
                return;
            }

            if (exempt) {
                resetState();
            }
        }

        attack = false;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) {
            return;
        }

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        int entityId = velocity.getEntityId();

        int ridingVehicleId = player.inVehicle() ? player.getRidingVehicleId() : Integer.MIN_VALUE;
        if (entityId != player.entityID && entityId != ridingVehicleId) {
            return;
        }

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> velocitySinceTick = 0);
    }

    private boolean shouldResetState() {
        if (player.packetStateData.lastPacketWasTeleport || player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(1)) {
            return true;
        }
        if (player.inVehicle() || player.isFlying || player.canFly || player.compensatedEntities.self.isDead) {
            return true;
        }
        if ((player.isGliding || player.wasGliding) && player.isExemptElytra()) {
            return true;
        }
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) {
            return true;
        }
        if (player.wasTouchingWater || player.wasTouchingLava || player.slightlyTouchingWater || player.slightlyTouchingLava) {
            return true;
        }
        return isInCobweb();
    }

    private boolean isInCobweb() {
        if (kbX == 0.0 || kbZ == 0.0) {
            return false;
        }
        SimpleCollisionBox box = player.boundingBox.copy().expand(0.1f);
        return Collisions.hasMaterial(player, box, pair -> pair.first().getType() == StateTypes.COBWEB);
    }

    private float getAttributeSpeed() {
        double attributeSpeed = player.compensatedEntities.self.getAttributeValue(Attributes.MOVEMENT_SPEED);

        OptionalInt speed = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.SPEED);
        if (speed.isPresent()) {
            attributeSpeed *= 1.0f + 0.2f * (speed.getAsInt() + 1);
        }

        OptionalInt slowness = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.SLOWNESS);
        if (slowness.isPresent()) {
            attributeSpeed *= 1.0f - 0.15f * (slowness.getAsInt() + 1);
        }

        return (float) attributeSpeed;
    }

    private void resetState() {
        kbX = 0.0;
        kbZ = 0.0;
        ticks = 0;
    }

    public void rewardBufferAndVL() {
        buffer = Math.max(0, buffer - 0.5);
        reward();
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        allowJumpReset = config.getBooleanElse(getConfigName() + ".allowed-jump-reset", true);
        minVelocity = config.getDoubleElse(getConfigName() + ".min-velocity", 90);
    }
}
