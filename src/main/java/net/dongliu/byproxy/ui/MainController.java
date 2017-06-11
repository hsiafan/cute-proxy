package net.dongliu.byproxy.ui;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.ShutdownHooks;
import net.dongliu.byproxy.parser.HttpMessage;
import net.dongliu.byproxy.parser.Message;
import net.dongliu.byproxy.parser.WebSocketMessage;
import net.dongliu.byproxy.proxy.AppKeyStoreGenerator;
import net.dongliu.byproxy.proxy.ProxyServer;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.MainSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.ui.component.*;
import net.dongliu.byproxy.ui.task.InitContextTask;
import net.dongliu.byproxy.ui.task.LoadTask;
import net.dongliu.byproxy.ui.task.SaveSettingTask;
import net.dongliu.byproxy.ui.task.SaveTrafficDataTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateEncodingException;
import java.util.Optional;

/**
 * @author Liu Dong
 */
@Slf4j
public class MainController {

    public MenuItem deleteMenu;
    public MenuItem copyURLButton;
    public MenuItem startProxyMenu;
    public MenuItem stopProxyMenu;
    public CatalogPane catalogPane;
    public SplitMenuButton setKeyStoreButton;
    public MyButton saveFileButton;
    public MyButton openFileButton;
    public VBox root;
    public SplitPane splitPane;
    public SplitMenuButton proxyConfigureButton;
    public MyButton startProxyButton;
    public MyButton stopProxyButton;

    public Label listenedAddressLabel;

    public HttpMessagePane httpMessagePane;
    public WebSocketMessagePane webSocketMessagePane;

    private volatile ProxyServer proxyServer;
    private Context context = Context.getInstance();

    @FXML
    private void startProxy() {
        startProxyButton.setDisable(true);
        startProxyMenu.setDisable(true);
        try {
            proxyServer = new ProxyServer(context.getMainSetting(), context.getSslContextManager());
            proxyServer.setMessageListener(new UIMessageListener(catalogPane::addTreeItemMessage));
            proxyServer.start();
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
            proxyServer.stop();
            Platform.runLater(() -> {
                startProxyButton.setDisable(false);
                startProxyMenu.setDisable(false);
                listenedAddressLabel.setText("");
            });
        }).start();
    }

    @FXML
    private void initialize() {
        ShutdownHooks.registerTask(() -> {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        });

        Property<Message> selectedMessage = catalogPane.getSelectedMessage();

        selectedMessage.addListener((ov, old, message) -> {
            if (message == null) {
                hideContent();
            } else {
                showMessage(message);
            }
        });

        copyURLButton.disableProperty().bind(UIUtils.observeNull(selectedMessage));

        val selectedTreeItem = catalogPane.getSelectedTreeItem();
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
    void updateSetting(ActionEvent e) {
        MainSettingDialog dialog = new MainSettingDialog();
        dialog.mainSettingProperty().setValue(context.getMainSetting());
        Optional<MainSetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, newConfig.get(), context.getKeyStoreSetting(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "save settings failed");
        }
    }

    @FXML
    void setKeyStore(ActionEvent e) {
        KeyStoreSettingDialog dialog = new KeyStoreSettingDialog();
        dialog.keyStoreSettingProperty().setValue(context.getKeyStoreSetting());
        Optional<KeyStoreSetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, context.getMainSetting(), newConfig.get(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "save key store failed");
        }
    }

    @FXML
    void setProxy(ActionEvent e) {
        ProxySettingDialog dialog = new ProxySettingDialog();
        dialog.proxySettingProperty().setValue(context.getProxySetting());
        Optional<ProxySetting> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            SaveSettingTask task = new SaveSettingTask(context, context.getMainSetting(), context.getKeyStoreSetting(),
                    newConfig.get());
            UIUtils.runBackground(task, "save secondary proxy setting failed");
        }
    }

    /**
     * Get listened addresses, show in toolbar
     */
    private void updateListenedAddress() {
        MainSetting config = context.getMainSetting();
        String host = config.getHost().trim();
        int port = config.getPort();
        Platform.runLater(() -> listenedAddressLabel.setText("Listened " + host + ":" + port));
    }

    /**
     * Show message content in right area
     */
    private void showMessage(Message message) {
        if (message instanceof HttpMessage) {
            httpMessagePane.setHttpMessage((HttpMessage) message);
            httpMessagePane.setVisible(true);
            webSocketMessagePane.setVisible(false);
        } else if (message instanceof WebSocketMessage) {
            webSocketMessagePane.setMessage((WebSocketMessage) message);
            httpMessagePane.setVisible(false);
            webSocketMessagePane.setVisible(true);
        }
    }

    /**
     * hide right area
     */
    private void hideContent() {
        httpMessagePane.setVisible(false);
        webSocketMessagePane.setVisible(false);
    }

    @FXML
    private void clearAll(ActionEvent e) {
        catalogPane.clearAll();
    }

    @FXML
    void open(ActionEvent e) {
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
    void save(ActionEvent e) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ByProxy data", "*.bpd"));
        fileChooser.setInitialFileName("ByProxy.bpd");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        val messages = catalogPane.getMessages();
        SaveTrafficDataTask saveTask = new SaveTrafficDataTask(file.getPath(), messages);
        UIUtils.runBackground(saveTask, "Save data failed!");
    }


    @FXML
    void exportPem(ActionEvent e) throws CertificateEncodingException, IOException {
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
    void exportCrt(ActionEvent e) throws CertificateEncodingException, IOException {
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

    public void copyUrl(ActionEvent event) {
        String url = catalogPane.getSelectedMessage().getValue().getUrl();
        UIUtils.copyToClipBoard(url);
    }

    public void deleteTreeNode(ActionEvent event) {
        val treeItem = catalogPane.getSelectedTreeItem().getValue();
        if (treeItem != null) {
            catalogPane.deleteTreeNode(treeItem);
        }
    }
}
