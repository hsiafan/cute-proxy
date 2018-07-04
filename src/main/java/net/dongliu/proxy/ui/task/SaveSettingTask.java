package net.dongliu.proxy.ui.task;

import javafx.concurrent.Task;
import net.dongliu.proxy.Context;
import net.dongliu.proxy.setting.KeyStoreSetting;
import net.dongliu.proxy.setting.ProxySetting;
import net.dongliu.proxy.setting.ServerSetting;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * @author Liu Dong
 */
public class SaveSettingTask extends Task<Void> {

    private Context context;
    private ServerSetting serverSetting;
    private KeyStoreSetting keyStoreSetting;
    private ProxySetting proxySetting;

    public SaveSettingTask(Context context, ServerSetting serverSetting,
                           KeyStoreSetting keyStoreSetting,
                           ProxySetting proxySetting) {
        this.context = context;
        this.serverSetting = requireNonNull(serverSetting);
        this.keyStoreSetting = requireNonNull(keyStoreSetting);
        this.proxySetting = requireNonNull(proxySetting);
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 10);
        // if need to load new key store
        context.serverSetting(serverSetting);
        updateProgress(1, 10);
        context.keyStoreSetting(keyStoreSetting);
        updateProgress(5, 10);
        context.proxySetting(proxySetting);
        updateProgress(7, 10);
        updateMessage("Save serverSetting to file");
        Path configPath = ServerSetting.configPath();
        try (var os = Files.newOutputStream(configPath);
             var bos = new BufferedOutputStream(os);
             var oos = new ObjectOutputStream(bos)) {
            oos.writeObject(serverSetting);
            oos.writeObject(keyStoreSetting);
            oos.writeObject(proxySetting);
        }
        updateProgress(10, 10);
        return null;
    }
}
