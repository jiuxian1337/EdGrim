package tech.zkmjnic.edgrim.utils.anticheat;

import ac.grim.grimac.api.GrimUser;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {
    private static final char SECTION_CHAR = '\u00A7';
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + SECTION_CHAR + "[0-9A-FK-ORX]");
    private final Pattern HEX_PATTERN = Pattern.compile("([&" + SECTION_CHAR + "]#[A-Fa-f0-9]{6})|([&" + SECTION_CHAR + "]x([&" + SECTION_CHAR + "][A-Fa-f0-9]){6})");
    private final char PLACEHOLDER_ESCAPE_CHAR = '\uFFFF'; // this specific character holds no significance

    public @NotNull String toUnlabledString(@Nullable Vector3i vec) {
        return vec == null ? "null" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    public @NotNull String toUnlabledString(@Nullable Vector3f vec) {
        return vec == null ? "null" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    public @NotNull String replacePlaceholders(@Nullable PlayerData player, @NotNull String string, boolean removeFormatting) {
        return replacePlaceholders(player, player == null ? null : player.platformPlayer, string, removeFormatting);
    }

    public @NotNull String replacePlaceholders(@Nullable PlayerData player, @NotNull String string) {
        return replacePlaceholders(player, player == null ? null : player.platformPlayer, string, false);
    }

    public @NotNull String replacePlaceholders(@Nullable Sender sender, @NotNull String string) {
        return replacePlaceholders(sender != null ? sender.getPlatformPlayer() : null, string);
    }

    public @NotNull String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        return replacePlaceholders(player == null ? null : EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId()), player, string, false);
    }

    private @NotNull String replacePlaceholders(@Nullable PlayerData EdGrimPlayer, @Nullable PlatformPlayer platformPlayer, @NotNull String string, boolean removeFormatting) {
        for (Map.Entry<String, String> entry : EdGrimAPI.INSTANCE.getExternalAPI().getStaticReplacements().entrySet()) {
            string = string.replace(entry.getKey(), entry.getValue());
        }

        if (EdGrimPlayer != null) {
            for (Map.Entry<String, Function<GrimUser, String>> entry : EdGrimAPI.INSTANCE.getExternalAPI().getVariableReplacements().entrySet()) {
                String value = entry.getValue().apply(EdGrimPlayer).replace('%', PLACEHOLDER_ESCAPE_CHAR);
                if (removeFormatting) value = filterDiscordText(value);
                string = string.replace(entry.getKey(), value);
            }
        }

        return EdGrimAPI.INSTANCE.getMessagePlaceHolderManager().replacePlaceholders(platformPlayer, string).replace(PLACEHOLDER_ESCAPE_CHAR, '%');
    }

    public static String filterDiscordText(String message) {
        if (message == null || message.isBlank()) return message;
        final StringBuilder sb = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); ++i) {
            final char c = message.charAt(i);
            // Escape a newline
            if (c == '\n') {
                sb.append("\\n");
            }  // Escape Markdown special characters
            else if (c == '`' || c == '*' || c == '_' || c == '~' || c == '|') {
                sb.append('\\').append(c);
            } else {
                // Escape "# ", "> ", etc
                if (c == '#' || c == '>' || c == '-') {
                    // check if there's a space next
                    if (((i + 1 < message.length()) && (message.charAt(i + 1) == ' '))
                            && ((i == 0) || (message.charAt(i - 1) == '\n'))) {
                        sb.append("\\").append(c);
                    } else {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public @NotNull Component replacePlaceholders(@NotNull PlayerData player, @NotNull Component component) {
        // Replacement config that forces any placeholder replacement to be pure text
        final TextReplacementConfig safeReplacement = TextReplacementConfig.builder()
                .match("%[a-zA-Z0-9_]+%") // Match placeholders
                .replacement(placeholder -> Component.text(replacePlaceholders(player, placeholder.content())))
                .build();
        return component.replaceText(safeReplacement);
    }

    public @NotNull Component miniMessage(@NotNull String string) {
        string = string.replace("%prefix%", EdGrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&7[&bEdGrim&7]"));

        // hex codes
        Matcher matcher = HEX_PATTERN.matcher(string);
        StringBuilder sb = new StringBuilder(string.length());

        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(0).replaceAll("[&" + SECTION_CHAR + "#x]", "") + ">");
        }

        string = matcher.appendTail(sb).toString();

        // MiniMessage doesn't like legacy formatting codes
        string = translateAlternateColorCodes('&', string)
                .replace(SECTION_CHAR + "0", "<!b><!i><!u><!st><!obf><black>")
                .replace(SECTION_CHAR + "1", "<!b><!i><!u><!st><!obf><dark_blue>")
                .replace(SECTION_CHAR + "2", "<!b><!i><!u><!st><!obf><dark_green>")
                .replace(SECTION_CHAR + "3", "<!b><!i><!u><!st><!obf><dark_aqua>")
                .replace(SECTION_CHAR + "4", "<!b><!i><!u><!st><!obf><dark_red>")
                .replace(SECTION_CHAR + "5", "<!b><!i><!u><!st><!obf><dark_purple>")
                .replace(SECTION_CHAR + "6", "<!b><!i><!u><!st><!obf><gold>")
                .replace(SECTION_CHAR + "7", "<!b><!i><!u><!st><!obf><gray>")
                .replace(SECTION_CHAR + "8", "<!b><!i><!u><!st><!obf><dark_gray>")
                .replace(SECTION_CHAR + "9", "<!b><!i><!u><!st><!obf><blue>")
                .replace(SECTION_CHAR + "a", "<!b><!i><!u><!st><!obf><green>")
                .replace(SECTION_CHAR + "b", "<!b><!i><!u><!st><!obf><aqua>")
                .replace(SECTION_CHAR + "c", "<!b><!i><!u><!st><!obf><red>")
                .replace(SECTION_CHAR + "d", "<!b><!i><!u><!st><!obf><light_purple>")
                .replace(SECTION_CHAR + "e", "<!b><!i><!u><!st><!obf><yellow>")
                .replace(SECTION_CHAR + "f", "<!b><!i><!u><!st><!obf><white>")
                .replace(SECTION_CHAR + "r", "<reset>")
                .replace(SECTION_CHAR + "k", "<obfuscated>")
                .replace(SECTION_CHAR + "l", "<bold>")
                .replace(SECTION_CHAR + "m", "<strikethrough>")
                .replace(SECTION_CHAR + "n", "<underlined>")
                .replace(SECTION_CHAR + "o", "<italic>");

        return MiniMessage.miniMessage().deserialize(string).compact();
    }

    public Component getParsedComponent(Sender sender, String key, String fallbackText) {
        String message = EdGrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallbackText);
        message = MessageUtil.replacePlaceholders(sender, message);
        return MessageUtil.miniMessage(message);
    }

    @Contract("_, _ -> new")
    public static @NotNull String translateAlternateColorCodes(char altColorChar, @NotNull String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = SECTION_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    @Contract("!null -> !null; null -> null")
    public static @Nullable String stripColor(@Nullable String input) {
        return input == null ? null : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }
}
