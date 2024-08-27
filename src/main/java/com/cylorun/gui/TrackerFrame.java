package com.cylorun.gui;

import com.cylorun.Tracker;
import com.cylorun.gui.components.*;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.APIUtil;
import com.cylorun.utils.I18n;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TrackerFrame extends JFrame implements WindowListener {

    private static TrackerFrame instance;
    private JTextArea logArea;
    private JPanel editorPanel;
    public JTabbedPane tabbedPane;
    private Container initialView;

    public TrackerFrame() {
        super("Live-Tracker " + Tracker.VERSION);

        this.setSize(750, 400);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(this);
        this.setLayout(new GridLayout(1, 2));

        this.initialView = this.getContentPane();
        this.tabbedPane = new JTabbedPane();
        this.add(this.getTextArea());

        this.editorPanel = new RunPanel();
        this.reload();
        this.setVisible(true);
    }

    private JScrollPane getTextArea() {
        this.logArea = new JTextArea();
        this.logArea.setEditable(false);
        this.logArea.setLineWrap(true);

        return new JScrollPane(this.logArea);
    }

    private synchronized void reload() {
        int idx = this.tabbedPane.getSelectedIndex();
        this.reloadTabbedPane();
        this.tabbedPane.setSelectedIndex(idx == -1 ? 0 : idx);
    }

    private void reloadTabbedPane() {
        this.remove(this.tabbedPane);
        this.tabbedPane = new JTabbedPane();

        JPanel generalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel advancedPanel = new JPanel();
        advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));
        advancedPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        TrackerOptions options = TrackerOptions.getInstance();

        generalPanel.add(new TextOptionField("Sheet Name", options.sheet_name, (val) -> {
            options.sheet_name = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new TextOptionField("Sheet ID", options.sheet_id, (val) -> {
            options.sheet_id = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new MultiChoiceOptionField(I18n.getSupported().toArray(new String[0]), options.lang, "Game language", (val) -> {
            options.lang = val;
            TrackerOptions.save();
        }));

        generalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        generalPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        generalPanel.add(new BooleanOptionField("Generate Headers", "Generates headers for google sheets", options.gen_labels, (val) -> {
            options.gen_labels = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new BooleanOptionField("Upload to sheets", options.upload_sheets, (val) -> {
            options.upload_sheets = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new BooleanOptionField("Detect SSG", options.detect_ssg, (val) -> {
            options.detect_ssg = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new BooleanOptionField("Only Track Completions", options.only_track_completions, (val) -> {
            options.only_track_completions = val;
            TrackerOptions.save();
        }));

        generalPanel.add(new BooleanOptionField("Save Runs Locally", "Saves runs to ./local", options.always_save_locally, (val) -> {
            options.always_save_locally = val;
            TrackerOptions.save();
        }));

        generalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        generalPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        generalPanel.add(new ActionButton("Open Tracker Folder", (e) -> {
            try {
                Desktop.getDesktop().open(TrackerOptions.getTrackerDir().toFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open folder");
            }
        }));

        generalPanel.add(new ActionButton("Validate Settings", (e) -> TrackerOptions.validateSettings()));

        advancedPanel.add(new NumberOptionField("Save interval (s)", "The interval which game files are updated at", options.game_save_interval, (val) -> {
            options.game_save_interval = val;
            TrackerOptions.save();
        }));

        advancedPanel.add(new NumberOptionField("Max respawn to hunger reset (s)", "The maximum time between a respawn point being set and a death for it to count as a hunger reset", options.max_respawn_to_hr_time, (val) -> {
            options.max_respawn_to_hr_time = val;
            TrackerOptions.save();
        }));
        advancedPanel.add(new NumberOptionField("Path update interval (s)", "Interval between coordinate checks, used for the chunkmap", options.path_interval, (val) -> {
            options.path_interval = val;
            TrackerOptions.save();
        }));

        if (options.upload_remote_server) {
            advancedPanel.add(new JSeparator(JSeparator.HORIZONTAL));

            advancedPanel.add(new TextOptionField("API key", options.api_key, true, (val) -> {
                options.api_key = val;
                TrackerOptions.save();
                this.onApiKeyChange(val);
            }, (e) -> new Thread(() -> {
                if (!options.upload_remote_server) {
                    return;
                }
                boolean res = APIUtil.isValidKey(options.api_key);
                if (res) {
                    JOptionPane.showMessageDialog(this, "Valid Key", "Verification", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Tracker.log(Level.INFO, "Valid API Key");
                    JOptionPane.showMessageDialog(this, "Invalid key", "Verification", JOptionPane.ERROR_MESSAGE);
                }
            }, "ApiKeyVerification").start()
            ));

            advancedPanel.add(new TextOptionField("API URL", options.api_url, false, (val) -> {
                options.api_url = val;
                TrackerOptions.save();
            }, (e) -> new Thread(() -> {
                if (!options.upload_remote_server) {
                    return;
                }
                boolean res = APIUtil.isValidApiUrl(options.api_url);
                if (res) {
                    JOptionPane.showMessageDialog(this, "Valid URL", "Verification", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Tracker.log(Level.INFO, "Valid API Url");
                    JOptionPane.showMessageDialog(this, "Invalid URL", "Verification", JOptionPane.ERROR_MESSAGE);
                }
            }, "ApiUrlVerification").start()
            ));
        }

        advancedPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        advancedPanel.add(new JSeparator(JSeparator.HORIZONTAL));

        advancedPanel.add(new BooleanOptionField("Debug messages", options.show_debug, (val) -> {
            options.show_debug = val;
            TrackerOptions.save();
        }));

        advancedPanel.add(new BooleanOptionField("Generate World Map", options.generate_chunkmap, (val) -> {
            options.generate_chunkmap = val;
            TrackerOptions.save();
        }));

        advancedPanel.add(new BooleanOptionField("Experimental Tracking", options.use_experimental_tracking, (val) -> {
            options.use_experimental_tracking = val;
            TrackerOptions.save();
        }));

        advancedPanel.add(new BooleanOptionField("Upload to a remote server", options.upload_remote_server, (val) -> {
            boolean hasChange = val != options.upload_remote_server;
            options.upload_remote_server = val;
            TrackerOptions.save();

            if (hasChange) {
                SwingUtilities.invokeLater(this::reload);
            }
        }));

        this.tabbedPane.add("General", generalPanel);
        this.tabbedPane.add("Advanced", advancedPanel);

        this.onApiKeyChange(TrackerOptions.getInstance().api_key);

        this.add(this.tabbedPane);

        this.tabbedPane.revalidate();
        this.tabbedPane.repaint();
        this.revalidate();
        this.repaint();
    }

    public void appendLog(Object o) {
        SwingUtilities.invokeLater(() -> this.logArea.append(o.toString()));
    }

    public void open() {
        TrackerOptions options = TrackerOptions.getInstance();
        this.setLocation(options.last_win_x, options.last_win_y);
        this.setVisible(true);
    }

    public static synchronized TrackerFrame getInstance() {
        if (instance == null) {
            instance = new TrackerFrame();
        }
        return instance;
    }

    private void onApiKeyChange(String newKey) {
        if (!TrackerOptions.getInstance().upload_remote_server) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (APIUtil.isValidKey(newKey)) {
                this.tabbedPane.add("Runs", editorPanel);
            } else {
                int idx = this.tabbedPane.indexOfTab("Runs");
                if (idx != -1) {
                    this.tabbedPane.remove(idx);
                }
            }
        });
    }

    public void setView(Container container) {
        this.setContentPane(container);
        this.revalidate();
        this.repaint();
    }

    public void resetToInitialView() {
        this.setView(this.initialView);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        TrackerOptions options = TrackerOptions.getInstance();
        options.last_win_x = e.getWindow().getX();
        options.last_win_y = e.getWindow().getY();
        TrackerOptions.save();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
