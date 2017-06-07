package net.dongliu.byproxy.ui.component;

import net.dongliu.byproxy.setting.MainSetting;
import net.dongliu.byproxy.utils.NetUtils;
import net.dongliu.byproxy.utils.NetworkInfo;
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
import net.dongliu.commons.exception.Throwables;

import java.io.IOException;
import java.util.List;

/**
 * Show proxy configure.
 */
public class MainSettingDialog extends MyDialog<MainSetting> {

    @FXML
    private ComboBox<NetworkInfo> hostBox;
    @FXML
    private TextField portFiled;
    @FXML
    private TextField timeoutField;

    private final ObjectProperty<MainSetting> mainSetting = new SimpleObjectProperty<>();

    public MainSettingDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_setting.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw Throwables.throwAny(e);
        }

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getModel() : null;
        });

        mainSetting.addListener((o, old, n) -> setModel(n));

    }

    public MainSetting getMainSetting() {
        return mainSetting.get();
    }

    public ObjectProperty<MainSetting> mainSettingProperty() {
        return mainSetting;
    }

    @FXML
    void initialize() {
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

    public void setModel(MainSetting mainSetting) {
        NetworkInfo networkInfo = Lists.findFirst(hostBox.getItems(), n -> n.getIp().equals(mainSetting.getHost()));
        if (networkInfo == null) {
            // network interface not found, use default listened-all one
            networkInfo = Lists.findFirst(hostBox.getItems(), n -> n.getIp().equals(""));
        }
        hostBox.getSelectionModel().select(networkInfo);
        int port = mainSetting.getPort();
        portFiled.setText(port == 0 ? "" : String.valueOf(port));
        timeoutField.setText(String.valueOf(mainSetting.getTimeout()));
    }

    public MainSetting getModel() {
        NetworkInfo networkInfo = hostBox.getSelectionModel().getSelectedItem();
        return new MainSetting(networkInfo.getIp(),
                Strings.toInt(portFiled.getText()),
                Strings.toInt(timeoutField.getText()));
    }

}
