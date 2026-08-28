package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.manager.violationdatabase.Violation;
import cc.watchneko.manager.violationdatabase.ViolationDatabaseManager;
import cc.watchneko.platform.api.player.OfflinePlatformPlayer;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrimHistory implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("history", "hist")
                        .permission("watchneko.help")
                        .required("target", StringParser.stringParser(), WatchNekoAPI.INSTANCE.getCommandAdapter().onlinePlayerSuggestions())
                        .optional("page", IntegerParser.integerParser())
                        .permission("watchneko.history")
                        .handler(this::handleHistory)
        );
    }

    private void handleHistory(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        Integer page = context.getOrDefault("page", 1);

        if (!WatchNekoAPI.INSTANCE.getViolationDatabaseManager().isEnabled()) {
            String msg = WatchNekoAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("grim-history-disabled",
                            "%prefix% &cHistory subsystem is disabled!");
            sender.sendMessage(MessageUtil.miniMessage(msg));
            return;
        } else if (!WatchNekoAPI.INSTANCE.getViolationDatabaseManager().isLoaded()) {
            String msg = WatchNekoAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("grim-history-load-failure",
                            "%prefix% &cHistory subsystem failed to load! Check server console for errors.");
            sender.sendMessage(MessageUtil.miniMessage(msg));
            return;
        }

        WatchNekoAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(WatchNekoAPI.INSTANCE.getGrimPlugin(), () -> {
            int entriesPerPage = WatchNekoAPI.INSTANCE.getConfigManager().getConfig().getIntElse("history.entries-per-page", 15);
            String header = WatchNekoAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-header",
                    "%prefix% &bShowing logs for &f%player% (&f%page%&b/&f%maxPages%&f)");
            String logFormat = WatchNekoAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-entry",
                    "%prefix% &8[&f%server%&8] &bFailed &f%check% (x&c%vl%&f) &7%verbose% (&b%timeago% ago&7)");

            OfflinePlatformPlayer targetPlayer = WatchNekoAPI.INSTANCE.getPlatformPlayerFactory().getOfflineFromName(target);

            ViolationDatabaseManager violations = WatchNekoAPI.INSTANCE.getViolationDatabaseManager();
            int logCount = violations.getLogCount(targetPlayer.getUniqueId());
            List<Violation> logs = violations.getViolations(targetPlayer.getUniqueId(), page, entriesPerPage);
            int maxPages = (int) Math.ceil((float) logCount / entriesPerPage);

            sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, header
                    .replace("%player%", targetPlayer.getName())
                    .replace("%page%", String.valueOf(page))
                    .replace("%maxPages%", String.valueOf(maxPages))
            )));

            for (int i = logs.size() - 1; i >= 0; i--) {
                Violation log = logs.get(i);
                sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, logFormat
                        .replace("%player%", targetPlayer.getName())
                        .replace("%grim_version%", log.grimVersion())
                        .replace("%client_brand%", log.clientBrand())
                        .replace("%client_version%", log.clientVersion())
                        .replace("%server_version%", log.serverVersion())
                        .replace("%check%", log.checkName())
                        .replace("%verbose%", log.verbose())
                        .replace("%vl%", String.valueOf(log.vl()))
                        .replace("%timeago%", getTimeAgo(log.createdAt()))
                        .replace("%server%", log.server())
                )));
            }
        });
    }

    /**
     * Calculates the time elapsed since a given timestamp in a human-readable format.
     *
     * @param timestamp The timestamp in milliseconds since epoch (e.g., from System.currentTimeMillis()).
     * @return A string representing the time elapsed (e.g., "5d 3h 10m").
     */
    private String getTimeAgo(long timestamp) {
        // Calculate duration directly from current time and the provided timestamp
        long durationMillis = System.currentTimeMillis() - timestamp;

        // Ensure duration is non-negative, though for "time ago" it should be.
        if (durationMillis < 0) {
            return "0s"; // Or handle as an error/future time
        }

        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        durationMillis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0 || result.length() == 0)
            result.append(seconds).append("s"); // Always show seconds if nothing else, or if it's 0s.

        return result.toString().trim();
    }
}
