package com.cylorun.utils;

import com.cylorun.Tracker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.cylorun.io.TrackerOptions;
import org.apache.logging.log4j.Level;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MinecraftTranslations {

    private static JsonObject langJson;
    private static final JsonObject DEFAULT_LANG = ResourceUtil.loadJsonResource(MinecraftTranslations.class.getClassLoader().getResource("translations/en_us.json"));

    private static JsonObject getCurrLang() {
        if (langJson == null) {
            String lang = TrackerOptions.getInstance().lang;
            URL url = Tracker.class.getClassLoader().getResource("translations/" + lang + ".json");
            if (url == null) {
                Tracker.log(Level.ERROR, "Resource not found: translations/" + lang + ".json. Falling back to default language.");
                langJson = DEFAULT_LANG;
            } else {
                langJson = ResourceUtil.loadJsonResource(url);
            }
        }

        return langJson;
    }

    public static JsonObject getAllDeathMessages() {
        JsonElement deaths = getCurrLang().get("deaths");
        return deaths != null ? deaths.getAsJsonObject() : DEFAULT_LANG.getAsJsonObject("deaths");
    }

    public static List<String> getSupported() {
        List<String> supported = new ArrayList<>();
        supported.add("en_us");
        return supported;
    }

    public static boolean isValidLanguage(String lang) {
        if (lang == null) {
            return false;
        }
        return getSupported().contains(lang.trim());
    }

    public static String get(String key) {
        JsonElement translation = getCurrLang().get(key);
        if (translation == null || translation.isJsonNull()) {
            Tracker.log(Level.WARN, "Missing translation for key: " + key + ". Falling back to default language.");
            translation = DEFAULT_LANG.get(key);
        }

        return translation != null ? translation.getAsString() : key;
    }
}
