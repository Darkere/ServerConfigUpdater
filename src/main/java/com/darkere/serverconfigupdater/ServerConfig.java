package com.darkere.serverconfigupdater;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    private ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private ForgeConfigSpec spec;
    private ForgeConfigSpec.IntValue version;

    ServerConfig(){
        version = builder.comment("Version this world is on. This value gets updated automatically!").defineInRange("version", 0, 0, Integer.MAX_VALUE);
        spec = builder.build();
    }
    public int getCurrentVersion(){
        return version.get();
    }
    public void setVersion(int version){
        this.version.set(version);
    }

    public ForgeConfigSpec getSpec(){
        return spec;
    }
}
