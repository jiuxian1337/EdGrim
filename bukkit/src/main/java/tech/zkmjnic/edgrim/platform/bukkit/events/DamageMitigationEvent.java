package tech.zkmjnic.edgrim.platform.bukkit.events;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageMitigationEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        EdGrimPlayer EdGrimPlayer = findGrimPlayer(player.getUniqueId());
        if (EdGrimPlayer == null || EdGrimPlayer.disableGrim) {
            return;
        }
        if (!EdGrimPlayer.shouldMitigateDamage()) {
            return;
        }
        event.setDamage(0.5);
        EdGrimPlayer.consumeMitigateDamage();
    }

    private EdGrimPlayer findGrimPlayer(java.util.UUID uuid) {
        for (EdGrimPlayer entry : EdGrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (uuid.equals(entry.getUniqueId())) {
                return entry;
            }
        }
        return null;
    }
}
