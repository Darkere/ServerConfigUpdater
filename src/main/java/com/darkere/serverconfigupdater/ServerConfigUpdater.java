package com.darkere.serverconfigupdater;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ServerConfigUpdater.MODID)
public class ServerConfigUpdater {
    // Directly reference a log4j logger.
    public static final String MODID = "serverconfigupdater";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final CommonConfig COMMON_CONFIG = new CommonConfig();
    public static final ServerConfig SERVER_CONFIG = new ServerConfig();

    public ServerConfigUpdater() {

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, ()->new IExtensionPoint.DisplayTest(()->"ANY", (remote, isServer)-> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG.getSpec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG.getSpec());
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        COMMON_CONFIG.readVersionhistory();
        FileList list = new FileList();
        list.tryDeletingFiles();
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        COMMON_CONFIG.readVersionhistory();
        Set<ModConfig> configs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER);
        Set<ModConfig> toReset = new HashSet<>();
        Set<String> modIDsToReset = COMMON_CONFIG.getModIDsToReset();
        if (modIDsToReset.isEmpty()) return;
        configs.forEach(x -> {
            if (modIDsToReset.contains(x.getModId())) {
                toReset.add(x);
                modIDsToReset.remove(x.getModId());
            }
        });

        modIDsToReset.forEach(x -> LOGGER.warn("No ServerConfig for MODID" + x + " found!"));

        MinecraftServer server = event.getServer();


        final Path serverConfig = server.getWorldPath(new LevelResource("serverconfig"));
        List<ModConfig> notOpen = new ArrayList<>();
        LogConfigsToReset(toReset);

        for (ModConfig modConfig : toReset) {
            String fileName = modConfig.getFullPath().toString();
            File file = new File(fileName);
            if(!file.delete()){
                notOpen.add(modConfig);
                LOGGER.warn("Unable to reset config for "+ modConfig.getModId());
            }
        }

        notOpen.forEach(toReset::remove);
        Method openConfig = null;
        try {
            openConfig = ConfigTracker.class.getDeclaredMethod("openConfig", ModConfig.class, Path.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        if (openConfig == null) return;
        openConfig.setAccessible(true);
        try {
            for (ModConfig modConfig : toReset) {
                if(!modConfig.getFullPath().toFile().exists())
                    openConfig.invoke(ConfigTracker.INSTANCE, modConfig, serverConfig);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void LogConfigsToReset(Set<ModConfig> toReset) {
        StringBuilder builder = new StringBuilder();
        toReset.forEach(x-> builder.append(x.getModId()).append(", "));
        String s = builder.toString();
        int comma = s.lastIndexOf(",");
        s = s.substring(0,comma);
        LOGGER.info("Resetting configs for: " + s);
    }

}
