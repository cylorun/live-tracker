package com.cylorun.gui;

import com.cylorun.Tracker;
import com.cylorun.gui.components.ActionButton;
import com.cylorun.gui.components.RunRecordEntry;
import com.cylorun.io.dto.RunRecord;
import com.cylorun.utils.APIUtil;
import com.cylorun.utils.ResourceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.cylorun.io.TrackerOptions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RunPanel extends JPanel {
    private final JPanel runRecordPanel;
    private final JButton refreshButton;
    private boolean isFetching = false;

    public RunPanel() {
        this.runRecordPanel = new JPanel();
        this.refreshButton = new JButton("Reload");
        try {
            this.refreshButton.setIcon(new ImageIcon(ResourceUtil.loadImageResource("icons/reload.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.add(this.refreshButton);
        this.runRecordPanel.setLayout(new BoxLayout(this.runRecordPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(this.runRecordPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollPane.getVerticalScrollBar().setUnitIncrement(8);
        scrollPane.setPreferredSize(new Dimension(370, 300));

        this.add(scrollPane);
        this.runRecordPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        this.refreshButton.addActionListener((e -> {
            this.fetchData();
        }));

        if (TrackerOptions.getInstance().upload_remote_server) {
            this.fetchData();
        }
    }

    private void toggleButtonLoading() {
        if (this.isFetching) {
            this.refreshButton.setText("Loading...");
            this.refreshButton.setEnabled(false);
        } else {
            this.refreshButton.setText("Reload");
            this.refreshButton.setEnabled(true);
        }
    }

    private void fetchData() {
        if (this.isFetching) {
            return;
        }

        if (!APIUtil.isValidUrl(TrackerOptions.getInstance().api_url)) {
            Tracker.log(Level.WARN, "Invalid API Url");
            return;
        }
        this.isFetching = true;
        this.toggleButtonLoading();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(TrackerOptions.getInstance().api_url + "/runs/today")
                .build();

        new SwingWorker<RunRecord[], Void>() {
            @Override
            protected RunRecord[] doInBackground() {
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Tracker.log(Level.ERROR, "Failed to fetch run data, status code: " + response.code());
                        response.close();
                        return null;
                    }

                    String jsonString = response.body().string();
                    response.close();
                    ObjectMapper mp = new ObjectMapper();

                    RunRecord[] runs = mp.convertValue(jsonString, RunRecord[].class);
                    return runs;
                } catch (Exception e) {
                    Tracker.log(Level.ERROR, "Failed to fetch run data: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                isFetching = false;
                try {
                    RunRecord[] runs = get();
                    runRecordPanel.removeAll();
                    if (runs == null) {
                        runRecordPanel.add(new JLabel("Something went wrong"));
                        runRecordPanel.add(new ActionButton("Try again", e -> fetchData()));
                    }  else if(runs.equals(new JsonArray())) {
                        runRecordPanel.add(new JLabel("There are no runs to display"));
                    } else {
                        for (RunRecord run : runs) {
                            RunRecordEntry entry = new RunRecordEntry(run);
                            JPanel entryPanel = new JPanel(new BorderLayout());
                            entryPanel.add(entry, BorderLayout.NORTH);
                            entryPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                            runRecordPanel.add(entryPanel);
                        }
                    }

                    runRecordPanel.revalidate();
                    runRecordPanel.repaint();
                    toggleButtonLoading();
                } catch (Exception e) {
                    Tracker.log(Level.ERROR, "Failed to process fetched data: " + e);
                }
            }
        }.execute();
    }

}
