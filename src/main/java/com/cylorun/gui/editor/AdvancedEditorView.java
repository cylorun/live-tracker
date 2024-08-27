package com.cylorun.gui.editor;

import com.cylorun.gui.TrackerFrame;
import com.cylorun.gui.components.ColorPicker;
import com.cylorun.gui.components.MultiChoiceOptionField;
import com.cylorun.gui.components.TextEditor;
import com.cylorun.gui.components.TextOptionField;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.JSONUtil;
import com.cylorun.utils.ResourceUtil;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;

public class AdvancedEditorView extends JPanel {

    private final MultiChoiceOptionField columnField;
    private TextOptionField valueField;
    private TextEditor textEditorField;
    private ColorPicker colorChooser;
    private JsonObject runData;
    private Consumer<Boolean> onChange;
    private Color prevColor = Color.WHITE;
    public AdvancedEditorView(JsonObject runData, Consumer<Boolean> onChange) {
        TrackerOptions options = TrackerOptions.getInstance();
        this.onChange = onChange;
        this.runData = runData;
        this.setLayout(new BorderLayout());

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

        this.valueField = new TextOptionField("Value", this.runData.get("run_id").getAsString(), (val) -> {
            if (this.runData == null) {
                return;
            }
            this.checkForChanges();
        });

        this.add(this.columnField);
        this.add(this.valueField);
        this.add(this.textEditorField);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        this.add(new JLabel("Run Color"));
        this.add(this.colorChooser);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        this.add(Box.createVerticalStrut(10));


    }

    private void checkForChanges() {
        boolean hasColorChanged = !this.prevColor.equals(this.colorChooser.getCurrentColor());
        boolean hasValueChanged = !this.valueField.getValue().equals(JSONUtil.getOptionalString(this.runData,this.columnField.getValue()).orElse(""));
        this.onChange.accept(hasColorChanged || hasValueChanged); // returns if the save button should be enabled
    }
}
