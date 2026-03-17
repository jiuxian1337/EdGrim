package tech.zkmjnic.edgrim.checks.impl.movement;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

/**
 * @Author: siuxian_baka
 * @Date: 2026/3/12 09:57
 */
@CheckData(name = "NoSlowB (OffHand)", configName = "NoSlowB", setback = 0)
public class NoSlowB extends Check implements PostPredictionCheck {

    public NoSlowB(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            final EdGrimPlayer player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || !PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;
            WrapperPlayClientUseItem wrapperPlayClientUseItem = new WrapperPlayClientUseItem(event);
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)
                    && player.gamemode == GameMode.SPECTATOR)
                return;

            ItemStack spigotMainHand = player.platformPlayer.getInventory().getItemInHand();
            ItemStack spigotOffHand = player.platformPlayer.getInventory().getItemInOffHand();
            ItemStack itemInHand = player.inventory.getItemInHand(wrapperPlayClientUseItem.getHand());
            ItemStack spigotItem = wrapperPlayClientUseItem.getHand() == InteractionHand.MAIN_HAND ? spigotMainHand : spigotOffHand;

            if (itemInHand.getType() != spigotItem.getType()) {
                event.setCancelled(true);
                flagAndAlertWithSetback("Hand: " + wrapperPlayClientUseItem.getHand() +
                        "\nGrimMainItem: " + player.inventory.getItemInHand(InteractionHand.MAIN_HAND) +
                        "\nGrimOffItem: " + player.inventory.getItemInHand(InteractionHand.OFF_HAND) +
                        "\nSpigotMainItem: " + spigotMainHand +
                        "\nSpigotOffItem: " + spigotOffHand);
            }

        }
    }
}
