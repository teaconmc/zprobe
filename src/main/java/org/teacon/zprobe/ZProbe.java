package org.teacon.zprobe;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod(ZProbe.ID)
@Mod.EventBusSubscriber(modid = ZProbe.ID, value = Dist.DEDICATED_SERVER)
public final class ZProbe {
    public static final String ID = "zprobe";
    private static final Logger logger = LogUtils.getLogger();
    static MinecraftServer minecraftServer = null;

    public ZProbe() {
        logger.debug("ZProbe reached construction");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);
    }

    public void serverSetup(FMLDedicatedServerSetupEvent event) {
        logger.debug("ZProbe reached server setup");

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        boolean vanillaShutdownHookRemoved = false;

        var threads = Thread.getAllStackTraces().keySet();
        for (var thread : threads) {
            if (thread.getName().equals("Server Shutdown Thread")) {
                vanillaShutdownHookRemoved = Runtime.getRuntime().removeShutdownHook(thread);
            }
        }

        if (vanillaShutdownHookRemoved) {
            logger.info("Successfully removed vanilla shutdown hook");
        } else {
            logger.warn("Failed to remove vanilla shutdown hook");
        }

        ZProbeHttpServer.start();
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        minecraftServer = event.getServer();
        logger.info("Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(new GracefulShutdown(minecraftServer),
                "ZProbe Server Shutdown Thread"));
    }
}
