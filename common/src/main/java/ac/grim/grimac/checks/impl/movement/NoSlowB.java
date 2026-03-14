package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

/**
 * @Author：jiuxian_baka
 * @Date：2026/3/12 09:57
 */
@CheckData(name = "NoSlowB (OffHand)", setback = 0)
public class NoSlowB extends Check implements PostPredictionCheck {

    public NoSlowB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || !PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;
            WrapperPlayClientUseItem wrapperPlayClientUseItem = new WrapperPlayClientUseItem(event);
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)
                    && player.gamemode == GameMode.SPECTATOR)
                return;

            ItemStack spigotMainHand = player.platformPlayer.getInventory().getItemInHand();
            ItemStack spigotOffHand = player.platformPlayer.getInventory().getItemInOffHand();

            System.out.println("Hand: " + wrapperPlayClientUseItem.getHand());
            System.out.println("GrimMainItem: " + player.inventory.getItemInHand(InteractionHand.MAIN_HAND));
            System.out.println("GrimOffItem: " + player.inventory.getItemInHand(InteractionHand.OFF_HAND));
            System.out.println("SpigotMainItem: " + spigotMainHand);
            System.out.println("SpigotOffItem: " + spigotOffHand);

            ItemStack itemInHand = player.inventory.getItemInHand(wrapperPlayClientUseItem.getHand());
            ItemStack spigotItem = wrapperPlayClientUseItem.getHand() == InteractionHand.MAIN_HAND ? spigotMainHand : spigotOffHand;

            if (itemInHand.isEmpty() && !itemInHand.equals(spigotItem)) {
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
