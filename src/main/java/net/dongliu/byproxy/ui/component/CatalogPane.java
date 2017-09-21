package net.dongliu.byproxy.ui.component;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import net.dongliu.byproxy.struct.Message;
import net.dongliu.byproxy.ui.ItemValue;
import net.dongliu.byproxy.ui.TreeNodeValue;
import net.dongliu.byproxy.ui.UIUtils;
import net.dongliu.byproxy.utils.NetUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javafx.beans.binding.Bindings.createStringBinding;

/**
 * @author Liu Dong
 */
public class CatalogPane extends BorderPane {
    @FXML
    private StackPane stackPane;
    @FXML
    private ListView<Message> messageList;
    @FXML
    private TreeView<ItemValue> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    private Property<Message> selectedMessage = new SimpleObjectProperty<>();
    private Property<TreeItem<ItemValue>> selectedTreeItem = new SimpleObjectProperty<>();

    public CatalogPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        messageList.setCellFactory(listView -> new ListCell<Message>() {
            @Override
            public void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.displayText());
                }
            }
        });
        messageList.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> selectedMessage.setValue(n));

        TreeItem<ItemValue> root = new TreeItem<>(new TreeNodeValue(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        ReadOnlyObjectProperty<TreeItem<ItemValue>> selectTreeNode = messageTree.getSelectionModel().selectedItemProperty();
        selectTreeNode.addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof TreeNodeValue) {
                selectedMessage.setValue(null);
            } else {
                Message message = (Message) n.getValue();
                selectedMessage.setValue(message);
            }
        });

        ReadOnlyObjectProperty<Toggle> toggleProperty = viewTypeGroup.selectedToggleProperty();
        StringBinding typeProperty = createStringBinding(() -> (String) toggleProperty.get().getUserData(), toggleProperty);

        selectedTreeItem.bind(Bindings.createObjectBinding(() -> {
            if (!"tree".equals(typeProperty.get())) {
                return null;
            }
            return selectTreeNode.get();
        }, typeProperty, selectTreeNode));

        typeProperty.addListener((ov, o, type) -> {
            if (type.equals("list")) {
                stackPane.getChildren().remove(messageList);
                stackPane.getChildren().add(messageList);
            } else if (type.equals("tree")) {
                stackPane.getChildren().remove(messageTree);
                stackPane.getChildren().add(messageTree);
            }
        });
    }


    public void clearAll() {
        messageList.getItems().clear();
        messageTree.setRoot(new TreeItem<>(new TreeNodeValue("")));
    }

    public void addTreeItemMessage(Message message) {
        messageList.getItems().add(message);
        TreeItem<ItemValue> root = messageTree.getRoot();
        String host = NetUtils.genericMultiCDNS(message.getHost());


        for (TreeItem<ItemValue> item : root.getChildren()) {
            TreeNodeValue node = (TreeNodeValue) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(message));
                node.increaseChildren();
                return;
            }
        }

        TreeNodeValue node = new TreeNodeValue(host);
        TreeItem<ItemValue> nodeItem = new TreeItem<>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(message));
        node.increaseChildren();
    }

    public Collection<Message> getMessages() {
        ObservableList<Message> items = messageList.getItems();
        return new ArrayList<>(items);
    }

    private static class TreeCellFactory implements Callback<TreeView<ItemValue>, TreeCell<ItemValue>> {

        @Override
        public TreeCell<ItemValue> call(TreeView<ItemValue> treeView) {
            return new TreeCell<ItemValue>() {
                @Override
                protected void updateItem(ItemValue itemValue, boolean empty) {
                    super.updateItem(itemValue, empty);

                    if (empty) {
                        setText(null);
                    } else {
                        setText(itemValue.displayText());
                    }
                }
            };
        }
    }


    private class TreeViewMouseHandler implements EventHandler<MouseEvent> {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(MouseEvent event) {
            if (!event.getButton().equals(MouseButton.SECONDARY)) {
                return;
            }

            TreeItem<ItemValue> treeItem = messageTree.getSelectionModel().getSelectedItem();
            if (treeItem == null) {
                return;
            }
            ItemValue itemValue = treeItem.getValue();

            ContextMenu contextMenu = new ContextMenu();

            if (itemValue instanceof Message) {
                Message message = (Message) itemValue;
                MenuItem copyMenu = new MenuItem("Copy URL");
                copyMenu.setOnAction(e -> UIUtils.copyToClipBoard(message.getUrl()));
                contextMenu.getItems().add(copyMenu);
            }

            MenuItem deleteMenu = new MenuItem("Delete");
            deleteMenu.setOnAction(e -> deleteTreeNode(treeItem));
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(CatalogPane.this, event.getScreenX(), event.getScreenY());
        }
    }

    public void deleteTreeNode(TreeItem<ItemValue> treeItem) {
        TreeItem<ItemValue> parent = treeItem.getParent();
        parent.getChildren().remove(treeItem);
        // also remove from list view
        ItemValue value = treeItem.getValue();
        List<Message> removed = new ArrayList<>();
        if (value instanceof Message) {
            Message message = (Message) value;
            removed.add(message);
        } else {
            for (TreeItem<ItemValue> child : treeItem.getChildren()) {
                Message message = (Message) child.getValue();
                removed.add(message);
            }
        }
        messageList.getItems().removeAll(removed);
    }

    public Property<Message> selectedMessageProperty() {
        return selectedMessage;
    }

    public Property<TreeItem<ItemValue>> selectedTreeItemProperty() {
        return selectedTreeItem;
    }
}
