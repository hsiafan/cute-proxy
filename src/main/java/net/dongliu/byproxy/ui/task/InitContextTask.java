package net.dongliu.byproxy.ui.task;

import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.server.CAKeyStoreGenerator;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.MainSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.setting.Settings;
import net.dongliu.byproxy.ui.UIUtils;
import javafx.concurrent.Task;
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
        // load mainSetting
        updateMessage("Loading mainSetting file...");
        Path configPath = MainSetting.configPath();
        updateProgress(1, 10);
        MainSetting mainSetting;
        KeyStoreSetting keyStoreSetting;
        ProxySetting proxySetting;
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath);
                 BufferedInputStream bin = new BufferedInputStream(in);
                 ObjectInputStream oin = new ObjectInputStream(bin)) {
                mainSetting = (MainSetting) oin.readObject();
                keyStoreSetting = (KeyStoreSetting) oin.readObject();
                proxySetting = (ProxySetting) oin.readObject();
            }
        } else {
            mainSetting = MainSetting.getDefault();
            keyStoreSetting = KeyStoreSetting.getDefault();
            proxySetting = ProxySetting.getDefault();
        }
        updateProgress(3, 10);

        updateMessage("Loading key store file...");
        Path keyStorePath = Paths.get(keyStoreSetting.usedKeyStore());
        char[] keyStorePassword = keyStoreSetting.usedPassword().toCharArray();
        if (!Files.exists(keyStorePath)) {
            if (!keyStoreSetting.isUseCustom()) {
                logger.info("Generate new key store file");
                updateMessage("Generating new key store...");
                // generate one new key store
                CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
                generator.generate(keyStorePassword, Settings.rootCertificateValidates);
                byte[] keyStoreData = generator.getKeyStoreData();
                Files.write(keyStorePath, keyStoreData);
            } else {
                UIUtils.showMessageDialog("KeyStore file not found");
                //TODO: How to deal with this?
            }

        }
        context.setMainSetting(mainSetting);
        context.setKeyStoreSetting(keyStoreSetting);
        updateProgress(8, 10);
        context.setProxySetting(proxySetting);
        updateProgress(10, 10);
        return null;
    }
}
