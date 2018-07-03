package net.dongliu.proxy.ui.component;

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * Dialog for simplify fxml and code
 *
 * @author Liu Dong
 */
@DefaultProperty("content")
public class MyDialog<T> extends Dialog<T> {

    private final ObjectProperty<Node> content;

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    public void setContent(Node content) {
        this.content.set(content);
    }

    public Node getContent() {
        return content.get();
    }

    public MyDialog() {
        content = getDialogPane().contentProperty();
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    }
}
