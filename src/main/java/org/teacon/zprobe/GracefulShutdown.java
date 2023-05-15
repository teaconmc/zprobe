package org.teacon.zprobe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class GracefulShutdown implements Runnable {
    public static final int GRACEFUL_PERIOD;

    static {
        if (System.getenv().containsKey("ZPROBE_GRACEFUL_PERIOD")) {
            GRACEFUL_PERIOD = Integer.parseInt("ZPROBE_GRACEFUL_PERIOD");
        } else {
            GRACEFUL_PERIOD = 30;
        }
    }

    private final MinecraftServer server;

    public GracefulShutdown(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        server.sendSystemMessage(Component.literal(String.format("The server is scheduled for terminate in %d seconds", GRACEFUL_PERIOD)));
        for (int i = GRACEFUL_PERIOD; i > 0; --i) {
            if (i == GRACEFUL_PERIOD / 2 || i <= 10) {
                server.sendSystemMessage(Component.literal(String.format("The server will be terminated in %d seconds", i)));
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        server.halt(true);
        org.apache.logging.log4j.LogManager.shutdown();
    }
}
