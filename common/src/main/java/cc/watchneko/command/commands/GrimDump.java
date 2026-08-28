package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.platform.api.PlatformPlugin;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.utils.anticheat.MessageUtil;
import cc.watchneko.utils.common.PropertiesUtil;
import cc.watchneko.utils.reflection.ReflectionUtils;
import cc.watchneko.utils.reflection.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

import java.util.Map;
import java.util.Properties;

public class GrimDump implements BuildableCommand {

    private static final boolean PAPER = ReflectionUtils.hasClass("com.destroystokyo.paper.PaperConfig")
            || ReflectionUtils.hasClass("io.papermc.paper.configuration.Configuration");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private String link = null; // these links should not expire for a while

    public static JsonObject getBasicInfo(String type) {
        JsonObject base = new JsonObject();
        base.addProperty("type", type);
        base.addProperty("timestamp", System.currentTimeMillis());
        // versions
        JsonObject versions = new JsonObject();
        base.add("versions", versions);
        versions.addProperty("grim", WatchNekoAPI.INSTANCE.getExternalAPI().getGrimVersion());
        versions.addProperty("packetevents", PacketEvents.getAPI().getVersion().toString());
        versions.addProperty("server", PacketEvents.getAPI().getServerManager().getVersion().getReleaseName());
        versions.addProperty("implementation", WatchNekoAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());
        // state of different properties
        JsonObject states = new JsonObject();
        base.add("states", states);
        if (WatchNekoAPI.INSTANCE.isInitialized())
            states.addProperty("platform", WatchNekoAPI.INSTANCE.getPlatform().toString());
        if (ViaVersionUtil.isAvailable) states.addProperty("has_viaversion", true);
        if (PAPER) states.addProperty("has_paper", true);
        // system
        JsonObject system = new JsonObject();
        base.add("system", system);
        system.addProperty("os_name", System.getProperty("os.name"));
        system.addProperty("java_version", System.getProperty("java.version"));
        system.addProperty("user_language", System.getProperty("user.language"));
        // build
        JsonObject build = new JsonObject();
        base.add("build", getBuildInfo());
        return base;
    }

    private static JsonObject getBuildInfo() {
        JsonObject object = new JsonObject();
        try {
            Properties properties = PropertiesUtil.readProperties(WatchNekoAPI.INSTANCE.getClass(), "watchneko.properties");
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                object.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (Exception ignored) {
        }
        return object;
    }

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("dump", Description.of("Generate a debug dump"))
                        .permission("watchneko.dump")
                        .handler(this::handleDump)
        );
    }

    private void handleDump(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (link != null) {
            sender.sendMessage(MessageUtil.miniMessage(WatchNekoAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("upload-log", "%prefix% &fUploaded debug to: %url%")
                    .replace("%url%", link)));
            return;
        }
        // TODO: change this back to application/json once allowed
        GrimLog.sendLogAsync(sender, generateDump(), string -> link = string, "text/yaml");
    }

    /**
     * Generates a diagnostic dump in JSON format that contains various metadata
     * about the system, platform, and plugins. This dump is primarily used for
     * debugging and finding potential issues with the environment.
     *
     * @return A JSON-formatted string containing the diagnostic dump.
     */
    private String generateDump() {
        JsonObject base = getBasicInfo("dump");
        // plugins
        JsonArray plugins = new JsonArray();
        base.add("plugins", plugins);
        for (PlatformPlugin plugin : WatchNekoAPI.INSTANCE.getPluginManager().getPlugins()) {
            JsonObject pluginJson = new JsonObject();
            pluginJson.addProperty("enabled", plugin.isEnabled());
            pluginJson.addProperty("name", plugin.getName());
            pluginJson.addProperty("version", plugin.getVersion());
            plugins.add(pluginJson);
        }
        return gson.toJson(base);
    }
}
