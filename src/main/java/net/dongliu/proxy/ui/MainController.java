package net.dongliu.proxy.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.dongliu.proxy.CloseHooks;
import net.dongliu.proxy.Context;
import net.dongliu.proxy.data.HttpMessage;
import net.dongliu.proxy.data.Message;
import net.dongliu.proxy.data.WebSocketMessage;
import net.dongliu.proxy.netty.Server;
import net.dongliu.proxy.setting.ServerSetting;
import net.dongliu.proxy.ui.component.*;
import net.dongliu.proxy.ui.task.InitContextTask;
import net.dongliu.proxy.ui.task.LoadTask;
import net.dongliu.proxy.ui.task.SaveSettingTask;
import net.dongliu.proxy.ui.task.SaveTrafficDataTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateEncodingException;
import java.util.Collection;

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
    private SimpleButton startProxyButton;
    @FXML
    private SimpleButton stopProxyButton;

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
            server = new Server(context.serverSetting(), context.sslContextManager(),
                    context.proxySetting(),
                    message -> Platform.runLater(() -> catalogPane.addTreeItemMessage(message)));
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
        CloseHooks.registerTask(() -> {
            if (server != null) {
                server.stop();
                server = null;
            }
        });

        var currentMessage = catalogPane.currentMessageProperty();
        currentMessage.addListener((ov, old, message) -> {
            if (message == null) {
                hideContent();
            } else {
                showMessage(message);
            }
        });

        var hasCurrentMessage = UIUtils.observeNull(currentMessage);
        copyURLButton.disableProperty().bind(hasCurrentMessage);

        var currentTreeItem = catalogPane.currentTreeItemProperty();
        deleteMenu.disableProperty().bind(UIUtils.observeNull(currentTreeItem));
        loadConfigAndKeyStore();

    }

    /**
     * Load app mainSetting, and keyStore contains private key/certs
     */
    private void loadConfigAndKeyStore() {
        var task = new InitContextTask(context);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Handle setting menu
     */
    @FXML
    private void updateSetting(ActionEvent e) throws IOException {
        var dialog = new MainSettingDialog();
        dialog.mainSettingProperty().setValue(context.serverSetting());
        var newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            var task = new SaveSettingTask(context, newConfig.get(), context.keyStoreSetting(),
                    context.proxySetting());
            UIUtils.runTaskWithProcessDialog(task, "append settings failed");
        }
    }

    @FXML
    private void setKeyStore(ActionEvent e) throws IOException {
        var dialog = new KeyStoreSettingDialog();
        dialog.keyStoreSettingProperty().setValue(context.keyStoreSetting());
        var newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            var task = new SaveSettingTask(context, context.serverSetting(), newConfig.get(),
                    context.proxySetting());
            UIUtils.runTaskWithProcessDialog(task, "append key store failed");
        }
    }

    @FXML
    private void setProxy(ActionEvent e) throws IOException {
        var dialog = new ProxySettingDialog();
        dialog.proxySettingProperty().setValue(context.proxySetting());
        var newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            var task = new SaveSettingTask(context, context.serverSetting(), context.keyStoreSetting(),
                    newConfig.get());
            UIUtils.runTaskWithProcessDialog(task, "append secondary proxy setting failed");
        }
    }

    /**
     * Get listened addresses, show in toolbar
     */
    private void updateListenedAddress() {
        ServerSetting config = context.serverSetting();
        String host = config.host().trim();
        int port = config.port();
        Platform.runLater(() -> listenedAddressLabel.setText("Listened " + host + ":" + port));
    }

    /**
     * Show message content in right area
     */
    private void showMessage(Message message) {
        if (message instanceof HttpMessage) {
            httpRoundTripMessagePane.setHttpMessage((HttpMessage) message);
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CuteProxy data", "*.mpd"));
        File file = fileChooser.showOpenDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }

        catalogPane.clearAll();
        LoadTask task = new LoadTask(file.getPath(), catalogPane::addTreeItemMessage);
        UIUtils.runTaskWithProcessDialog(task, "Load data failed!");
    }

    // save captured data to file
    @FXML
    private void save(ActionEvent e) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CuteProxy data", "*.mpd"));
        fileChooser.setInitialFileName("captured.mpd");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Collection<Message> messages = catalogPane.getMessages();
        SaveTrafficDataTask saveTask = new SaveTrafficDataTask(file.getPath(), messages);
        UIUtils.runTaskWithProcessDialog(saveTask, "Save data failed!");
    }


    @FXML
    private void exportPem(ActionEvent e) throws CertificateEncodingException, IOException {
        var generator = Context.getInstance().sslContextManager().getKeyStoreGenerator();
        byte[] data = generator.exportRootCert(true);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pem file", "*.pem"));
        fileChooser.setInitialFileName("CuteProxy.pem");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    private void exportCrt(ActionEvent e) throws CertificateEncodingException, IOException {
        var generator = Context.getInstance().sslContextManager().getKeyStoreGenerator();
        byte[] data = generator.exportRootCert(false);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Crt file", "*.crt"));
        fileChooser.setInitialFileName("CuteProxy.crt");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    private void copyUrl(ActionEvent event) {
        String url = catalogPane.currentMessageProperty().getValue().url();
        UIUtils.copyToClipBoard(url);
    }

    @FXML
    private void deleteTreeNode(ActionEvent event) {
        var treeItem = catalogPane.currentTreeItemProperty().getValue();
        if (treeItem != null) {
            catalogPane.deleteTreeItem(treeItem);
        }
    }
}
