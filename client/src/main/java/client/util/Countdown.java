package client.util;

import javafx.application.Platform;
import java.util.function.IntConsumer;

public class Countdown {
    public static void run(int seconds, IntConsumer onTick, Runnable onFinish) {
        new Thread(() -> {
            for (int i = seconds; i >= 0; i--) {
                int t = i;
                Platform.runLater(() -> onTick.accept(t));
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            Platform.runLater(onFinish);
        }, "CountdownThread").start();
    }
}
