package me.cylorun.utils;

public class Assert {

    public static void isTrue(boolean condition) {
        if (!condition) {
            ExceptionUtil.showError(new AssertionError());
            throw new AssertionError();
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            ExceptionUtil.showError(new AssertionError(message));
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition) {
        if (condition) {
            ExceptionUtil.showError(new AssertionError());
            throw new AssertionError();
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            ExceptionUtil.showError(new AssertionError(message));
            throw new AssertionError(message);
        }
    }

    public static void isEqual(Object value1, Object value2) {
        if (value1 != value2) {
            ExceptionUtil.showError(new AssertionError());
            throw new AssertionError();
        }
    }

    public static void isEqual(Object value1, Object value2, String message) {
        if (value1 != value2) {
            ExceptionUtil.showError(new AssertionError(message));
            throw new AssertionError(message);
        }
    }

    public static void isNotNull(Object object) {
        if (object == null) {
            throw new AssertionError();
        }
    }

    public static void isNotNull(Object object, String message) {
        if (object == null) {
            throw new AssertionError(message);
        }
    }

}