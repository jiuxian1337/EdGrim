package tech.zkmjnic.edgrim.utils.item;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class UnsupportedItem extends ItemBehaviour {

    public static final UnsupportedItem INSTANCE = new UnsupportedItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, EdGrimPlayer player, InteractionHand hand) {
        return false;
    }

}
