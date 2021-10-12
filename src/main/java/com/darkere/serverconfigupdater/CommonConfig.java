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
    private static final char versionToModIDSeparator = '=';
    private static final char modIDSeparator = ',';

    private ForgeConfigSpec.ConfigValue<List<? extends String>> history;
    private Map<Integer, String> versionhistory = new LinkedHashMap<>();
    private ForgeConfigSpec.ConfigValue<List<? extends String>> filesToDelete;
    private ForgeConfigSpec.BooleanValue deleteFolders;

    public CommonConfig() {
        buildConfig();
    }

    public ForgeConfigSpec getSpec() {
        return spec;
    }

    private void buildConfig() {

        builder.push("Versions");
        history = builder.comment("Define a version here. On world load the mod will look up the serverconfig version and reset all files that specified up to the newest version.")
            .defineList("versions", List.of("0=modid"), o -> true);
        builder.pop();
        builder.push("File Deleter");
        filesToDelete = builder.comment("This is intended for deleting files for pack updates. This is a last resort! Replace with empty files instead when possible. The file will be deleted every launch if it exists! Specify the path to the file. Comma Separated List. Example: scripts/badscript.zs")
            .defineListAllowEmpty(List.of("files"),()-> List.of(""), o -> true);
        deleteFolders = builder.comment("By default Folders are only deleted if they are empty. Set to true to change that.")
            .define("deleteFoldersWithContent", false);
        builder.pop();
        spec = builder.build();
    }

    public boolean shouldDeleteFolders() {
        return deleteFolders.get();
    }

    public List<Path> getFilesToDelete() {
        List<? extends String> files = filesToDelete.get();
        if (files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream().map(Paths::get).collect(Collectors.toList());
    }

    public void readVersionhistory() {
        List<? extends String> list = history.get();
        for (String s : list) {
            if (s.isEmpty()) continue;
            List<String> strings = StringUtils.split(s, versionToModIDSeparator);
            int version = Integer.parseInt(strings.get(0));
            if (version != 0) {
                versionhistory.put(version, strings.get(1));
            }

        }
    }

    public Set<String> getModIDsToReset() {
        int version = ServerConfigUpdater.SERVER_CONFIG.getCurrentVersion();
        Set<String> modIDs = new HashSet<>();
        int maxversion = version;
        for (Map.Entry<Integer, String> entry : versionhistory.entrySet()) {
            if (entry.getKey() > version) {
                maxversion = entry.getKey();
                List<String> strings = StringUtils.split(entry.getValue(), modIDSeparator);
                strings.forEach(s -> s = s.trim());
                modIDs.addAll(strings);
            }
        }
        ServerConfigUpdater.SERVER_CONFIG.setVersion(maxversion);
        return modIDs;
    }
}
