# ServerConfigUpdater
Adding a way for packdevs to update ServerConfigs

In 1.13+ Forge introduced ServerConfigs that can be found inside the world folder. 

The fact that they are inside the world folder makes it impossible to update them after they are initially generated. 

 

This mod allows you to selectively delete ServerConfigs when a world loads. Causing them to be regenerated from the defaultconfig folder. 

 

To use add a new Version into the config and specify the modID's of the serverconfigs you want to update.

New Versions are integrated into the version history when the game loads and on world load. 

The Project is available here:
https://www.curseforge.com/minecraft/mc-mods/serverconfig-updater

or through the Twitch Launcher
