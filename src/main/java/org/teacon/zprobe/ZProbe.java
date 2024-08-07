package org.teacon.zprobe;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

@Mod(value = ZProbe.ID, dist = Dist.DEDICATED_SERVER)
public final class ZProbe {
    public static final String ID = "zprobe";
    private static final Logger logger = LogUtils.getLogger();
    static final CompletableFuture<MinecraftServer> minecraftServer = new CompletableFuture<>();

    public ZProbe(IEventBus modBus) {
        logger.debug("ZProbe reached construction");
        modBus.addListener(this::dedicatedServerSetup);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
    }

    public void dedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        logger.debug("ZProbe reached server setup");

        // ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
        //         () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        ZProbeHttpServer.start();
    }

    public void serverStarted(ServerStartedEvent event) {
        minecraftServer.complete(event.getServer());
    }
}
