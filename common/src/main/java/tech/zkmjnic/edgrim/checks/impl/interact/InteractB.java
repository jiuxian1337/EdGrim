package tech.zkmjnic.edgrim.checks.impl.interact;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import tech.zkmjnic.edgrim.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import tech.zkmjnic.edgrim.utils.nmsutil.InteractVisibilityUtil;

@CheckData(name = "InteractB", description = "entity attack visibility check", decay = 0.05)
public final class InteractB extends Check implements PacketCheck {
    private int cancelVL;

    public InteractB(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        this.cancelVL = config.getIntElse(getConfigName() + ".cancelVL", 0);
    }

    private boolean shouldCancel() {
        return cancelVL >= 0 && violations >= cancelVL;
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

        if (InteractVisibilityUtil.isEntityVisible(player, entity.getPossibleCollisionBoxes())) {
            reward();
            return;
        }

        if (flagAndAlert("entity=" + interact.getEntityId() + ", type=" + entity.type.getName().getKey())
                && shouldCancel()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
