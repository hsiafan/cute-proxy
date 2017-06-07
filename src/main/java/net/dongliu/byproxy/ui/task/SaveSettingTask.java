package net.dongliu.byproxy.ui.task;

import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.MainSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import javafx.concurrent.Task;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * @author Liu Dong
 */
public class SaveSettingTask extends Task<Void> {

    private Context context;
    private MainSetting mainSetting;
    private KeyStoreSetting keyStoreSetting;
    private ProxySetting proxySetting;

    public SaveSettingTask(Context context, MainSetting mainSetting,
                           KeyStoreSetting keyStoreSetting,
                           ProxySetting proxySetting) {
        this.context = context;
        this.mainSetting = requireNonNull(mainSetting);
        this.keyStoreSetting = requireNonNull(keyStoreSetting);
        this.proxySetting = requireNonNull(proxySetting);
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 10);
        // if need to load new key store
        context.setMainSetting(mainSetting);
        updateProgress(1, 10);
        context.setKeyStoreSetting(keyStoreSetting);
        updateProgress(5, 10);
        context.setProxySetting(proxySetting);
        updateProgress(7, 10);
        updateMessage("Save mainSetting to file");
        Path configPath = MainSetting.configPath();
        try (OutputStream os = Files.newOutputStream(configPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(mainSetting);
            oos.writeObject(keyStoreSetting);
            oos.writeObject(proxySetting);
        }
        updateProgress(10, 10);
        return null;
    }
}
