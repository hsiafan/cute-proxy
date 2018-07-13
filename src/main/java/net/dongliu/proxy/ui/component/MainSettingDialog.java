package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.setting.ServerSetting;
import net.dongliu.proxy.utils.NetworkInfo;
import net.dongliu.proxy.utils.Networks;

import java.io.IOException;

/**
 * Show proxy configure.
 */
public class MainSettingDialog extends MyDialog<ServerSetting> {

    @FXML
    private ComboBox<NetworkInfo> hostBox;
    @FXML
    private TextField portFiled;
    @FXML
    private TextField timeoutField;

    private final ObjectProperty<ServerSetting> mainSetting = new SimpleObjectProperty<>();

    public MainSettingDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_setting.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getModel() : null;
        });

        mainSetting.addListener((o, old, n) -> setModel(n));

    }

    public ObjectProperty<ServerSetting> mainSettingProperty() {
        return mainSetting;
    }

    @FXML
    private void initialize() {
        var networkInfos = Networks.getNetworkInfoList();
        hostBox.getItems().add(new NetworkInfo("all network interface", ""));
        hostBox.getItems().addAll(networkInfos);
        hostBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(NetworkInfo networkInfo) {
                if (networkInfo == null) {
                    return null;
                }
                if (networkInfo.getIp().isEmpty()) {
                    return networkInfo.getName();
                }
                return networkInfo.getName() + " - " + networkInfo.getIp();
            }

            @Override
            public NetworkInfo fromString(String str) {
                throw new UnsupportedOperationException();
            }
        });
    }

    public void setModel(ServerSetting serverSetting) {
        var networkInfo = Lists.findOrNull(hostBox.getItems(), n -> n.getIp().equals(serverSetting.host()));
        if (networkInfo == null) {
            // network interface not found, use default listened-all one
            networkInfo = Lists.find(hostBox.getItems(), n -> n.getIp().equals("")).get();
        }
        hostBox.getSelectionModel().select(networkInfo);
        int port = serverSetting.port();
        portFiled.setText(port == 0 ? "" : String.valueOf(port));
        timeoutField.setText(String.valueOf(serverSetting.timeout()));
    }

    public ServerSetting getModel() {
        var networkInfo = hostBox.getSelectionModel().getSelectedItem();
        return new ServerSetting(networkInfo.getIp(),
                Strings.toInt(portFiled.getText(), 0),
                Strings.toInt(timeoutField.getText(), 0));
    }

}
