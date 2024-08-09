package com.cylorun.instance.world;

import com.cylorun.Tracker;
import com.cylorun.instance.WorldFile;
import com.cylorun.utils.JSONUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class WorldCreationEventHandler extends Thread {
    public List<String> previousWorlds;
    private String lastPath = "";
    private File lwjson = Paths.get(System.getProperty("user.home")).resolve("speedrunigt").resolve("latest_world.json").toFile();

    public WorldCreationEventHandler() {
        super("WorldCreationEventHandler");
        this.previousWorlds = new ArrayList<>();
        this.start();
    }

    private List<Consumer<WorldFile>> listeners = new ArrayList<>();

    public void addListener(Consumer<WorldFile> listener) {
        listeners.add(listener);
    }

    private String getLastWorldPath() {
        FileReader reader = null;
        try {
            reader = new FileReader(lwjson);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonElement element = JsonParser.parseReader(reader);
        return element.getAsJsonObject().get("world_path").getAsString();
    }

    public void notifyListeners(WorldFile world) {
        for (Consumer<WorldFile> listener : listeners) {
            listener.accept(world);
        }
    }

    @Override
    public void run() {
        this.lastPath = getLastWorldPath(); // so that it wont detect old worlds

        while (true) {
            String newPath = getLastWorldPath();
            if (!Objects.equals(newPath, this.lastPath) && !this.previousWorlds.contains(newPath)) { // makes sure that a world wont be tracked multiple times
                this.lastPath = newPath;

                JsonObject recordJson = JSONUtil.parseFile(Paths.get(newPath).resolve("speedrunigt").resolve("record.json").toFile());
                // that file will usually not exist unless the world has already been loaded before
                if (recordJson != null && recordJson.get("category").getAsString().equals("pratice_world")) { // yes i spelled it right
                    Tracker.log(Level.INFO, String.format("Practice world %s detected, will not track", new File(newPath).getName()));
                    continue;
                }

                this.previousWorlds.add(newPath);
                this.notifyListeners(new WorldFile(this.lastPath));
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
