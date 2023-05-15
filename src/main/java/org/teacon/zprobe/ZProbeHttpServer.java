package org.teacon.zprobe;

import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class ZProbeHttpServer {
    private static final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, "ZProbe Executor Thread"));
    private static final Logger logger = LogUtils.getLogger();
    private static final HttpServer httpServer;
    private static final int LISTEN_PORT;
    private static volatile boolean serverHealthy = true;
    private static volatile int lastTick = 0;

    static {
        if (System.getenv().containsKey("ZPROBE_LISTEN_PORT")) {
            LISTEN_PORT = Integer.parseInt(System.getenv("ZPROBE_LISTEN_PORT"));
        } else {
            LISTEN_PORT = 35565;
        }

        try {
            httpServer = HttpServer.create(InetSocketAddress.createUnresolved("0.0.0.0", LISTEN_PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executor.scheduleAtFixedRate(() -> {
            if (ZProbe.minecraftServer == null) return;
            serverHealthy = lastTick != ZProbe.minecraftServer.getTickCount();
            if (!serverHealthy) {
                logger.warn("Server isn't ticking for at least 5 seconds, status is set to unhealthy");
            }
            lastTick = ZProbe.minecraftServer.getTickCount();
        }, 1, 5, TimeUnit.SECONDS);

        httpServer.createContext("/ready", exchange -> {
            exchange.sendResponseHeaders(ZProbe.minecraftServer != null ? 200 : 500, -1);
            exchange.close();
        });
        httpServer.createContext("/live", exchange -> {
            exchange.sendResponseHeaders(serverHealthy ? 200 : 500, -1);
            exchange.close();
        });
    }

    static void start() {
        logger.info("Starting probe http server");
        httpServer.start();
    }
}
