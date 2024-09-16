package com.cylorun.gui.editor;

import com.cylorun.Tracker;
import com.cylorun.gui.TrackerFrame;
import com.cylorun.io.TrackerOptions;
import com.cylorun.io.dto.RunRecord;
import com.cylorun.utils.JSONUtil;
import com.cylorun.utils.ResourceUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RunEditor extends JPanel {
    private final RunRecord record;
    private final JButton saveButton;
    private final JButton viewButton;
    private boolean isFetching = false;
    private EditorView editorView;
    private Color prevColor = Color.WHITE;

    public RunEditor(RunRecord runRecord) {
        TrackerOptions options = TrackerOptions.getInstance();
        this.record = runRecord;
        this.setLayout(new BorderLayout());

        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.X_AXIS));
        JButton backButton = new JButton("Back");
        try {
            backButton.setIcon(new ImageIcon(ResourceUtil.loadImageResource("icons/back.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        backButton.addActionListener((e) -> TrackerFrame.getInstance().resetToInitialView());
        this.editorView = EditorView.getView(this.record, this.record, this::onChange);
        this.viewButton = new JButton(options.advanced_editor_view ? "Basic" : "Advanced");
        this.viewButton.addActionListener((e) -> this.toggleViewType());

        topBar.add(backButton);
        topBar.add(Box.createRigidArea(new Dimension(10, 0)));
        topBar.add(new JLabel(String.format("Run %s", this.record.get("run_id").getAsInt())));
        topBar.add(Box.createHorizontalGlue());
        topBar.add(this.viewButton, BorderLayout.EAST);
        this.add(topBar, BorderLayout.NORTH);
        this.add(new JScrollPane(this.editorView), BorderLayout.CENTER);

        this.saveButton = new JButton("Save");
        this.saveButton.setEnabled(false);

        this.saveButton.addActionListener((e -> this.handleSaveButtonAction()));

        this.fetchData();

        TrackerFrame.getInstance().setView(this);
    }

    private void toggleViewType() {
        TrackerOptions options = TrackerOptions.getInstance();
        options.advanced_editor_view = !options.advanced_editor_view;
        TrackerOptions.save();

        this.viewButton.setText(options.advanced_editor_view ? "Basic" : "Advanced");
        this.setEditorView(options.advanced_editor_view ?
                new AdvancedEditorView(this.runData, this.record, (hasChanges) -> this.onChange(hasChanges)) :
                new BasicEditorView(this.runData, this.record, (hasChanges) -> this.onChange(hasChanges))
        );
    }

    private void onChange(boolean hasChanges) {
        this.saveButton.setEnabled(hasChanges);
    }

    private void setEditorView(EditorView view) {
        this.editorView = view;

        this.repaint();
        this.revalidate();
    }


    private void handleSaveButtonAction() {

    }

    public List<String> getAllValueKeys() {
        List<String> keys = new ArrayList<>();
        extractKeys(this.runData, keys);
        return keys;
    }

    public static void extractKeys(JsonObject jsonObject, List<String> keys) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue().isJsonPrimitive() || entry.getValue().isJsonNull()) {
                keys.add(entry.getKey());
            }
            if (entry.getValue().isJsonObject()) {
                extractKeys(entry.getValue().getAsJsonObject(), keys);
            }
        }
    }

    private boolean editRun(String column, String value) {
        OkHttpClient client = new OkHttpClient();
        JsonObject o = new JsonObject();
        String id = this.record.get("run_id").getAsString();
        o.addProperty("column", column);
        o.addProperty("value", value);
        o.addProperty("id", id);

        RequestBody body = RequestBody.create(o.toString(), MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder().url(TrackerOptions.getInstance().api_url + "/runs/" + id).put(body).addHeader("authorization", TrackerOptions.getInstance().api_key).build();

        try (Response res = client.newCall(req).execute()) {
            return res.code() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private Color getColorFromRun(JsonObject run) {
        if (run.get("color").isJsonNull() || run.get("color") == null) {
            return Color.WHITE;
        }
        return getColorFromString(run.get("color").getAsString());
    }

    private Color getColorFromString(String s) {
        Integer[] rgb = Arrays.stream(s.split(",")).map((e) -> Integer.parseInt(e.strip())).toArray(Integer[]::new);
        if (rgb.length != 3) {
            Tracker.log(Level.ERROR, "Invalid color string: " + s);
            return Color.WHITE;
        }
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    private void fetchData() {
        if (this.isFetching) {
            return;
        }
        this.isFetching = true;

        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder().url(TrackerOptions.getInstance().api_url + "/runs/" + record.get("run_id").getAsString()).get().build();

                try (Response res = client.newCall(req).execute()) {
                    String jsonData = res.body().string();
                    return JsonParser.parseString(jsonData).getAsJsonObject();
                } catch (Exception e) {
                    Tracker.log(Level.ERROR, "Failed to fetch data: " + e);
                }
                return null;
            }

            @Override
            protected void done() {
                isFetching = false;
                try {
                    JsonObject r = this.get();
                    if (r == null) {
                        Tracker.log(Level.ERROR, "Failed to fetch data");
                        return;
                    }

                    runData = JSONUtil.flatten(r);
                    String[] values = getAllValueKeys().toArray(new String[0]);
//                    columnField.setOptions(values);
                    Color color = getColorFromRun(runData);
                    prevColor = color;
//                    colorChooser.setColor(color);
                } catch (InterruptedException | ExecutionException e) {
                    Tracker.log(Level.ERROR, "Failed to process run data: " + e);
                }
            }
        }.execute();
    }
}
