package com.darkere.serverconfigupdater;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

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
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG.getSpec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG.getSpec());
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        COMMON_CONFIG.updateVersionHistory();
        FileList list = new FileList();
        list.tryDeletingFiles();
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerAboutToStartEvent event) {
        COMMON_CONFIG.updateVersionHistory();
        Field configsets = null;
        try {
            configsets = ConfigTracker.class.getDeclaredField("configSets");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (configsets == null) return;
        configsets.setAccessible(true);
        EnumMap<ModConfig.Type, Set<ModConfig>> sets = null;
        try {
            sets = (EnumMap<ModConfig.Type, Set<ModConfig>>) configsets.get(ConfigTracker.INSTANCE);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Set<ModConfig> configs = sets.get(ModConfig.Type.SERVER);
        Set<ModConfig> toReset = new HashSet<>();
        Set<String> modIDsToReset = COMMON_CONFIG.getModIDsToReset();
        if (modIDsToReset.isEmpty()) return;
        configs.forEach(x -> {
            if (modIDsToReset.contains(x.getModId())) {
                toReset.add(x);
            }
        });

        modIDsToReset.forEach(x -> LOGGER.warn("No ServerConfig for MODID" + x + " found!"));

        MinecraftServer server = event.getServer();


        final Path serverConfig = server.getWorldPath(new FolderName("serverconfig"));
        for (ModConfig modConfig : toReset) {
            String fileName = modConfig.getFullPath().toString();
            LOGGER.info("Resetting " + fileName);
            File file = new File(fileName);
            file.delete();
        }
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

}
