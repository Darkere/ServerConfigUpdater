package com.darkere.serverconfigupdater;

import com.electronwill.nightconfig.core.utils.StringUtils;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public class CommonConfig {
    private ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private ForgeConfigSpec spec;
    private char versionToModIDSeparator = '=';
    private char versionSeparator = ';';
    private char modIDSeparator = ',';

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
        newVersion = builder.comment("Version Number. VersionNumbers are simple Integers. Use a number larger than the last version.").defineInRange("newVersion", 0, 0, Integer.MAX_VALUE);
        toDelete = builder.comment("ModID's of the ServerConfigs that will be deleted when a world with a version lower than this version is loaded the first time. Comma Separated list. (ServerConfig without -server.toml)").define("toDelete", "");
        builder.pop();

        builder.push("Version History");
        history = builder.comment("Editing these values will not affect any worlds that are already on that version.").define("history", "");
        builder.pop();
        spec = builder.build();
    }

    private void readVersionhistory() {
        String toRead = history.get();
        List<String> list = StringUtils.split(toRead,versionSeparator);
        for (String s : list) {
            if(s.isEmpty()) continue;
            List<String> strings = StringUtils.split(s, versionToModIDSeparator);
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
                modIDs.addAll(StringUtils.split(entry.getValue(),modIDSeparator));
            }
        }
        ServerConfigUpdater.SERVER_CONFIG.setVersion(maxversion);
        return modIDs;
    }
    public void updateVersionHistory(){
        readVersionhistory();
        if(newVersion.get() != 0){
            versionhistory.put(newVersion.get(),toDelete.get());
            newVersion.set(0);
            toDelete.set("");
        }
        writeVersionHistory();

    }



    private void writeVersionHistory() {
        StringBuilder builder = new StringBuilder();
        versionhistory.forEach((i, s) -> builder.append(i).append(versionToModIDSeparator).append(s).append(versionSeparator));
        history.set(builder.toString());
    }
}
