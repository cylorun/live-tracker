package com.cylorun;

import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.ThreadUtil;
import com.cylorun.utils.UpdateUtil;
import com.formdev.flatlaf.FlatDarculaLaf;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TrackerAppLaunch {
    public static List<String> args;

    public static void main(String[] args) {
        TrackerAppLaunch.args = Arrays.asList(args);
        ToolTipManager.sharedInstance().setInitialDelay(0);

        System.setProperty("tracker.dir", TrackerOptions.getTrackerDir().toString());

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        FlatDarculaLaf.setup();

        UpdateUtil.checkForUpdates(Tracker.VERSION);

        TrackerAppLaunch.parseArgs(TrackerAppLaunch.args);
        Tracker.run();
    }

    private static void parseArgs(List<String> args) {
        if (args.contains("-deleteOldJar")) {
            TrackerAppLaunch.deleteOldJar();
        }

        if (args.contains("-portable")) {
            TrackerAppLaunch.runAsPortable();
        }
    }

    private static void runAsPortable() {
        Tracker.log(Level.INFO, "Running as portable");
    }

    private static void deleteOldJar() {
        File toDelete = new File(args.get(args.indexOf("-deleteOldJar") + 1));
        Tracker.log(Level.INFO, "Deleting old jar " + toDelete.getName());

        for (int i = 0; i < 200 && !toDelete.delete(); i++) {
            ThreadUtil.sleep(10);
        }

        if (toDelete.exists()) {
            Tracker.log(Level.ERROR, "Failed to delete " + toDelete.getName());
        } else {
            Tracker.log(Level.INFO, "Deleted " + toDelete.getName());
        }

    }
}
