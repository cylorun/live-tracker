package com.cylorun;

import com.cylorun.gui.TrackerFrame;
import com.cylorun.instance.Run;
import com.cylorun.instance.WorldFile;
import com.cylorun.instance.world.WorldCreationEventHandler;
import com.cylorun.io.TrackerOptions;
import com.cylorun.io.sheets.GoogleSheetsClient;
import com.cylorun.map.ChunkMap;
import com.cylorun.utils.APIUtil;
import com.cylorun.utils.ExceptionUtil;
import com.cylorun.utils.LogReceiver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;


public class Tracker {

    public static final String VERSION = Tracker.class.getPackage().getImplementationVersion() == null ? "DEV" : Tracker.class.getPackage().getImplementationVersion();
    private static final Logger LOGGER = LogManager.getLogger(Tracker.class);

    public static void run() {
        Tracker.log(Level.INFO, "Running Live-Tracker-" + VERSION);

        List<WorldFile> worlds = new ArrayList<>();
        TrackerFrame.getInstance().open();
        new Thread(GoogleSheetsClient::setup, "google-sheets-setup").start();

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            log(Level.ERROR, "Uncaught exception caught.");
            onCrash(e);
        });

        WorldCreationEventHandler worldHandler = new WorldCreationEventHandler(); // only one WorldFile object should be created per world path
        worldHandler.addListener(world -> {
            Tracker.log(Level.DEBUG, "New world detected: " + world);
            if (!worlds.contains(world)) {
                worlds.add(world);
            }

            if (worlds.size() > 1) {
                WorldFile prev = worlds.get(worlds.size() - 2);
                worlds.remove(prev);
                prev.onCompletion(); // since a new world has been created this one can be abandoned
            }

            handleWorld(world);
        });
    }

    public static void stop() {
        log(Level.INFO, "Stopping...");
        System.exit(0);
    }

    private static void handleWorld(WorldFile world) {
        TrackerOptions options = TrackerOptions.getInstance();
        world.setCompletionHandler(() -> {
            Run run;
            try {
                run = new Run(world);
            } catch (IOException e) {
                Tracker.log(Level.ERROR, "Failed to track run: " + e.getMessage());
                onCrash(e);
                return;
            }

            if (run.shouldPush()) {
                run.gatherAllData();

                if (options.always_save_locally) {
                    if (run.save(TrackerOptions.getTrackerDir().resolve("local"))) {
                        Tracker.log(Level.INFO, "Saved run locally");
                    } else {
                        Tracker.log(Level.ERROR, "Failed to save run locally");
                    }
                }

                APIUtil.tryUploadRun(run);
                if (options.upload_sheets) {
                    try {
                        GoogleSheetsClient.appendRowTop(run);
                    } catch (IOException | GeneralSecurityException e) {
                        e.printStackTrace();
                        Tracker.log(Level.ERROR, "Failed to upload run to google sheets: " + e.getMessage());
                        Tracker.log(Level.ERROR, "Stacktrace: " + ExceptionUtil.toDetailedString(e));
                    } catch (NullPointerException e) {
                        Tracker.log(Level.ERROR, "No provided sheet id or name");
                    }
                }
            }

            if (options.generate_chunkmap) {
                new Thread(() -> {
                    ChunkMap cm = new ChunkMap(world.getSeed(), 500, kaptainwutax.mcutils.state.Dimension.OVERWORLD, world);
                    cm.generate();
                    cm.setDimension(kaptainwutax.mcutils.state.Dimension.NETHER).generate();
                }, "ChunkMapGen").start();
            }
        });
    }

    public static void onCrash(Throwable t) {
        log(Level.ERROR, "UNCAUGHT TRACKER ERROR!!!!!!!!!!!");
        log(Level.ERROR, "MESSSAGE: " + t.getMessage());
        log(Level.ERROR, "STACKTRACE: " + ExceptionUtil.toDetailedString(t));
        log(Level.ERROR, "PLEASE REPORT THIS TO @cylorun AND RESTART THE TRACKER");
    }


    public static Path getSourcePath() {
        try {
            return Paths.get(Tracker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(Level level, Object o) {
        LOGGER.log(level, o);
        LogReceiver.log(level, o);
    }
}