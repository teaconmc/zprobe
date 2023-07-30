package org.teacon.zprobe;

import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class ZProbeHttpServer {
    private static final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, "ZProbe Executor Thread"));
    private static final Logger logger = LogUtils.getLogger();
    private static final HttpServer httpServer;
    private static final int LISTEN_PORT;
    private static final int MAX_TICK_PERIOD;
    private static volatile boolean serverHealthy = true;
    private static volatile long lastTick = -1;

    static {
        if (System.getenv().containsKey("ZPROBE_LISTEN_PORT")) {
            LISTEN_PORT = Integer.parseInt(System.getenv("ZPROBE_LISTEN_PORT"));
        } else {
            LISTEN_PORT = 35565;
        }

        if (System.getenv().containsKey("ZPROBE_MAX_TICK_PERIOD")) {
            MAX_TICK_PERIOD = Integer.parseInt(System.getenv("ZPROBE_MAX_TICK_PERIOD"));
        } else {
            MAX_TICK_PERIOD = 5;
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executor.scheduleAtFixedRate(() -> {
            var minecraftServer = ZProbe.minecraftServer.getNow(null);
            if (minecraftServer == null) return;
            serverHealthy = lastTick != minecraftServer.getNextTickTime();
            if (!serverHealthy) {
                logger.warn("Server isn't ticking for at least 5 seconds, status is set to unhealthy");
            }
            lastTick = minecraftServer.getNextTickTime();
        }, MAX_TICK_PERIOD, MAX_TICK_PERIOD, TimeUnit.SECONDS);

        httpServer.createContext("/ready", exchange -> {
            exchange.sendResponseHeaders(ZProbe.minecraftServer.isDone() ? 204 : 500, -1);
            exchange.close();
        });
        httpServer.createContext("/live", exchange -> {
            exchange.sendResponseHeaders(serverHealthy ? 204 : 500, -1);
            exchange.close();
        });
        httpServer.createContext("/stop", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            new GracefulShutdown(ZProbe.minecraftServer, new OutputStreamWriter(exchange.getResponseBody())).run();
            exchange.close();
        });
    }

    static void start() {
        logger.info("Starting probe http server on port " + LISTEN_PORT);
        httpServer.setExecutor(Util.ioPool());
        httpServer.start();
    }

    static void stop() {
        httpServer.stop(0);
    }
}
