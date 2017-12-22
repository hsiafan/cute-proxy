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
import net.dongliu.byproxy.data.Message;
import net.dongliu.byproxy.ui.UIUtils;
import net.dongliu.byproxy.utils.Networks;

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
    private TreeView<Item> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    private Property<Message> currentMessage = new SimpleObjectProperty<>();
    private Property<TreeItem<Item>> currentTreeItem = new SimpleObjectProperty<>();

    public CatalogPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        messageList.setCellFactory(listView -> new ListCell<>() {
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
        messageList.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> currentMessage.setValue(n));

        TreeItem<Item> root = new TreeItem<>(new TreeNode(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        ReadOnlyObjectProperty<TreeItem<Item>> selectTreeNode = messageTree.getSelectionModel().selectedItemProperty();
        selectTreeNode.addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof TreeNode) {
                currentMessage.setValue(null);
            } else {
                Message message = (Message) n.getValue();
                currentMessage.setValue(message);
            }
        });

        ReadOnlyObjectProperty<Toggle> toggleProperty = viewTypeGroup.selectedToggleProperty();
        StringBinding typeProperty = createStringBinding(() -> (String) toggleProperty.get().getUserData(), toggleProperty);

        currentTreeItem.bind(Bindings.createObjectBinding(() -> {
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
        messageTree.setRoot(new TreeItem<>(new TreeNode("")));
    }

    public void addTreeItemMessage(Message message) {
        messageList.getItems().add(message);
        TreeItem<Item> root = messageTree.getRoot();
        String host = Networks.genericMultiCDNS(message.getHost());
        insertNewMessage(message, root, host);
    }

    private void insertNewMessage(Message message, TreeItem<Item> root, String host) {
        TreeItem<Item> cItem = null;
        int cResult = TreeNode.MISS;
        int nodeEndPos = 0;
        for (TreeItem<Item> item : root.getChildren()) {
            Item value = item.getValue();
            if (!(value instanceof TreeNode)) {
                break;
            }
            nodeEndPos++;
            TreeNode node = (TreeNode) value;
            int result = node.match(host);
            if (result > cResult) {
                cItem = item;
                cResult = result;
            }
        }

        if (cResult == TreeNode.MISS) {
            // no one matches, create new top level node
            TreeNode node = new TreeNode(host);
            TreeItem<Item> nodeItem = new TreeItem<>(node);
            root.getChildren().add(nodeEndPos, nodeItem);
            nodeItem.getChildren().add(new TreeItem<>(message));
            node.increaseChildren();
            return;
        }

        if (cResult == TreeNode.EQUAL) {
            // exactly match
            cItem.getChildren().add(new TreeItem<>(message));
            return;
        }

        if (cResult == TreeNode.IS_SUB) {
            // host is sub domain of node pattern
            insertNewMessage(message, cItem, host);
            return;
        }

        if (cResult == TreeNode.IS_SUPER) {
            // node pattern is sub domain of host
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newItem = new TreeItem<>(newNode);
            root.getChildren().add(nodeEndPos, newItem);
            root.getChildren().remove(cItem);
            newItem.getChildren().add(cItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newItem.getChildren().add(leaf);
            return;
        }

        // share common domain suffix
        String rootPath = ((TreeNode) (root.getValue())).getPattern();
        String commonPattern = host.substring(host.length() - cResult, host.length());
        if (commonPattern.equals(rootPath)) {
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newTreeItem = new TreeItem<>(newNode);
            root.getChildren().add(nodeEndPos, newTreeItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newTreeItem.getChildren().add(leaf);
        } else {
            TreeNode parentNode = new TreeNode(commonPattern);
            TreeItem<Item> parentItem = new TreeItem<>(parentNode);
            root.getChildren().add(nodeEndPos, parentItem);
            root.getChildren().remove(cItem);
            parentItem.getChildren().add(cItem);
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newItem = new TreeItem<>(newNode);
            parentItem.getChildren().add(newItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newItem.getChildren().add(leaf);
        }
    }

    public Collection<Message> getMessages() {
        ObservableList<Message> items = messageList.getItems();
        return new ArrayList<>(items);
    }

    private static class TreeCellFactory implements Callback<TreeView<Item>, TreeCell<Item>> {

        @Override
        public TreeCell<Item> call(TreeView<Item> treeView) {
            return new TreeCell<>() {
                @Override
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setText(null);
                    } else {
                        setText(item.displayText());
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

            TreeItem<Item> treeItem = messageTree.getSelectionModel().getSelectedItem();
            if (treeItem == null) {
                return;
            }
            Item item = treeItem.getValue();

            ContextMenu contextMenu = new ContextMenu();

            if (item instanceof Message) {
                Message message = (Message) item;
                MenuItem copyMenu = new MenuItem("Copy URL");
                copyMenu.setOnAction(e -> UIUtils.copyToClipBoard(message.getUrl()));
                contextMenu.getItems().add(copyMenu);
            }

            MenuItem deleteMenu = new MenuItem("Delete");
            deleteMenu.setOnAction(e -> deleteTreeItem(treeItem));
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(CatalogPane.this, event.getScreenX(), event.getScreenY());
        }
    }

    public void deleteTreeItem(TreeItem<Item> treeItem) {
        TreeItem<Item> parent = treeItem.getParent();
        parent.getChildren().remove(treeItem);

        // also remove from list view
        List<Message> removed = new ArrayList<>();
        findAllLeafs(treeItem, removed);
        messageList.getItems().removeAll(removed);
    }

    private void findAllLeafs(TreeItem<Item> item, List<Message> messageList) {
        Item value = item.getValue();
        if (value instanceof Message) {
            messageList.add((Message) value);
            return;
        }
        for (TreeItem<Item> ni : item.getChildren()) {
            findAllLeafs(ni, messageList);
        }
    }

    public Property<Message> currentMessageProperty() {
        return currentMessage;
    }

    public Property<TreeItem<Item>> currentTreeItemProperty() {
        return currentTreeItem;
    }
}
