package net.dongliu.byproxy.ui.task;

import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.ServerSetting;
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
        context.setServerSetting(serverSetting);
        updateProgress(1, 10);
        context.setKeyStoreSetting(keyStoreSetting);
        updateProgress(5, 10);
        context.setProxySetting(proxySetting);
        updateProgress(7, 10);
        updateMessage("Save serverSetting to file");
        Path configPath = ServerSetting.configPath();
        try (OutputStream os = Files.newOutputStream(configPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(serverSetting);
            oos.writeObject(keyStoreSetting);
            oos.writeObject(proxySetting);
        }
        updateProgress(10, 10);
        return null;
    }
}
