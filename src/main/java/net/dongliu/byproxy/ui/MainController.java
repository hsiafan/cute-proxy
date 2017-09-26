package net.dongliu.byproxy.ui;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.ExitHooks;
import net.dongliu.byproxy.netty.Server;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.ssl.AppKeyStoreGenerator;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.struct.Message;
import net.dongliu.byproxy.struct.WebSocketMessage;
import net.dongliu.byproxy.ui.component.*;
import net.dongliu.byproxy.ui.task.InitContextTask;
import net.dongliu.byproxy.ui.task.LoadTask;
import net.dongliu.byproxy.ui.task.SaveSettingTask;
import net.dongliu.byproxy.ui.task.SaveTrafficDataTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateEncodingException;
import java.util.Collection;
import java.util.Optional;

/**
 * The main UI Controller
 *
 * @author Liu Dong
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    @FXML
    private MenuItem deleteMenu;
    @FXML
    private MenuItem copyURLButton;
    @FXML
    private MenuItem startProxyMenu;
    @FXML
    private MenuItem stopProxyMenu;
    @FXML
    private CatalogPane catalogPane;
    @FXML
    private VBox root;
    @FXML
    private MyButton startProxyButton;
    @FXML
    private MyButton stopProxyButton;

    @FXML
    private Label listenedAddressLabel;

    @FXML
    private HttpRoundTripMessagePane httpRoundTripMessagePane;
    @FXML
    private WebSocketMessagePane webSocketMessagePane;

    private volatile Server server;
    private Context context = Context.getInstance();

    @FXML
    private void startProxy() {
        startProxyButton.setDisable(true);
        startProxyMenu.setDisable(true);
        try {
            server = new Server(context.getServerSetting());
            server.setSslContextManager(context.getSslContextManager());
            server.setProxySetting(context.getProxySetting());
            server.setMessageListener(new MessageListener() {
                @Override
                public void onHttpRequest(HttpRoundTripMessage message) {
                    Platform.runLater(() -> catalogPane.addTreeItemMessage(message));
                }

                @Override
                public void onWebSocket(WebSocketMessage message) {
                    Platform.runLater(() -> catalogPane.addTreeItemMessage(message));
                }
            });
            server.start();
        } catch (Throwable t) {
            logger.error("Start proxy failed", t);
            UIUtils.showMessageDialog("Start proxy failed!");
            return;
        }
        Platform.runLater(() -> {
            stopProxyButton.setDisable(false);
            stopProxyMenu.setDisable(false);
            updateListenedAddress();
        });
    }

    @FXML
    private void stopProxy() {
        stopProxyButton.setDisable(true);
        stopProxyMenu.setDisable(true);
        new Thread(() -> {
            server.stop();
            server = null;
            Platform.runLater(() -> {
                startProxyButton.setDisable(false);
                startProxyMenu.setDisable(false);
                listenedAddressLabel.setText("");
            });
        }).start();
    }

    @FXML
    private void initialize() {
        ExitHooks.registerTask(() -> {
            if (server != null) {
                server.stop();
                server = null;
            }
        });

        Property<Message> selectedMessage = catalogPane.selectedMessageProperty();
        selectedMessage.addListener((ov, old, message) -> {
            if (message == null) {
                hideContent();
            } else {
                showMessage(message);
            }
        });

        ObservableValue<Boolean> messageSelected = UIUtils.observeNull(selectedMessage);
        copyURLButton.disableProperty().bind(messageSelected);
//        replayMenu.disableProperty().bind(messageSelected);

        Property<TreeItem<ItemValue>> selectedTreeItem = catalogPane.selectedTreeItemProperty();
        deleteMenu.disableProperty().bind(UIUtils.observeNull(selectedTreeItem));
        loadConfigAndKeyStore();

    }

    /**
     * Load app mainSetting, and keyStore contains private key/certs
     */
    private void loadConfigAndKeyStore() {
        InitContextTask task = new InitContextTask(context);
        UIUtils.runBackground(task, "Init mainSetting failed");
    }

    /**
     * Handle setting menu
     */
    @FXML
    private void updateSetting(ActionEvent e) throws IOException {
        MainSettingDialog dialog = new MainSettingDialog();
        dialog.mainSettingProperty().setValue(context.getServerSetting());
        Optional<ServerSetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, newConfig.get(), context.getKeyStoreSetting(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "append settings failed");
        }
    }

    @FXML
    private void setKeyStore(ActionEvent e) throws IOException {
        KeyStoreSettingDialog dialog = new KeyStoreSettingDialog();
        dialog.keyStoreSettingProperty().setValue(context.getKeyStoreSetting());
        Optional<KeyStoreSetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, context.getServerSetting(), newConfig.get(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "append key store failed");
        }
    }

    @FXML
    private void setProxy(ActionEvent e) throws IOException {
        ProxySettingDialog dialog = new ProxySettingDialog();
        dialog.proxySettingProperty().setValue(context.getProxySetting());
        Optional<ProxySetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, context.getServerSetting(), context.getKeyStoreSetting(),
                    newConfig.get());
            UIUtils.runBackground(task, "append secondary proxy setting failed");
        }
    }

    /**
     * Get listened addresses, show in toolbar
     */
    private void updateListenedAddress() {
        ServerSetting config = context.getServerSetting();
        String host = config.getHost().trim();
        int port = config.getPort();
        Platform.runLater(() -> listenedAddressLabel.setText("Listened " + host + ":" + port));
    }

    /**
     * Show message content in right area
     */
    private void showMessage(Message message) {
        if (message instanceof HttpRoundTripMessage) {
            httpRoundTripMessagePane.setRoundTripMessage((HttpRoundTripMessage) message);
            webSocketMessagePane.setVisible(false);
            httpRoundTripMessagePane.setVisible(true);
//            httpRoundTripMessagePane.setStyle("-fx-background-color:white;");
        } else if (message instanceof WebSocketMessage) {
            webSocketMessagePane.setMessage((WebSocketMessage) message);
            httpRoundTripMessagePane.setVisible(false);
            webSocketMessagePane.setVisible(true);
        }
    }

    /**
     * hide right area
     */
    private void hideContent() {
        httpRoundTripMessagePane.setVisible(false);
        webSocketMessagePane.setVisible(false);
    }

    @FXML
    private void clearAll(ActionEvent e) {
        catalogPane.clearAll();
    }

    @FXML
    private void open(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ByProxy data", "*.bpd"));
        File file = fileChooser.showOpenDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }

        catalogPane.clearAll();
        LoadTask task = new LoadTask(file.getPath(), catalogPane::addTreeItemMessage);
        UIUtils.runBackground(task, "Load data failed!");
    }

    // save captured data to file
    @FXML
    private void save(ActionEvent e) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ByProxy data", "*.bpd"));
        fileChooser.setInitialFileName("ByProxy.bpd");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Collection<Message> messages = catalogPane.getMessages();
        SaveTrafficDataTask saveTask = new SaveTrafficDataTask(file.getPath(), messages);
        UIUtils.runBackground(saveTask, "Save data failed!");
    }


    @FXML
    private void exportPem(ActionEvent e) throws CertificateEncodingException, IOException {
        AppKeyStoreGenerator generator = Context.getInstance().getSslContextManager().getAppKeyStoreGenerator();
        byte[] data = generator.exportCACertificate(true);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pem file", "*.pem"));
        fileChooser.setInitialFileName("ByProxy.pem");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    private void exportCrt(ActionEvent e) throws CertificateEncodingException, IOException {
        AppKeyStoreGenerator generator = Context.getInstance().getSslContextManager().getAppKeyStoreGenerator();
        byte[] data = generator.exportCACertificate(false);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Crt file", "*.crt"));
        fileChooser.setInitialFileName("ByProxy.crt");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    private void copyUrl(ActionEvent event) {
        String url = catalogPane.selectedMessageProperty().getValue().getUrl();
        UIUtils.copyToClipBoard(url);
    }

    @FXML
    private void deleteTreeNode(ActionEvent event) {
        TreeItem<ItemValue> treeItem = catalogPane.selectedTreeItemProperty().getValue();
        if (treeItem != null) {
            catalogPane.deleteTreeNode(treeItem);
        }
    }
}
