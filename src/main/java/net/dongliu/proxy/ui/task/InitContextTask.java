package net.dongliu.proxy.ui.task;

import javafx.concurrent.Task;
import net.dongliu.proxy.Context;
import net.dongliu.proxy.setting.KeyStoreSetting;
import net.dongliu.proxy.setting.ProxySetting;
import net.dongliu.proxy.setting.ServerSetting;
import net.dongliu.proxy.setting.Settings;
import net.dongliu.proxy.ssl.RootKeyStoreGenerator;
import net.dongliu.proxy.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * When start bazehe, run this task
 *
 * @author Liu Dong
 */
public class InitContextTask extends Task<Void> {
    private static final Logger logger = LoggerFactory.getLogger(InitContextTask.class);
    private Context context;

    public InitContextTask(Context context) {
        this.context = context;
    }

    @Override
    public Void call() throws Exception {
        // load serverSetting
        updateMessage("Loading serverSetting file...");
        Path configPath = ServerSetting.configPath();
        updateProgress(1, 10);
        ServerSetting serverSetting;
        KeyStoreSetting keyStoreSetting;
        ProxySetting proxySetting;
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath);
                 BufferedInputStream bin = new BufferedInputStream(in);
                 ObjectInputStream oin = new ObjectInputStream(bin)) {
                serverSetting = (ServerSetting) oin.readObject();
                keyStoreSetting = (KeyStoreSetting) oin.readObject();
                proxySetting = (ProxySetting) oin.readObject();
            }
        } else {
            serverSetting = ServerSetting.newDefaultServerSetting();
            keyStoreSetting = KeyStoreSetting.newDefaultKeyStoreSetting();
            proxySetting = ProxySetting.newDefaultProxySetting();
        }
        updateProgress(3, 10);

        updateMessage("Loading key store file...");
        Path keyStorePath = Paths.get(keyStoreSetting.usedKeyStore());
        char[] keyStorePassword = keyStoreSetting.usedPassword().toCharArray();
        if (!Files.exists(keyStorePath)) {
            if (!keyStoreSetting.useCustom()) {
                logger.info("Generate new key store file");
                updateMessage("Generating new key store...");
                // generate one new key store
                RootKeyStoreGenerator generator = RootKeyStoreGenerator.getInstance();
                byte[] keyStoreData = generator.generate(keyStorePassword, Settings.rootCertValidityDays);
                Files.write(keyStorePath, keyStoreData);
            } else {
                UIUtils.showMessageDialog("KeyStore file not found");
                //TODO: How to deal with this?
            }

        }
        context.serverSetting(serverSetting);
        context.keyStoreSetting(keyStoreSetting);
        updateProgress(8, 10);
        context.proxySetting(proxySetting);
        updateProgress(10, 10);
        return null;
    }
}
