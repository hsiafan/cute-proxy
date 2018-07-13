/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.*;
import net.dongliu.commons.Strings;
import net.dongliu.proxy.setting.ProxySetting;

import java.io.IOException;

/**
 * Show second proxy setting.
 */
public class ProxySettingDialog extends MyDialog<ProxySetting> {

    @FXML
    private CheckBox useProxy;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField userField;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portFiled;
    @FXML
    private ToggleGroup proxyTypeGroup;

    private final ObjectProperty<ProxySetting> proxySetting = new SimpleObjectProperty<>();

    public ProxySettingDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proxy_setting.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getModel() : null;
        });

        proxySetting.addListener((o, old, n) -> setModel(n));

    }

    @FXML
    void initialize() {
        enable(false);
        useProxy.setSelected(false);
        useProxy.selectedProperty().addListener((b, o, n) -> enable(n));
    }

    private void enable(Boolean n) {
        hostField.setDisable(!n);
        portFiled.setDisable(!n);
        userField.setDisable(!n);
        passwordField.setDisable(!n);
        for (Toggle toggle : proxyTypeGroup.getToggles()) {
            RadioButton radioButton = (RadioButton) toggle;
            radioButton.setDisable(!n);
        }
    }

    public void setModel(ProxySetting proxySetting) {
        useProxy.setSelected(proxySetting.use());
        hostField.setText(proxySetting.host());
        portFiled.setText(String.valueOf(proxySetting.port()));
        userField.setText(proxySetting.user());
        passwordField.setText(proxySetting.password());
        String type = proxySetting.type();
        ObservableList<Toggle> toggles = proxyTypeGroup.getToggles();
        for (Toggle toggle : toggles) {
            if (toggle.getUserData().equals(type)) {
                toggle.setSelected(true);
            }
        }
    }

    public ProxySetting getModel() {
        boolean use = useProxy.isSelected();
        String host = hostField.getText();
        int port = Strings.toInt(portFiled.getText(), 0);
        String user = userField.getText();
        String password = passwordField.getText();
        RadioButton radioButton = (RadioButton) proxyTypeGroup.getSelectedToggle();
        String type = (String) radioButton.getUserData();
        return new ProxySetting(type, host, port, user, password, use);
    }

    public ProxySetting getProxySetting() {
        return proxySetting.get();
    }

    public ObjectProperty<ProxySetting> proxySettingProperty() {
        return proxySetting;
    }
}
