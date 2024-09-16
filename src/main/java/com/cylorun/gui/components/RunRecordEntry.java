package com.cylorun.gui.components;

import com.cylorun.Tracker;
import com.cylorun.io.dto.RunRecord;
import com.google.gson.JsonObject;
import com.cylorun.gui.editor.RunEditor;
import com.cylorun.io.TrackerOptions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RunRecordEntry extends JPanel {
    private final JButton deleteButton;
    private final JButton editButton;
    private final JButton viewButton;
    private final RunRecord record;

    public RunRecordEntry(RunRecord record) {
        this.setLayout(new BorderLayout());
        this.deleteButton = new JButton("Delete");
        this.editButton = new JButton("Edit");
        this.viewButton = new JButton("View");
        this.record = record;
        Date date = new Date(this.record.date_played_est);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

        JLabel runIdLabel = new JLabel(String.format("<html>Run Id: <b>%s<b> </html>", this.record.run_id));
        runIdLabel.setToolTipText(String.format("Date Played: %s\n World Name: %s", dateFormat.format(date), this.record.world_name));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(this.viewButton);
        buttonPanel.add(this.editButton);
        buttonPanel.add(this.deleteButton);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPanel.add(runIdLabel, BorderLayout.WEST);
        contentPanel.add(buttonPanel, BorderLayout.EAST);

        this.add(contentPanel, BorderLayout.CENTER);

        this.deleteButton.addActionListener((e -> {
            int option = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to delete run " + this.record.run_id,
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                if (!this.deleteRun()) {
                    Tracker.log(Level.WARN, "Failed to delete run " + this.record.run_id);
                    JOptionPane.showMessageDialog(null, "Failed to delete run");
                }
            }
        }));

        this.editButton.addActionListener((e) -> {
            this.editRun();
        });
    }

    private void editRun() {
        new RunEditor(this.record);
    }

    private boolean deleteRun() {
        OkHttpClient client = new OkHttpClient();

        Request req = new Request.Builder()
                .url(TrackerOptions.getInstance().api_url + "/runs/" + this.record.get("run_id").getAsInt())
                .delete()
                .addHeader("authorization", TrackerOptions.getInstance().api_key)
                .build();

        Response res;

        try {
            res = client.newCall(req).execute();
        } catch (IOException e) {
            return false;
        }

        return res.code() == 200;
    }
}
