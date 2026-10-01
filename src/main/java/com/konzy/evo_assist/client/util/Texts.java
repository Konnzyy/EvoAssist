package com.konzy.evo_assist.client.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiFunction;

/** Uses Minecraft's current language at display time, with an English fallback for standalone tools. */
public final class Texts {
    private static final Properties ENGLISH = loadEnglish();
    private static volatile BiFunction<String, Object[], String> translator = Texts::english;

    private Texts() {}

    public static void useTranslator(BiFunction<String, Object[], String> resolver) {
        translator = Objects.requireNonNull(resolver);
    }

    public static String tr(String key, Object... args) {
        return translator.apply(key, args);
    }

    private static String english(String key, Object[] args) {
        return String.format(Locale.ROOT, ENGLISH.getProperty(key, key), args);
    }

    private static Properties loadEnglish() {
        Properties values = new Properties();
        try (var stream = Texts.class.getResourceAsStream("/evoassist-english.properties")) {
            if (stream != null) values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        return values;
    }
}
