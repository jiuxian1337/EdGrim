package ac.grim.grimac.checks.impl.velocity;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;

/**
 * @Author：jiuxian_baka
 * @Date：2026/2/16 08:41
 */
@CheckData(name = "VelocityD (Delay)", configName = "VelocityD", description = "Check lag/delay/alink velocity")
public class VelocityD extends Check implements PostPredictionCheck {

    private long sendTime;
    private boolean velocity = false;
    private double buffer;
    private int lastPing;
    private short id;

    private int delay;

    public VelocityD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION || event.getPacketType() == PacketType.Play.Client.PONG) {
            if (velocity) {
                short transId;
                if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
                    transId = new WrapperPlayClientWindowConfirmation(event).getActionId();
                } else {
                    transId = (short) new WrapperPlayClientPong(event).getId();
                }
                if (transId == id) {
                    velocity = false;
                    long transDelay = System.currentTimeMillis() - sendTime - lastPing;
                    if (transDelay > delay) {
                        buffer = Math.min(5, buffer + 1);
                        if (buffer >= 5) {
                            flagAndAlert("delay: " + transDelay + "ms\n" +
                                    "ping: " + lastPing + "ms");
                            player.mitigateDamage();
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.5);
                    }
                }
            }
            lastPing = player.getTransactionPing();
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
            int entityId = velocity.getEntityId();

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (player.compensatedEntities.serverPlayerVehicle != null && entityId != player.compensatedEntities.serverPlayerVehicle) {
                return;
            }
            if (player.compensatedEntities.serverPlayerVehicle == null && entityId != player.entityID) {
                return;
            }

            event.getTasksAfterSend().add(() -> {
                short postId = player.checkManager.getKnockbackHandler().getLastPostVelocityTransactionId();
                if (postId == 0) {
                    this.velocity = false;
                    return;
                }
                this.id = postId;
                this.sendTime = System.currentTimeMillis();
                this.velocity = true;
            });

        }
    }

    @Override
    public void onReload(ConfigManager config) {
        delay = config.getIntElse(getConfigName() + ".delay", 250);
    }
}
