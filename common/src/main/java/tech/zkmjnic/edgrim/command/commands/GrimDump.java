package tech.zkmjnic.edgrim.command.commands;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.PlatformPlugin;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;
import tech.zkmjnic.edgrim.utils.common.PropertiesUtil;
import tech.zkmjnic.edgrim.utils.reflection.ReflectionUtils;
import tech.zkmjnic.edgrim.utils.reflection.ViaVersionUtil;
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

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("dump", Description.of("Generate a debug dump"))
                        .permission("edgrim.dump")
                        .handler(this::handleDump)
        );
    }

    private void handleDump(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (link != null) {
            sender.sendMessage(MessageUtil.miniMessage(EdGrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("upload-log", "%prefix% &fUploaded debug to: %url%")
                    .replace("%url%", link)));
            return;
        }
        // TODO: change this back to application/json once allowed
        GrimLog.sendLogAsync(sender, generateDump(), string -> link = string, "text/yaml");
    }

    public static JsonObject getBasicInfo(String type) {
        JsonObject base = new JsonObject();
        base.addProperty("type", type);
        base.addProperty("timestamp", System.currentTimeMillis());
        // versions
        JsonObject versions = new JsonObject();
        base.add("versions", versions);
        versions.addProperty("grim", EdGrimAPI.INSTANCE.getExternalAPI().getGrimVersion());
        versions.addProperty("packetevents", PacketEvents.getAPI().getVersion().toString());
        versions.addProperty("server", PacketEvents.getAPI().getServerManager().getVersion().getReleaseName());
        versions.addProperty("implementation", EdGrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());
        // state of different properties
        JsonObject states = new JsonObject();
        base.add("states", states);
        if (EdGrimAPI.INSTANCE.isInitialized()) states.addProperty("platform", EdGrimAPI.INSTANCE.getPlatform().toString());
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
            Properties properties = PropertiesUtil.readProperties(EdGrimAPI.INSTANCE.getClass(), "EdGrim.properties");
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                object.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (Exception ignored) {}
        return object;
    }

    /**
     * Generates a diagnostic dump in JSON format that contains various metadata
     * about the system, platform, and plugins. This dump is primarily used for
     * debugging and finding potential issues with the environment.
     * @return A JSON-formatted string containing the diagnostic dump.
     */
    private String generateDump() {
        JsonObject base = getBasicInfo("dump");
        // plugins
        JsonArray plugins = new JsonArray();
        base.add("plugins", plugins);
        for (PlatformPlugin plugin : EdGrimAPI.INSTANCE.getPluginManager().getPlugins()) {
            JsonObject pluginJson = new JsonObject();
            pluginJson.addProperty("enabled", plugin.isEnabled());
            pluginJson.addProperty("name", plugin.getName());
            pluginJson.addProperty("version", plugin.getVersion());
            plugins.add(pluginJson);
        }
        return gson.toJson(base);
    }
}
