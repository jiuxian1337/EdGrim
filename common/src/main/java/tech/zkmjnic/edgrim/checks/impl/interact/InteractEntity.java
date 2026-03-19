package tech.zkmjnic.edgrim.checks.impl.interact;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import tech.zkmjnic.edgrim.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import tech.zkmjnic.edgrim.utils.nmsutil.InteractVisibilityUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "InteractEntity", description = "Attacking entities through occluding geometry")
public final class InteractEntity extends Check implements PacketCheck {

    public InteractEntity(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR || player.inVehicle()) {
            return;
        }

        final WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        final PacketEntity entity = player.compensatedEntities.getEntity(interact.getEntityId());
        if (entity == null || entity instanceof PacketEntityEnderDragonPart || entity.isDead || entity.riding != null || !entity.canHit()) {
            return;
        }

        final SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
        if (InteractVisibilityUtil.isBoxVisible(player, box)) {
            reward();
            return;
        }

        if (flagAndAlert("entity=" + interact.getEntityId() + ", type=" + entity.type.getName().getKey()) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
