package org.teacon.zprobe;

import com.google.common.util.concurrent.Uninterruptibles;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GracefulShutdown implements Runnable {
    public static final int GRACEFUL_PERIOD;
    private static final Logger logger = LogUtils.getLogger();
    private static final Map<String, String> TEMPLATES;

    static {
        if (System.getenv().containsKey("ZPROBE_GRACEFUL_PERIOD")) {
            GRACEFUL_PERIOD = Integer.parseInt(System.getenv("ZPROBE_GRACEFUL_PERIOD"));
        } else {
            GRACEFUL_PERIOD = 30;
        }
        TEMPLATES = Map.of(
                "zh_cn", "服务器计划于 %d 秒后关闭",
                "en_us", "The server is scheduled for termination in %d second(s)");
    }

    private final CompletableFuture<MinecraftServer> server;
    private final Writer writer;

    public GracefulShutdown(CompletableFuture<MinecraftServer> server, Writer writer) {
        this.server = server;
        this.writer = writer;
    }

    @Override
    public void run() {
        var minecraftServer = this.server.join();
        if (minecraftServer != null) {
            this.sendShutdownMessages(minecraftServer, GRACEFUL_PERIOD, false);
            for (int i = GRACEFUL_PERIOD - 1; i >= 0; --i) {
                if (minecraftServer.getPlayerCount() == 0) break;
                Uninterruptibles.sleepUninterruptibly(1000L, TimeUnit.MILLISECONDS);
                if (i % 10 == 0 || i <= 10) {
                    this.sendShutdownMessages(minecraftServer, i, false);
                }
                this.sendShutdownMessages(minecraftServer, i, true);
            }
            minecraftServer.execute(() -> {
                logger.info("Halting the server");
                ZProbeHttpServer.stop();
                minecraftServer.halt(false);
            });
        }

        org.apache.logging.log4j.LogManager.shutdown();
    }

    private void sendShutdownMessages(MinecraftServer server, int terminationSecond, boolean overlay) {
        var message = String.format(TEMPLATES.get("en_us"), terminationSecond);
        if (!overlay) {
            server.sendSystemMessage(Component.literal(message));
            try {
                this.writer.write(message + "\r\n");
                this.writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (var player : server.getPlayerList().getPlayers()) {
            message = String.format(TEMPLATES.getOrDefault(player.getLanguage(), TEMPLATES.get("en_us")), terminationSecond);
            player.sendSystemMessage(Component.literal(message), overlay);
        }
    }
}
