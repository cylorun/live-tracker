package com.cylorun.gui;

import com.cylorun.Tracker;
import com.cylorun.gui.components.MultiChoiceOptionField;
import com.cylorun.gui.components.TextEditor;
import com.cylorun.gui.components.TextOptionField;
import com.cylorun.utils.JSONUtil;
import com.cylorun.utils.ResourceUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.cylorun.gui.components.ColorPicker;
import com.cylorun.io.TrackerOptions;
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
    private final JsonObject record;
    private final JPanel configPanel;
    private final MultiChoiceOptionField columnField;
    private TextOptionField valueField;
    private TextEditor textEditorField;
    private ColorPicker colorChooser;
    private final JButton saveButton;
    private JButton viewButton;
    private JsonObject runData;
    private boolean isFetching = false;
    private Color prevColor = Color.WHITE;

    public RunEditor(JsonObject runRecord) {
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

        topBar.add(backButton);
        topBar.add(Box.createRigidArea(new Dimension(10, 0)));
        topBar.add(new JLabel(String.format("Run %s", this.record.get("run_id").getAsInt())));

        backButton.addActionListener((e) -> TrackerFrame.getInstance().resetToInitialView());
        this.viewButton = new JButton(options.advanced_editor_view ? "Basic" : "Advanced");
        this.viewButton.addActionListener((e) -> this.toggleViewType());
        topBar.add(Box.createHorizontalGlue());
        topBar.add(this.viewButton, BorderLayout.EAST);

        this.add(topBar, BorderLayout.NORTH);

        this.configPanel = new JPanel();
        this.configPanel.setLayout(new BoxLayout(this.configPanel, BoxLayout.Y_AXIS));
        this.add(new JScrollPane(this.configPanel), BorderLayout.CENTER);

        this.saveButton = new JButton("Save");
        this.saveButton.setEnabled(false);

        this.colorChooser = new ColorPicker();
        this.textEditorField = new TextEditor((val) -> this.checkForChanges());
        this.colorChooser.addConsumer((newCol -> this.checkForChanges()));

        this.columnField = new MultiChoiceOptionField(new String[]{}, "run_id", "Column", (val) -> {
            this.valueField.setVisible(false);
            this.textEditorField.setVisible(false);
            if (val.equals("notes")) {
                this.textEditorField.setValue(JSONUtil.getOptionalString(this.runData, val).orElse(""));
                this.textEditorField.setVisible(true);
            } else {
                this.valueField.setValue(JSONUtil.getOptionalString(this.runData, val).orElse(""));
                this.valueField.setVisible(true);
            }
            this.checkForChanges();
        });

        this.valueField = new TextOptionField("Value", this.record.get("run_id").getAsString(), (val) -> {
            if (this.runData == null) {
                return;
            }
            this.checkForChanges();
        });

        this.configPanel.add(this.columnField);
        this.configPanel.add(this.valueField);
        this.configPanel.add(this.textEditorField);
        this.configPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        this.configPanel.add(new JLabel("Run Color"));
        this.configPanel.add(this.colorChooser);
        this.configPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        this.configPanel.add(Box.createVerticalStrut(10));
        this.configPanel.add(this.saveButton);
        this.configPanel.add(Box.createVerticalStrut(10));

        this.saveButton.addActionListener((e -> this.handleSaveButtonAction()));

        this.fetchData();

        TrackerFrame.getInstance().setView(this);
    }

    private void toggleViewType() {
        TrackerOptions options = TrackerOptions.getInstance();
        options.advanced_editor_view = !options.advanced_editor_view;
        TrackerOptions.save();

        this.viewButton.setText(options.advanced_editor_view ? "Basic" : "Advanced");
    }

    private void checkForChanges() {
        boolean hasColorChanged = !this.prevColor.equals(this.colorChooser.getCurrentColor());
        boolean hasValueChanged = !this.valueField.getValue().equals(JSONUtil.getOptionalString(this.runData,this.columnField.getValue()).orElse(""));
        this.saveButton.setEnabled(hasColorChanged || hasValueChanged);
    }

    private void handleSaveButtonAction() {
        if (!this.prevColor.equals(this.colorChooser.getCurrentColor()) && JOptionPane.showConfirmDialog(
                null,
                "Change color for run " + this.record.get("run_id").getAsString(),
                "confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
        ) {
            this.prevColor = this.colorChooser.getCurrentColor();
            this.editRun("color", this.colorChooser.getColorString());
            Tracker.log(Level.INFO, "Successfully edited color for run " + this.record.get("run_id").getAsString());
            this.checkForChanges();
        } else {
            int option = JOptionPane.showConfirmDialog(
                    null,
                    String.format("Change %s from %s to %s for run %s?",
                            this.columnField.getValue(),
                            JSONUtil.getOptionalString(this.runData, this.columnField.getValue()).orElse("\" \""),
                            this.columnField.getValue().equals("notes") ? this.textEditorField.getText() : this.valueField.getValue(),
                            this.record.get("run_id").getAsString()),
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                this.saveButton.setEnabled(false);
                String value = columnField.getValue().equals("notes") ? this.textEditorField.getHtmlText() : this.valueField.getValue();
                if (this.editRun(this.columnField.getValue(), value)) {
                    Tracker.log(Level.INFO, "Successfully edited run " + this.record.get("run_id").getAsString());
                } else {
                    Tracker.log(Level.ERROR, "Failed to edit run " + this.record.get("run_id").getAsString());
                }
            }
        }
    }

    public List<String> getAllValueKeys() {
        List<String> keys = new ArrayList<>();
        extractKeys(this.runData, keys);
        return keys;
    }

    private void extractKeys(JsonObject jsonObject, List<String> keys) {
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
                    columnField.setOptions(values);
                    Color color = getColorFromRun(runData);
                    prevColor = color;
                    colorChooser.setColor(color);
                } catch (InterruptedException | ExecutionException e) {
                    Tracker.log(Level.ERROR, "Failed to process run data: " + e);
                }
            }
        }.execute();
    }
}
