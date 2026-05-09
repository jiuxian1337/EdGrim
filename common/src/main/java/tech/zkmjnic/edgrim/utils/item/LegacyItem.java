package tech.zkmjnic.edgrim.utils.item;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.latency.CompensatedWorld;

public class LegacyItem extends ItemBehaviour {

    public static final LegacyItem INSTANCE = new LegacyItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, PlayerData player, InteractionHand hand) {
        return false; // move legacy code that is responsible for handling item use from PacketPlayerDigging here??
    }

}
