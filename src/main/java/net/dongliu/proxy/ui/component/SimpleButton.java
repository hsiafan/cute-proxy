package net.dongliu.proxy.ui.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

/**
 * Custom simple button
 *
 * @author Liu Dong
 */
public class SimpleButton extends Button {

    private StringProperty tooltipText = new SimpleStringProperty();
    private StringProperty iconPath = new SimpleStringProperty();


    public SimpleButton() {
        super();
        init();
    }

    private void init() {
        Bindings.bindBidirectional(tooltipText, tooltipProperty(), new StringConverter<Tooltip>() {
            @Override
            public String toString(Tooltip tooltip) {
                if (tooltip == null) {
                    return null;
                }
                return tooltip.getText();
            }

            @Override
            public Tooltip fromString(String string) {
                if (string == null) {
                    return null;
                }
                return new Tooltip(string);
            }
        });
        iconPath.addListener((observable, oldValue, newValue) ->
                graphicProperty().set(new ImageView(new Image(getClass().getResourceAsStream(newValue)))));
    }

    public String getTooltipText() {
        return tooltipText.get();
    }

    public StringProperty tooltipTextProperty() {
        return tooltipText;
    }

    public void setTooltipText(String tooltipText) {
        this.tooltipText.set(tooltipText);
    }

    public String getIconPath() {
        return iconPath.get();
    }

    public StringProperty iconPathProperty() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath.set(iconPath);
    }
}
