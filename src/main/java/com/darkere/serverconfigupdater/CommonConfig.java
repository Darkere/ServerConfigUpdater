package com.darkere.serverconfigupdater;

import com.electronwill.nightconfig.core.utils.StringUtils;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
    private ForgeConfigSpec.ConfigValue<String> filesToDelete;
    private ForgeConfigSpec.BooleanValue deleteFolders;
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
        builder.push("File Deleter");
        filesToDelete = builder.comment("This is intended for deleting datapacks and/or craft tweaker scripts. The file will be deleted every launch if it exists! No access to saves or world folder. Specify the path to the file. Comma Separated List. Example: scripts/badscript.zs").define("filesToDelete","");
        deleteFolders = builder.comment("By default Folders are only deleted if they are empty. Set to true to change that.").define("deleteFoldersWithContent", false);
        builder.pop();
        builder.push("Version History");
        history = builder.comment("Editing these values will not affect any worlds that are already on that version.").define("history", "");
        builder.pop();
        spec = builder.build();
    }

    public boolean shouldDeleteFolders(){
        return deleteFolders.get();
    }
    public List<Path> getFilesToDelete(){
        String files = filesToDelete.get();
        if(files.isEmpty()){
            return new ArrayList<>();
        }
        List <String> strings = StringUtils.split(files,modIDSeparator);
        return strings.stream().map(x-> Paths.get(x)).collect(Collectors.toList());
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
                List<String> strings = StringUtils.split(entry.getValue(),modIDSeparator);
                strings.forEach(s-> s = s.trim());
                modIDs.addAll(strings);
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
