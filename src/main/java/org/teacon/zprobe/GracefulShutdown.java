package org.teacon.zprobe;

import com.google.common.util.concurrent.Uninterruptibles;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GracefulShutdown implements Runnable {
    public static final int GRACEFUL_PERIOD;
    private static final Logger logger = LogUtils.getLogger();

    static {
        if (System.getenv().containsKey("ZPROBE_GRACEFUL_PERIOD")) {
            GRACEFUL_PERIOD = Integer.parseInt(System.getenv("ZPROBE_GRACEFUL_PERIOD"));
        } else {
            GRACEFUL_PERIOD = 30;
        }
    }

    private final CompletableFuture<MinecraftServer> server;
    private final Writer writer;

    public GracefulShutdown(CompletableFuture<MinecraftServer> server, Writer writer) {
        this.server = server;
        this.writer = writer;
    }

    @Override
    public void run() {
        // noinspection resource
        var minecraftServer = this.server.join();
        if (minecraftServer != null) {
            var broadcastMessage = String.format("The server is scheduled for terminate in %d seconds", GRACEFUL_PERIOD);
            minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(broadcastMessage), true);
            for (int i = GRACEFUL_PERIOD - 1; i >= 0; --i) {
                if (minecraftServer.getPlayerCount() == 0) break;
                Uninterruptibles.sleepUninterruptibly(1000L, TimeUnit.MILLISECONDS);
                broadcastMessage = String.format("The server will be terminated in %d seconds", i);
                if (i == GRACEFUL_PERIOD / 2 || i <= 10) {
                    minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(broadcastMessage), true);
                }
                try {
                    this.writer.write(broadcastMessage + "\r\n");
                    this.writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.info("Halting the server");
            minecraftServer.halt(true);
        }

        org.apache.logging.log4j.LogManager.shutdown();
    }
}
