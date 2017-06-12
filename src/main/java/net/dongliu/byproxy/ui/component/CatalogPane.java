package net.dongliu.byproxy.ui.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
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
import lombok.Getter;
import lombok.val;
import net.dongliu.byproxy.parser.Message;
import net.dongliu.byproxy.ui.RTreeItemValue;
import net.dongliu.byproxy.ui.UIUtils;
import net.dongliu.byproxy.utils.NetUtils;
import net.dongliu.commons.functional.UnChecked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Liu Dong
 */
public class CatalogPane extends BorderPane {
    @FXML
    private StackPane stackPane;
    @FXML
    private ListView<Message> messageList;
    @FXML
    private TreeView<RTreeItemValue> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    @Getter
    private Property<Message> selectedMessage = new SimpleObjectProperty<>();
    @Getter
    private Property<TreeItem<RTreeItemValue>> selectedTreeItem = new SimpleObjectProperty<>();

    public CatalogPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        UnChecked.run(fxmlLoader::load);
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
                    setText(item.getDisplay());
                }
            }
        });
        messageList.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> selectedMessage.setValue(n));

        val root = new TreeItem<RTreeItemValue>(new RTreeItemValue.Node(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        val selectTreeNode = messageTree.getSelectionModel().selectedItemProperty();
        selectTreeNode.addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItemValue.Node) {
                selectedMessage.setValue(null);
            } else {
                RTreeItemValue.Leaf value = (RTreeItemValue.Leaf) n.getValue();
                selectedMessage.setValue(value.getMessage());
            }
        });

        val toggleProperty = viewTypeGroup.selectedToggleProperty();
        val typeProperty = Bindings.createStringBinding(() -> (String) toggleProperty.get().getUserData(), toggleProperty);

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
        messageTree.setRoot(new TreeItem<>(new RTreeItemValue.Node("")));
    }

    public void addTreeItemMessage(Message message) {
        messageList.getItems().add(message);
        TreeItem<RTreeItemValue> root = messageTree.getRoot();
        String host = NetUtils.genericMultiCDNS(message.getHost());


        for (TreeItem<RTreeItemValue> item : root.getChildren()) {
            RTreeItemValue.Node node = (RTreeItemValue.Node) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItemValue.Leaf(message)));
                node.increaseChildren();
                return;
            }
        }

        RTreeItemValue.Node node = new RTreeItemValue.Node(host);
        TreeItem<RTreeItemValue> nodeItem = new TreeItem<>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItemValue.Leaf(message)));
        node.increaseChildren();
    }

    public Collection<Message> getMessages() {
        ObservableList<Message> items = messageList.getItems();
        return new ArrayList<>(items);
    }

    private static class TreeCellFactory implements Callback<TreeView<RTreeItemValue>, TreeCell<RTreeItemValue>> {

        @Override
        public TreeCell<RTreeItemValue> call(TreeView<RTreeItemValue> treeView) {
            return new TreeCell<RTreeItemValue>() {
                @Override
                protected void updateItem(RTreeItemValue item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setText(null);
                    } else {
                        String text;
                        if (item instanceof RTreeItemValue.Node) {
                            text = ((RTreeItemValue.Node) item).getPattern() + "(" + ((RTreeItemValue.Node)
                                    item).getCount() + ")";
                        } else if (item instanceof RTreeItemValue.Leaf) {
                            Message message = ((RTreeItemValue.Leaf) item).getMessage();
                            text = message.getDisplay();
                        } else {
                            text = "BUG..";
                        }
                        setText(text);
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

            TreeItem<RTreeItemValue> treeItem = messageTree.getSelectionModel().getSelectedItem();
            if (treeItem == null) {
                return;
            }
            RTreeItemValue itemValue = treeItem.getValue();

            ContextMenu contextMenu = new ContextMenu();

            if (itemValue instanceof RTreeItemValue.Leaf) {
                RTreeItemValue.Leaf leaf = (RTreeItemValue.Leaf) itemValue;
                MenuItem copyMenu = new MenuItem("Copy URL");
                copyMenu.setOnAction(e -> UIUtils.copyToClipBoard(leaf.getMessage().getUrl()));
                contextMenu.getItems().add(copyMenu);
            }

            MenuItem deleteMenu = new MenuItem("Delete");
            deleteMenu.setOnAction(e -> {
                deleteTreeNode(treeItem);

            });
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(CatalogPane.this, event.getScreenX(), event.getScreenY());
        }
    }

    public void deleteTreeNode(TreeItem<RTreeItemValue> treeItem) {
        TreeItem<RTreeItemValue> parent = treeItem.getParent();
        parent.getChildren().remove(treeItem);
        // also remove from list view
        RTreeItemValue value = treeItem.getValue();
        List<Message> removed = new ArrayList<>();
        if (value instanceof RTreeItemValue.Leaf) {
            Message message = ((RTreeItemValue.Leaf) value).getMessage();
            removed.add(message);
        } else {
            for (TreeItem<RTreeItemValue> child : treeItem.getChildren()) {
                Message message = ((RTreeItemValue.Leaf) child.getValue()).getMessage();
                removed.add(message);
            }
        }
        messageList.getItems().removeAll(removed);
    }
}
