package me.cylorun.gui.components;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class BooleanOptionField extends JCheckBox {
    public BooleanOptionField(String label, boolean value, Consumer<Boolean> consumer) {
        super(label);
        this.setSelected(value);
        this.addChangeListener((e)-> consumer.accept(this.isSelected()));
    }

    public BooleanOptionField(String label, String tooltipText, boolean value, Consumer<Boolean> consumer) {
        super(label);
        this.setSelected(value);
        this.setToolTipText(tooltipText);
        this.addChangeListener((e)-> consumer.accept(this.isSelected()));
    }
}
