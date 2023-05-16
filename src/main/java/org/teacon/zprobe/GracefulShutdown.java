package org.teacon.zprobe;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class GracefulShutdown implements Runnable {
    public static final int GRACEFUL_PERIOD;
    private static final Logger logger = LogUtils.getLogger();

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
        if (ZProbe.minecraftServer != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("The server is scheduled for terminate in %d seconds", GRACEFUL_PERIOD)), true);
            for (int i = GRACEFUL_PERIOD; i > 0; --i) {
                if (ZProbe.minecraftServer.getPlayerCount() == 0) break;

                if (i == GRACEFUL_PERIOD / 2 || i <= 10) {
                    server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("The server will be terminated in %d seconds", i)), true);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ZProbe.shouldTerminate = true;
            logger.info("Halting the server");
            server.halt(true);
        }

        org.apache.logging.log4j.LogManager.shutdown();
    }
}
