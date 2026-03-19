package tech.zkmjnic.edgrim.manager;

import tech.zkmjnic.edgrim.EdGrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.api.event.events.CommandExecuteEvent;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.events.packets.ProxyAlertMessenger;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PunishmentManager implements ConfigReloadable {
    PlayerData player;
    List<PunishGroup> groups = new ArrayList<>();
    String experimentalSymbol = "*";
    private String alertString;
    private boolean testMode;
    private String proxyAlertString = "";
    private boolean hoverEnabled;
    private String hoverFormat;
    private boolean clickEnabled;
    private String clickCommand;

    public PunishmentManager(PlayerData player) {
        this.player = player;
    }

    @Override
    public void reload(ConfigManager config) {
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());
        experimentalSymbol = config.getStringElse("experimental-symbol", "*");
        alertString = config.getStringElse("alerts-format", "%prefix% &f%player% &bfailed &f%check_name%%experimental% &f(x&c%vl%&f)");
        testMode = config.getBooleanElse("test-mode", false);
        proxyAlertString = config.getStringElse("alerts-format-proxy", "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name%%experimental% &f(x&c%vl%&f)");
        hoverEnabled = config.getBooleanElse("alerts.hover.enabled", true);
        hoverFormat = config.getStringElse("alerts-hover-format", "&7%verbose%");
        clickEnabled = config.getBooleanElse("alerts.click.enabled", true);
        clickCommand = config.getStringElse("alerts.click.command", "/edgrim spectate %player%");
        try {
            groups.clear();

            // To support reloading
            for (AbstractCheck check : player.checkManager.allChecks.values()) {
                check.setEnabled(false);
            }

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<AbstractCheck> checksList = new ArrayList<>();
                List<AbstractCheck> excluded = new ArrayList<>();
                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (command.startsWith("!")) {
                        exclude = true;
                        command = command.substring(1);
                    }
                    for (AbstractCheck check : player.checkManager.allChecks.values()) { // o(n) * o(n)?
                        if (check.getCheckName() != null &&
                                (check.getCheckName().toLowerCase(Locale.ROOT).contains(command)
                                        || check.getAlternativeName().toLowerCase(Locale.ROOT).contains(command))) { // Some checks have equivalent names like AntiKB and AntiKnockback
                            if (exclude) {
                                excluded.add(check);
                            } else {
                                checksList.add(check);
                                check.setEnabled(true);
                            }
                        }
                    }
                    for (AbstractCheck check : excluded) checksList.remove(check);
                }

                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter * 1000));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!", e);
        }
    }

    private String replaceAlertPlaceholders(String original, int vl, Check check, String verboseReplacement) {
        return MessageUtil.replacePlaceholders(player, original
                .replace("[alert]", alertString)
                .replace("[proxy]", proxyAlertString)
                .replace("%client%", "%brand%")
                .replace("%check_name%", check.getDisplayName())
                .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "")
                .replace("%vl%", Integer.toString(vl))
                .replace("%description%", check.getDescription())
        ).replace("%verbose%", verboseReplacement);
    }

    private Component buildInteractiveAlert(int vl, Check check, String verbose, boolean proxy) {
        String format = proxy ? proxyAlertString : alertString;

        String mainText = replaceAlertPlaceholders(format, vl, check, "");
        Component component = MessageUtil.miniMessage(mainText);

        if (hoverEnabled) {
            String escapedVerbose = MiniMessage.miniMessage().escapeTags(verbose);
            String hoverText = replaceAlertPlaceholders(hoverFormat, vl, check, escapedVerbose);
            Component hoverComponent = MessageUtil.miniMessage(hoverText);
            component = component.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        if (clickEnabled && clickCommand != null && !clickCommand.isBlank()) {
            String resolvedClickCommand = MessageUtil.replacePlaceholders(player, clickCommand.replace("%client%", "%brand%"));
            component = component.clickEvent(ClickEvent.runCommand(resolvedClickCommand));
        }

        return component;
    }

    public boolean handleAlert(PlayerData player, String verbose, Check check) {
        boolean sentDebug = false;

        // Check commands
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                final int vl = getViolations(group, check);
                final int violationCount = group.violations.size();

                @Nullable Set<@Nullable PlatformPlayer> verboseListeners = null;

                // Verbose that prints all flags
                if (EdGrimAPI.INSTANCE.getAlertManager().hasVerboseListeners()) {
                    sentDebug = true;
                    verboseListeners = EdGrimAPI.INSTANCE.getAlertManager().sendVerbose(buildInteractiveAlert(vl, check, verbose, false), null);
                }

                for (ParsedCommand command : group.commands) {
                    boolean isAlert = command.command.equals("[alert]");
                    boolean isProxy = command.command.equals("[proxy]");
                    Component interactive = null;
                    String cmd;

                    if (isAlert || isProxy) {
                        interactive = buildInteractiveAlert(violationCount, check, verbose, isProxy);
                        cmd = MiniMessage.miniMessage().serialize(interactive);
                    } else {
                        String escapedVerbose = MiniMessage.miniMessage().escapeTags(verbose);
                        cmd = replaceAlertPlaceholders(command.command, violationCount, check, escapedVerbose);
                    }

                    if (violationCount >= command.threshold) {
                        // 0 means execute once
                        // Any other number means execute every X interval
                        boolean inInterval = command.interval == 0 ? (command.executeCount == 0) : (violationCount % command.interval == 0);
                        if (inInterval) {
                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(player, check, verbose, cmd);
                            EdGrimAPI.INSTANCE.getEventBus().post(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            switch (command.command) {
                                case "[webhook]" -> EdGrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, check.getDisplayName(), violationCount);
                                case "[log]" -> {
                                    String verboseWithoutGl = verbose.replaceAll(" /gl .*", "");
                                    EdGrimAPI.INSTANCE.getViolationDatabaseManager().logAlert(player, verboseWithoutGl, check.getDisplayName(), vl);
                                }
                                case "[proxy]" -> ProxyAlertMessenger.sendPluginMessage(cmd);
                                case "[alert]" -> {
                                    sentDebug = true;
                                    if (testMode) { // secret test mode
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(interactive);
                                        }
                                    } else {
                                        EdGrimAPI.INSTANCE.getAlertManager().sendAlert(interactive, verboseListeners);
                                    }
                                }
                                default -> EdGrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(EdGrimAPI.INSTANCE.getGrimPlugin(), () ->
                                        EdGrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                                                EdGrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                                                cmd
                                        )
                                );
                            }
                        }

                        command.executeCount++;
                    }
                }
            }
        }

        return sentDebug;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                long currentTime = System.currentTimeMillis();

                group.violations.put(currentTime, check);
                // Remove violations older than the defined time in the config
                group.violations.entrySet().removeIf(time -> currentTime - time.getKey() > group.removeViolationsAfter);
            }
        }
    }

    private int getViolations(PunishGroup group, Check check) {
        int vl = 0;
        for (Check value : group.violations.values()) {
            if (value == check) vl++;
        }
        return vl;
    }
}

@RequiredArgsConstructor
class PunishGroup {
    public final List<AbstractCheck> checks;
    public final List<ParsedCommand> commands;
    public final Map<Long, Check> violations = new HashMap<>();
    public final int removeViolationsAfter; // time to remove violations after in milliseconds
}

@RequiredArgsConstructor
class ParsedCommand {
    public final int threshold;
    public final int interval;
    public final String command;
    public int executeCount;
}
