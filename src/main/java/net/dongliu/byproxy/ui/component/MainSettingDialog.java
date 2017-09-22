package net.dongliu.byproxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.utils.NetUtils;
import net.dongliu.byproxy.utils.NetworkInfo;
import net.dongliu.byproxy.utils.StringUtils;

import java.io.IOException;
import java.util.List;

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
        List<NetworkInfo> networkInfos = NetUtils.getNetworkInfoList();
        hostBox.getItems().add(new NetworkInfo("all network interface", ""));
        hostBox.getItems().addAll(networkInfos);
        hostBox.setConverter(new StringConverter<NetworkInfo>() {
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
        NetworkInfo networkInfo = hostBox.getItems().stream().filter(n -> n.getIp().equals(serverSetting.getHost()))
                .findFirst().orElse(null);
        if (networkInfo == null) {
            // network interface not found, use default listened-all one
            networkInfo = hostBox.getItems().stream().filter(n -> n.getIp().equals("")).findFirst().get();
        }
        hostBox.getSelectionModel().select(networkInfo);
        int port = serverSetting.getPort();
        portFiled.setText(port == 0 ? "" : String.valueOf(port));
        timeoutField.setText(String.valueOf(serverSetting.getTimeout()));
    }

    public ServerSetting getModel() {
        NetworkInfo networkInfo = hostBox.getSelectionModel().getSelectedItem();
        return new ServerSetting(networkInfo.getIp(),
                StringUtils.toInt(portFiled.getText()),
                StringUtils.toInt(timeoutField.getText()));
    }

}
