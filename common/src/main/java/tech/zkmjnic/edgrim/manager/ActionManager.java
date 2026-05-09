package tech.zkmjnic.edgrim.manager;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import lombok.Getter;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@Getter
public class ActionManager extends Check implements PacketCheck {
    private boolean attacking = false;
    private long lastAttack = 0;

    public ActionManager(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                player.totalFlyingPacketsSent = 0;
                attacking = true;
                lastAttack = System.currentTimeMillis();
            }
        } else if (isTickPacketIncludingNonMovement(event.getPacketType())) {
            player.totalFlyingPacketsSent++;
            player.incrementRespawnTick();
            attacking = false;
        }
    }

    public boolean hasAttackedSince(long time) {
        return System.currentTimeMillis() - lastAttack < time;
    }
}
