package net.dongliu.proxy.ui.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.dongliu.proxy.data.Message;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.function.Consumer;

/**
 * @author Liu Dong
 */
public class LoadTask extends Task<Void> {
    private String path;
    private Consumer<Message> consumer;

    public LoadTask(String path, Consumer<Message> consumer) {
        this.path = path;
        this.consumer = consumer;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("loading...");

        try (var in = new BufferedInputStream(new FileInputStream(path));
             var ois = new ObjectInputStream(in)) {
            int magicNum = ois.readInt();
            int majorVersion = ois.readByte();
            int minorVersoin = ois.readByte();
            int total = ois.readInt();
            int readed = 0;
            while (readed < total) {
                Message message = (Message) ois.readObject();
                Platform.runLater(() -> consumer.accept(message));
                readed++;
                updateProgress(readed, total);
            }
        }

        return null;
    }
}
