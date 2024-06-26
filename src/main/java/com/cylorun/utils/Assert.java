package com.cylorun.utils;

import com.cylorun.Tracker;
import org.apache.logging.log4j.Level;

public class Assert {

    public static void isTrue(boolean condition) {
        if (!condition) {
            Tracker.log(Level.WARN, "Assertion failed");
            throw new AssertionError();
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            Tracker.log(Level.WARN, "Assertion failed -> " + message);
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition) {
        if (condition) {
            Tracker.log(Level.WARN, "Assertion failed");
            throw new AssertionError();
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            Tracker.log(Level.WARN, "Assertion failed-> " + message);
            throw new AssertionError(message);
        }
    }

    public static void isEqual(Object value1, Object value2) {
        if (value1 != value2) {
            Tracker.log(Level.WARN, "Assertion failed");
            throw new AssertionError();
        }
    }

    public static void isEqual(Object value1, Object value2, String message) {
        if (value1 != value2) {
            Tracker.log(Level.WARN, "Assertion failed-> " + message);
            throw new AssertionError(message);
        }
    }

    public static void isNotNull(Object object) {
        if (object == null) {
            Tracker.log(Level.WARN,"Assertion failed");
        }
    }

    public static void isNotNull(Object object, String message) {
        if (object == null) {
            Tracker.log(Level.WARN,"Assertion failed -> "+message);
        }
    }
}