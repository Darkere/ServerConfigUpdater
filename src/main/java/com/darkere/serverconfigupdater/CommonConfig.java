package com.darkere.serverconfigupdater;

import com.electronwill.nightconfig.core.utils.StringUtils;
import com.mojang.blaze3d.vertex.VertexBuilderUtils;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public class CommonConfig {
    private ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private ForgeConfigSpec spec;

    private ForgeConfigSpec.ConfigValue<String> toDelete;
    private ForgeConfigSpec.IntValue newVersion;
    private ForgeConfigSpec.ConfigValue<String> history;
    private Map<Integer, String> versionhistory = new LinkedHashMap<>();

    public CommonConfig() {
        buildConfig();
    }

    public ForgeConfigSpec getSpec() {
        return spec;
    }

    private void buildConfig() {

        builder.push("Add New Version");
        newVersion = builder.comment("Version Number. VersionNumbers are simple Integers that will get sorted. Use a number higher than the last version").defineInRange("newVersion", 0, 0, Integer.MAX_VALUE);
        toDelete = builder.comment("ModID's of the ServerConfigs that will be deleted when a world with a version lower than this version is loaded the first time. Comma Separated list. (without -server.toml)").define("toDelete", "");
        builder.pop();

        builder.push("Version History");
        history = builder.comment("Editing these values will not affect any worlds that are already on that version").define("history", "");
        builder.pop();
        spec = builder.build();
    }

    private void readVersionhistory() {
        String toRead = history.get();
        List<String> list = StringUtils.splitLines(toRead);
        for (String s : list) {
            if(s.isEmpty()) continue;
            List<String> strings = StringUtils.split(s, ':');
            int version = Integer.parseInt(strings.get(0));
            if(version != 0){
                versionhistory.put(version, strings.get(1));
            }

        }
    }
    public Set<String> getModIDsToReset(){
        int version = ServerConfigUpdater.SERVER_CONFIG.getCurrentVersion();
        Set<String> modIDs = new HashSet<>();
        int maxversion = version;
        for(Map.Entry<Integer,String> entry : versionhistory.entrySet()){
            if(entry.getKey() > version){
                maxversion = entry.getKey();
                modIDs.addAll(StringUtils.split(entry.getValue(),','));
            }
        }
        ServerConfigUpdater.SERVER_CONFIG.setVersion(maxversion);
        return modIDs;
    }
    public void updateVersionHistory(){
        readVersionhistory();
        versionhistory.put(newVersion.get(),toDelete.get());
        writeVersionHistory();
        newVersion.set(0);
        toDelete.set("");
    }



    private void writeVersionHistory() {
        StringBuilder builder = new StringBuilder();
        versionhistory.forEach((i, s) -> builder.append(i).append(":").append(s).append(System.lineSeparator()));
        history.set(builder.toString());
    }
}
