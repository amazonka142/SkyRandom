package dev.macuser.skyrandom.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageManager {

    private static final String PLAYER_LANGUAGE_PATH = "players";

    private final JavaPlugin plugin;
    private final Map<PlayerLanguage, FileConfiguration> messages = new EnumMap<>(PlayerLanguage.class);
    private final File playerLanguagesFile;

    private FileConfiguration playerLanguages;
    private PlayerLanguage defaultLanguage = PlayerLanguage.RU;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerLanguagesFile = new File(plugin.getDataFolder(), "player-languages.yml");
    }

    public void reload(ConfigurationSection settings) {
        this.defaultLanguage = PlayerLanguage.fromCode(
            settings != null ? settings.getString("default-language", "ru") : "ru"
        );

        loadMessages(PlayerLanguage.RU, "messages_ru.yml");
        loadMessages(PlayerLanguage.EN, "messages_en.yml");
        loadPlayerLanguages();
    }

    public PlayerLanguage getDefaultLanguage() {
        return defaultLanguage;
    }

    public PlayerLanguage getLanguage(CommandSender sender) {
        if (sender instanceof Player player) {
            return getLanguage(player);
        }
        return defaultLanguage;
    }

    public PlayerLanguage getLanguage(Player player) {
        if (player == null) {
            return defaultLanguage;
        }

        String value = playerLanguages != null ? playerLanguages.getString(languagePath(player.getUniqueId().toString())) : null;
        return PlayerLanguage.fromCode(value == null ? defaultLanguage.getCode() : value);
    }

    public void setLanguage(Player player, PlayerLanguage language) {
        if (player == null || language == null) {
            return;
        }

        if (playerLanguages == null) {
            loadPlayerLanguages();
        }

        playerLanguages.set(languagePath(player.getUniqueId().toString()), language.getCode());
        savePlayerLanguages();
    }

    public String get(CommandSender sender, String key, Object... replacements) {
        return get(getLanguage(sender), key, replacements);
    }

    public String get(PlayerLanguage language, String key, Object... replacements) {
        String template = findMessage(language, key);
        return colorize(applyReplacements(template, replacements));
    }

    public List<String> getList(CommandSender sender, String key, Object... replacements) {
        return getList(getLanguage(sender), key, replacements);
    }

    public List<String> getList(PlayerLanguage language, String key, Object... replacements) {
        FileConfiguration config = messages.getOrDefault(language, messages.get(defaultLanguage));
        List<String> raw = resolveMessageList(config, key);
        if (raw.isEmpty()) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>(raw.size());
        for (String line : raw) {
            resolved.add(colorize(applyReplacements(line, replacements)));
        }
        return resolved;
    }

    private void loadMessages(PlayerLanguage language, String resourceName) {
        ensureBundledFile(resourceName);

        File file = new File(plugin.getDataFolder(), resourceName);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        if (plugin.getResource(resourceName) != null) {
            configuration.setDefaults(
                YamlConfiguration.loadConfiguration(
                    new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8)
                )
            );
        }

        messages.put(language, configuration);
    }

    private void loadPlayerLanguages() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!playerLanguagesFile.exists()) {
            try {
                playerLanguagesFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to create " + playerLanguagesFile.getName() + ": " + exception.getMessage());
            }
        }
        playerLanguages = YamlConfiguration.loadConfiguration(playerLanguagesFile);
    }

    private void savePlayerLanguages() {
        if (playerLanguages == null) {
            return;
        }
        try {
            playerLanguages.save(playerLanguagesFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save " + playerLanguagesFile.getName() + ": " + exception.getMessage());
        }
    }

    private void ensureBundledFile(String resourceName) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    private String findMessage(PlayerLanguage language, String key) {
        FileConfiguration preferred = messages.getOrDefault(language, messages.get(defaultLanguage));
        String preferredValue = resolveMessage(preferred, key);
        if (preferredValue != null) {
            return preferredValue;
        }

        FileConfiguration fallback = messages.get(defaultLanguage);
        String fallbackValue = resolveMessage(fallback, key);
        if (fallbackValue != null) {
            return fallbackValue;
        }
        return key;
    }

    private String resolveMessage(FileConfiguration configuration, String key) {
        if (configuration == null) {
            return null;
        }

        String value = configuration.getString(key);
        if (value != null) {
            return value;
        }

        FileConfiguration defaults = (FileConfiguration) configuration.getDefaults();
        return defaults != null ? defaults.getString(key) : null;
    }

    private List<String> resolveMessageList(FileConfiguration configuration, String key) {
        if (configuration == null) {
            return List.of();
        }

        List<String> value = configuration.getStringList(key);
        if (!value.isEmpty()) {
            return value;
        }

        FileConfiguration defaults = (FileConfiguration) configuration.getDefaults();
        if (defaults == null) {
            return List.of();
        }

        List<String> fallback = defaults.getStringList(key);
        return fallback.isEmpty() ? List.of() : fallback;
    }

    private String applyReplacements(String template, Object... replacements) {
        String resolved = template == null ? "" : template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = String.valueOf(replacements[i]);
            Object value = replacements[i + 1];
            resolved = resolved.replace("{" + key + "}", value == null ? "" : String.valueOf(value));
        }
        return resolved;
    }

    private String languagePath(String uuid) {
        return PLAYER_LANGUAGE_PATH + "." + uuid;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
