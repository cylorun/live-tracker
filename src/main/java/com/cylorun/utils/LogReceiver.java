package com.cylorun.utils;

import com.cylorun.gui.TrackerFrame;
import com.cylorun.TrackerOptions;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogReceiver {
    public static void log(Level level, Object o) {
        String s = String.format("[%s/%s]: %s\n", level.toString(), nowTime(), o.toString());
        if (level != Level.DEBUG || TrackerOptions.getInstance().show_debug) {
            SwingUtilities.invokeLater(() -> TrackerFrame.getInstance().appendLog(s, getLevelColor(level)));
        }
    }

    public static String getLevelColor(Level l) {
        if (l.equals(Level.WARN)) {
            return "#fcc419";
        } else if (l.equals(Level.ERROR)) {
            return "#ff6b6b";
        } else if(l.equals(Level.DEBUG)) {
            return "#808080";
        } else {
            return "#FFFFFF";
        }
    }

    private static String nowTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return now.format(formatter);
    }
}
