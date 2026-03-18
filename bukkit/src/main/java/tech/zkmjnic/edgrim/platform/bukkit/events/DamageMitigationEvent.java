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
        EdGrimPlayer grimPlayerlayer = findGrimPlayer(player.getUniqueId());
        if (grimPlayerlayer == null || grimPlayerlayer.disableGrim) {
            return;
        }
        if (grimPlayerlayer.getMitigateDamageTime() < System.currentTimeMillis()) {
            return;
        }
        event.setDamage(event.getDamage() * 0.05);
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
