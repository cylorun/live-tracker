package com.cylorun.io;

import com.cylorun.Tracker;
import com.cylorun.io.sheets.GoogleSheetsService;
import com.cylorun.utils.I18n;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.cylorun.io.sheets.GoogleSheetsClient;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TrackerOptions {

    public String sheet_id;
    public String api_key;
    public String api_url;
    public String sheet_name = "Raw Data";
    public String lang = "en_us";
    public Integer last_win_x = 0;
    public Integer last_win_y = 0;
    public boolean gen_labels = false;
    public boolean detect_ssg = true;
    public boolean only_track_completions = true;
    public boolean show_debug = false;
    public boolean generate_chunkmap = false;
    public boolean always_save_locally = false;
    public boolean upload_remote_server = false;
    public boolean upload_sheets = true;
    public boolean use_experimental_tracking = false; // use new stats like barters, map and shit
    public boolean advanced_editor_view = true;
    public int max_respawn_to_hr_time = 30; // seconds
    public int game_save_interval = 5; // seconds
    public int path_interval = 5; //secs
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_PATH = getTrackerDir().resolve("config.json");
    private static TrackerOptions instance;

    private TrackerOptions() {

    }

    public synchronized static TrackerOptions getInstance() {
        if (instance == null) {
            ensureTrackerDir();
            if (Files.exists(CONFIG_PATH)) {
                try {
                    instance = GSON.fromJson(new String((Files.readAllBytes(CONFIG_PATH))), TrackerOptions.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                instance = new TrackerOptions();
            }

        }
        return instance;
    }

    public static void ensureTrackerDir() {
        if (!getTrackerDir().toFile().exists()) {
            getTrackerDir().toFile().mkdirs();
        }
    }

    public static Path getTrackerDir() {
        return Paths.get(System.getProperty("user.home"), ".LiveTracker");
    }

    public static void save() {
        FileWriter writer;
        try {
            writer = new FileWriter(CONFIG_PATH.toFile());
            GSON.toJson(instance, writer);
            writer.close();
        } catch (IOException e) {
            Tracker.log(Level.ERROR, "Failed to write to config file");
            throw new RuntimeException();
        }
    }


    public static void validateSettings() {
        Tracker.log(Level.INFO, "Verifying settings");
        if (!Files.exists(Paths.get(GoogleSheetsService.CREDENTIALS_FILE))) {
            JOptionPane.showMessageDialog(null,"credentials.json file not found");
        } else if (getInstance().sheet_name == null || getInstance().sheet_name.isEmpty()) {
            JOptionPane.showMessageDialog(null,"sheet_name is not defined");
        } else if (getInstance().lang == null || getInstance().lang.isEmpty()) {
            JOptionPane.showMessageDialog(null,"Language not set");
        } else if (!I18n.isValidLanguage(getInstance().lang)) {
            JOptionPane.showMessageDialog(null,getInstance().lang + " is not a supported language");

        } else if (!GoogleSheetsClient.isValidSheet(getInstance().sheet_id, getInstance().sheet_name)) {
            JOptionPane.showMessageDialog(null,"Invalid sheet_id or sheet_name");
        } else {
            Tracker.log(Level.INFO, "Settings good");
        }

    }

}
