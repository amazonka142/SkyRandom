package dev.macuser.skyrandom.lang;

import java.util.Locale;

public enum PlayerLanguage {
    RU("ru"),
    EN("en");

    private final String code;

    PlayerLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PlayerLanguage fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return RU;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (PlayerLanguage language : values()) {
            if (language.code.equals(normalized)) {
                return language;
            }
        }
        return RU;
    }
}
