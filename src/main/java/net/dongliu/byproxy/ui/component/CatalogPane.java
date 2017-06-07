package net.dongliu.byproxy.ui.component;

import net.dongliu.byproxy.parser.Message;
import net.dongliu.byproxy.ui.RTreeItemValue;
import net.dongliu.byproxy.utils.NetUtils;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import net.dongliu.commons.exception.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Liu Dong
 */
public class CatalogPane extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(CatalogPane.class);
    @FXML
    private StackPane stackPane;
    @FXML
    private ListView<Message> messageList;
    @FXML
    private TreeView<RTreeItemValue> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    private Consumer<Message> listener;

    public CatalogPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw Throwables.throwAny(e);
        }
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
        messageList.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> listener.accept(n));

        TreeItem<RTreeItemValue> root = new TreeItem<>(new RTreeItemValue.Node(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        messageTree.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItemValue.Node) {
                listener.accept(null);
            } else {
                RTreeItemValue.Leaf value = (RTreeItemValue.Leaf) n.getValue();
                listener.accept(value.getMessage());
            }
        });


        viewTypeGroup.selectedToggleProperty().addListener((ov, o, n) -> {
            String type = (String) n.getUserData();
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

    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
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
                copyMenu.setOnAction(event1 -> {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(leaf.getMessage().getUrl());
                    clipboard.setContent(content);
                });
                contextMenu.getItems().add(copyMenu);
            }

            MenuItem deleteMenu = new MenuItem("Delete");
            deleteMenu.setOnAction(event1 -> {
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

            });
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(CatalogPane.this, event.getScreenX(), event.getScreenY());
        }
    }
}
