package net.dongliu.proxy.ui.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import net.dongliu.proxy.data.Header;
import net.dongliu.proxy.data.HttpMessage;
import net.dongliu.proxy.data.Message;
import net.dongliu.proxy.data.WebSocketMessage;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.ui.UIUtils;
import net.dongliu.proxy.utils.Networks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static javafx.beans.binding.Bindings.createStringBinding;
import static net.dongliu.proxy.ui.RequestCopyUtils.copyRequestAsCurl;

/**
 * Show catalogs
 *
 * @author Liu Dong
 */
public class CatalogPane extends BorderPane {
    @FXML
    private ToggleButton toggleSearch;
    @FXML
    private VBox settingArea;
    @FXML
    private VBox searchArea;
    @FXML
    private CheckBox searchInURL;
    @FXML
    private CheckBox searchInHeaders;
    @FXML
    private CheckBox searchInBody;

    @FXML
    private TextField filterText;
    @FXML
    private StackPane stackPane;
    @FXML
    private ListView<Message> messageList;
    @FXML
    private TreeView<Item> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    // only message accepted by filter will be shown
    private Predicate<Message> filter;
    // list to save all messages. if filter is changed, will apply filter to this list to get all shown message.
    private List<Message> allMessages;

    private Property<Message> currentMessage = new SimpleObjectProperty<>();
    private Property<TreeItem<Item>> currentTreeItem = new SimpleObjectProperty<>();

    public CatalogPane() throws IOException {
        var fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        // for filter
        settingArea.getChildren().remove(searchArea);
        filter = it -> true;
        allMessages = new ArrayList<>();

        // set list view
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
        messageList.getSelectionModel().selectedItemProperty()
                .addListener((ov, o, n) -> currentMessage.setValue(n));

        // set tree view
        var root = new TreeItem<Item>(new TreeNode(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        var selectTreeNode = messageTree.getSelectionModel().selectedItemProperty();
        selectTreeNode.addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof TreeNode) {
                currentMessage.setValue(null);
            } else {
                Message message = (Message) n.getValue();
                currentMessage.setValue(message);
            }
        });

        // for switching between tree view and list view
        var toggleProperty = viewTypeGroup.selectedToggleProperty();
        var typeProperty = createStringBinding(() -> (String) toggleProperty.get().getUserData(), toggleProperty);

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


    /**
     * clear all messages.
     */
    public void clearAll() {
        allMessages.clear();
        messageList.getItems().clear();
        messageTree.setRoot(new TreeItem<>(new TreeNode("")));
    }

    /**
     * send a new message to catalog pane
     */
    public void addTreeItemMessage(Message message) {
        allMessages.add(message);
        if (!filter.test(message)) {
            return;
        }
        messageList.getItems().add(message);
        insertNewMessageToTree(message);
    }

    @FXML
    private void changeFilter(ActionEvent e) {
        var keyword = filterText.getText().trim();
        filter = m -> {
            if (searchInURL.isSelected()) {
                if (m.url().contains(keyword)) {
                    return true;
                }
            }
            if (searchInHeaders.isSelected()) {
                if (m instanceof HttpMessage) {
                    for (var httpHeaders : List.of(((HttpMessage) m).requestHeader(), ((HttpMessage) m).responseHeader())) {
                        for (Header header : httpHeaders.headers()) {
                            if (header.value().contains(keyword)) {
                                return true;
                            }
                        }
                    }
                }
            }
            if (searchInBody.isSelected()) {
                List<Body> bodies;
                if (m instanceof WebSocketMessage) {
                    bodies = List.of(((WebSocketMessage) m).body());
                } else if (m instanceof HttpMessage) {
                    bodies = List.of(((HttpMessage) m).requestBody(), ((HttpMessage) m).responseBody());
                } else {
                    throw new RuntimeException();
                }
                for (Body body : bodies) {
                    if (body.finished() && body.size() > 0 && body.type().isText()) {
                        try {
                            if (body.getAsString().contains(keyword)) {
                                return true;
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            return false;
        };
        onChangeFilter();
    }

    @FXML
    private void clearFilter() {
        filterText.setText("");
        filter = m -> true;
        onChangeFilter();
    }

    @FXML
    private void toggleSearch(ActionEvent e) {
        if (toggleSearch.isSelected()) {
            searchArea.setVisible(true);
            settingArea.getChildren().add(searchArea);
        } else {
            searchArea.setVisible(false);
            settingArea.getChildren().remove(searchArea);
        }
    }

    /**
     * Called when filter has been changed, to refresh catalog pane.
     */
    private void onChangeFilter() {
        messageList.getItems().clear();
        messageTree.setRoot(new TreeItem<>(new TreeNode("")));
        for (var message : allMessages) {
            if (filter.test(message)) {
                messageList.getItems().addAll(message);
                insertNewMessageToTree(message);
            }
        }
    }

    private void insertNewMessageToTree(Message message) {
        var root = messageTree.getRoot();
        String host = Networks.genericMultiCDNS(message.host());
        insertNewMessageAtNode(message, root, host);
    }

    private void insertNewMessageAtNode(Message message, TreeItem<Item> parent, String host) {
        TreeItem<Item> cItem = null;
        int cResult = TreeNode.MISS;
        int nodeEndPos = 0;
        for (var item : parent.getChildren()) {
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
            parent.getChildren().add(nodeEndPos, nodeItem);
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
            insertNewMessageAtNode(message, cItem, host);
            return;
        }

        if (cResult == TreeNode.IS_SUPER) {
            // node pattern is sub domain of host
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newItem = new TreeItem<>(newNode);
            parent.getChildren().add(nodeEndPos, newItem);
            parent.getChildren().remove(cItem);
            newItem.getChildren().add(cItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newItem.getChildren().add(leaf);
            return;
        }

        // share common domain suffix
        String rootPath = ((TreeNode) (parent.getValue())).getPattern();
        String commonPattern = host.substring(host.length() - cResult, host.length());
        if (commonPattern.equals(rootPath)) {
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newTreeItem = new TreeItem<>(newNode);
            parent.getChildren().add(nodeEndPos, newTreeItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newTreeItem.getChildren().add(leaf);
        } else {
            TreeNode parentNode = new TreeNode(commonPattern);
            TreeItem<Item> parentItem = new TreeItem<>(parentNode);
            parent.getChildren().add(nodeEndPos, parentItem);
            parent.getChildren().remove(cItem);
            parentItem.getChildren().add(cItem);
            TreeNode newNode = new TreeNode(host);
            TreeItem<Item> newItem = new TreeItem<>(newNode);
            parentItem.getChildren().add(newItem);
            TreeItem<Item> leaf = new TreeItem<>(message);
            newItem.getChildren().add(leaf);
        }
    }

    public Collection<Message> getMessages() {
        var items = messageList.getItems();
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

            var treeItem = messageTree.getSelectionModel().getSelectedItem();
            if (treeItem == null) {
                return;
            }
            Item item = treeItem.getValue();

            ContextMenu contextMenu = new ContextMenu();

            if (item instanceof Message) {
                Message message = (Message) item;
                MenuItem copyMenu = new MenuItem("Copy URL");
                copyMenu.setOnAction(e -> UIUtils.copyToClipBoard(message.url()));
                contextMenu.getItems().add(copyMenu);

                if (item instanceof HttpMessage) {
                    HttpMessage httpMessage = (HttpMessage) item;
                    MenuItem copyAsCurlMenu = new MenuItem("Copy as curl");
                    copyAsCurlMenu.setOnAction(e -> copyRequestAsCurl(httpMessage));
                    contextMenu.getItems().addAll(copyAsCurlMenu);
                }
            }

            MenuItem deleteMenu = new MenuItem("Delete");
            deleteMenu.setOnAction(e -> deleteTreeItem(treeItem));
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(CatalogPane.this, event.getScreenX(), event.getScreenY());
        }
    }

    public void deleteTreeItem(TreeItem<Item> treeItem) {
        var parent = treeItem.getParent();
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
        for (var ni : item.getChildren()) {
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
