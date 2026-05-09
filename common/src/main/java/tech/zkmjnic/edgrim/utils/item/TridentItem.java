package tech.zkmjnic.edgrim.utils.item;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.latency.CompensatedWorld;

public class TridentItem extends ItemBehaviour {

    public static TridentItem INSTANCE = new TridentItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, PlayerData player, InteractionHand hand) {
        if (this.nextDamageWillBreak(item)) {
            return false;
        }

        return item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE) <= 0;
    }

    private boolean nextDamageWillBreak(ItemStack item) {
        return item.isDamageableItem() && item.getDamageValue() >= item.getMaxDamage() - 1;
    }

}
