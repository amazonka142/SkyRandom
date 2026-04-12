package dev.macuser.skyrandom;

import java.io.InputStream;
import java.util.Properties;
import org.bukkit.plugin.java.JavaPlugin;

public record BuildInfo(String version, int buildNumber, String buildDate) {

    public static BuildInfo load(JavaPlugin plugin) {
        Properties properties = new Properties();
        try (InputStream input = plugin.getResource("build-info.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
            // Fall back to plugin metadata when build info is missing.
        }

        String version = properties.getProperty("version", plugin.getDescription().getVersion());
        int buildNumber = parseBuildNumber(properties.getProperty("buildNumber"), version);
        String buildDate = properties.getProperty("buildDate", "unknown");
        return new BuildInfo(version, buildNumber, buildDate);
    }

    private static int parseBuildNumber(String rawBuildNumber, String version) {
        if (rawBuildNumber != null) {
            try {
                return Integer.parseInt(rawBuildNumber.trim());
            } catch (NumberFormatException ignored) {
                // Fall through to version parsing.
            }
        }

        int plusIndex = version.lastIndexOf('+');
        if (plusIndex >= 0 && plusIndex + 1 < version.length()) {
            try {
                return Integer.parseInt(version.substring(plusIndex + 1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }
}
