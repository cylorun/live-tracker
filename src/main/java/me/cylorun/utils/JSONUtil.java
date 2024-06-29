package me.cylorun.utils;

import com.google.gson.*;
import me.cylorun.Tracker;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.Optional;

public class JSONUtil {
    public static JsonObject parseFile(File file) {
        FileReader reader;

        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            Tracker.log(Level.ERROR, "Json file non existent: " + file.getAbsolutePath());
            return null;
        }

        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    public static JsonObject flatten(JsonObject jsonObject) { // collapses all children objects
        JsonObject flatJsonObject = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                JsonObject nestedObject = entry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> nestedEntry : nestedObject.entrySet()) {
                    flatJsonObject.add(nestedEntry.getKey(), nestedEntry.getValue());
                }
            } else {
                flatJsonObject.add(entry.getKey(), entry.getValue());
            }
        }

        return flatJsonObject;
    }

    public static String prettify(String data) {
        JsonElement e = JsonParser.parseString(data);
        Gson pretty = new GsonBuilder().setPrettyPrinting().create();
        return pretty.toJson(e);
    }

    public static Optional<String> getOptionalString(JsonObject obj, String member) {
        return Optional.ofNullable(obj.get(member))
                .map(JsonElement::getAsString);
    }

    public static Optional<Integer> getOptionalInt(JsonObject obj, String member) {
        return Optional.ofNullable(obj.get(member))
                .map(JsonElement::getAsInt);
    }

    public static Optional<JsonObject> getOptionalJsonObj(JsonObject obj, String member) {
        return Optional.ofNullable(obj.get(member))
                .map(JsonElement::getAsJsonObject);
    }
}
