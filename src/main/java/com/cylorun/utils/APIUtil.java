package com.cylorun.utils;

import com.cylorun.Tracker;
import com.cylorun.instance.Run;
import com.cylorun.io.TrackerOptions;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class APIUtil {

    private static int uploadRun(Run run) {
        TrackerOptions options = TrackerOptions.getInstance();
        OkHttpClient client = new OkHttpClient();

        String bodyJson = getRunJson(run);
        Tracker.log(Level.DEBUG, "Uploading " + bodyJson);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), bodyJson);
        Request request = new Request.Builder()
                .url(TrackerOptions.getInstance().api_url + "/runs")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("authorization", options.api_key)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return 502;
        }
        int code = response.code();
        response.close();
        return code;
    }

    public static String getRunJson(Run run) {
        String runJson = new Gson().toJson(run);
        return String.format("{\"run\":%s}", runJson);
    }

    public static void tryUploadRun(Run run) {
        if (!TrackerOptions.getInstance().upload_remote_server) {
            return;
        }
        if (!APIUtil.isValidKey(TrackerOptions.getInstance().api_key)) {
            Tracker.log(Level.WARN, "Invalid API key or none provided, will not upload run");
            return;
        }

        int retries = 0;
        int code;
        while ((code = uploadRun(run)) != 200) {
            Tracker.log(Level.ERROR, "Failed to upload run, trying again. code: " + code);

            if (retries++ > 5) {
                Tracker.log(Level.ERROR, "Failed to upload run, saving locally...");
                Path savePath = TrackerOptions.getTrackerDir().resolve("local");
                if (!run.save(savePath, true)) {
                    Tracker.log(Level.ERROR, "Failed to save run locally");
                } else {
                    Tracker.log(Level.INFO, "Saved run locally");
                }

                break;
            }
        }

        if (code == 200) {
            Tracker.log(Level.INFO, "Successfully uploaded run");
        }
    }

    public static boolean isValidUrl(String url) {
        if (url == null) {
            return false;
        }
        String regex = "\\bhttps?:\\/\\/((localhost:\\d+)|(localhost)|([\\w.-]+\\.[a-z]{2,}))(:\\d+)?(\\/[^\\s]*)?\\b";
        Pattern p = Pattern.compile(regex);
        return p.matcher(url).groupCount() > 1; // bad
    }

    public static boolean isValidKey(String key) {
        if (!isValidApiUrl(TrackerOptions.getInstance().api_url)) {
            Tracker.log(Level.WARN, "Provided API url is invalid, can't verify key");
            return false;
        }

        if (key == null) {
            Tracker.log(Level.WARN, "No API key provided");
            return false;
        }

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(TrackerOptions.getInstance().api_url + "/verify")
                .addHeader("authorization", key)
                .build();

        try (Response res = client.newCall(req).execute()) {
            if (res.code() == 429) {
                Tracker.log(Level.WARN, "Ratelimit exceeded, try again in 15 minutes");
                return false;
            }

            if (res.code() != 200) {
                Tracker.log(Level.WARN, "Invalid API key");
            }

            return res.code() == 200;
        } catch (IOException e) {
            Tracker.log(Level.ERROR, "Failed to verify key");
            return false;
        }
    }

    public static boolean isValidApiUrl(String url) {
        if (url == null) {
            Tracker.log(Level.WARN, "No API Url provided");
            return false;
        }

        if (!isValidUrl(url)) {
            Tracker.log(Level.WARN, "Invalid URL Format");
            return false;
        }

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url + "/ping")
                .get()
                .build();

        try (Response res = client.newCall(req).execute()) {
            if (res.code() != 200) {
                Tracker.log(Level.WARN, "Invalid API key");
            }

            return res.code() == 200;
        } catch (IOException | IllegalArgumentException e) {
            Tracker.log(Level.ERROR, "Failed to verify URL: " + e.getMessage());
            return false;
        }
    }
}
