package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.dongliu.proxy.setting.KeyStoreSetting;

import java.io.File;
import java.io.IOException;

/**
 * Show proxy configure.
 */
public class KeyStoreSettingDialog extends MyDialog<KeyStoreSetting> {

    @FXML
    private CheckBox useCustomCheckBox;
    @FXML
    private TextField keyStoreField;
    @FXML
    private Button chooseFileButton;
    @FXML
    private PasswordField keyStorePasswordField;

    private final ObjectProperty<KeyStoreSetting> keyStoreSetting = new SimpleObjectProperty<>();

    public ObjectProperty<KeyStoreSetting> keyStoreSettingProperty() {
        return keyStoreSetting;
    }

    public KeyStoreSettingDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/key_store_setting.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getModel() : null;
        });

        keyStoreSetting.addListener((o, old, n) -> setModel(n));

    }

    @FXML
    void initialize() {
        useCustomCheckBox.selectedProperty().addListener((w, o, n) -> setUseCustom(n));
    }

    private void setUseCustom(boolean selected) {
        keyStoreField.setDisable(!selected);
        chooseFileButton.setDisable(!selected);
        keyStorePasswordField.setDisable(!selected);
    }

    public void setModel(KeyStoreSetting setting) {
        useCustomCheckBox.setSelected(setting.useCustom());
        keyStoreField.setText(setting.keyStore());
        keyStorePasswordField.setText(setting.keyStorePassword());
    }


    public KeyStoreSetting getModel() {
        return new KeyStoreSetting(keyStoreField.getText(), keyStorePasswordField.getText(),
                useCustomCheckBox.isSelected());
    }


    @FXML
    void choseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        if (keyStoreField.getText() != null && !keyStoreField.getText().isEmpty()) {
            File file = new File(keyStoreField.getText());
            if (file.exists()) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore File", "*.p12"));
        File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
            keyStoreField.setText(file.getAbsolutePath());
        }
    }
}
