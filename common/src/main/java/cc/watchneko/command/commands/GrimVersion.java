package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;


public class GrimVersion implements BuildableCommand {


    public static void checkForUpdatesAsync(Sender sender) {
        String current = WatchNekoAPI.INSTANCE.getExternalAPI().getGrimVersion();
        sender.sendMessage(Component.text()
                .append(Component.text("WatchNeko Version: ").color(NamedTextColor.GRAY))
                .append(Component.text(current).color(NamedTextColor.AQUA))
                .build());
    }

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("version")
                        .permission("watchneko.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        checkForUpdatesAsync(sender);
    }

}
