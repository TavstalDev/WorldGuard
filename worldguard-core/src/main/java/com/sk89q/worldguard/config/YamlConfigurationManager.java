/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.config;

import com.google.common.collect.ImmutableMap;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.util.report.Unreported;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.storage.DriverType;
import com.sk89q.worldguard.protection.managers.storage.RegionDriver;
import com.sk89q.worldguard.protection.managers.storage.file.DirectoryYamlDriver;
import com.sk89q.worldguard.protection.managers.storage.sql.SQLDriver;
import com.sk89q.worldguard.util.sql.DataSourceConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class YamlConfigurationManager extends ConfigurationManager {

    @Unreported private YAMLProcessor config;

    public abstract void copyDefaults();

    @Override
    public void load() {
        copyDefaults();

        config = new YAMLProcessor(new File(getDataFolder(), "config.yml"), true, YAMLFormat.EXTENDED);
        try {
            config.load();
        } catch (IOException e) {
            log.severe("Error reading configuration for global config: ");
            e.printStackTrace();
        }

        config.removeProperty("suppress-tick-sync-warnings");
        migrateRegionsToUuid = config.getBoolean("regions.uuid-migration.perform-on-next-start", true);
        keepUnresolvedNames = config.getBoolean("regions.uuid-migration.keep-names-that-lack-uuids", true);
        useRegionsCreatureSpawnEvent = config.getBoolean("regions.use-creature-spawn-event", true);
        disableDefaultBypass = config.getBoolean("regions.disable-bypass-by-default", false);
        announceBypassStatus = config.getBoolean("regions.announce-bypass-status", false);

        useGodPermission = config.getBoolean("auto-invincible", config.getBoolean("auto-invincible-permission", false));
        useGodGroup = config.getBoolean("auto-invincible-group", false);
        useAmphibiousGroup = config.getBoolean("auto-no-drowning-group", false);
        config.removeProperty("auto-invincible-permission");
        usePlayerMove = config.getBoolean("use-player-move-event", true);
        usePlayerTeleports = config.getBoolean("use-player-teleports", true);
        particleEffects = config.getBoolean("use-particle-effects", true);
        disablePermissionCache = config.getBoolean("disable-permission-cache", false);

        deopOnJoin = config.getBoolean("security.deop-everyone-on-join", false);
        blockInGameOp = config.getBoolean("security.block-in-game-op-command", false);

        hostKeys = new HashMap<>();
        Object hostKeysRaw = config.getProperty("host-keys");
        if (!(hostKeysRaw instanceof Map)) {
            config.setProperty("host-keys", new HashMap<String, String>());
        } else {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) hostKeysRaw).entrySet()) {
                String key = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());
                hostKeys.put(key.toLowerCase(), value);
            }
        }
        hostKeysAllowFMLClients = config.getBoolean("security.host-keys-allow-forge-clients", false);

        // ====================================================================
        // Custom Messages
        // ====================================================================

        denyMessage = config.getString("messages.deny-message", "&c&lHey! &7Sorry, but you can't %what% here.");
        denyMessageFlag = new StringFlag("deny-message", denyMessage);
        entryDenyMessage = config.getString("messages.entry-deny-message", "&c&lHey! &7You are not permitted to enter this area.");
        entryDenyMessageFlag = new StringFlag("entry-deny-message", entryDenyMessage);
        exitDenyMessage = config.getString("messages.exit-deny-message", "&c&lHey! &7You are not permitted to leave this area.");
        exitDEnyMessageFlag = new StringFlag("exit-deny-message", exitDenyMessage);

        useThat = config.getString("messages.use-that", "use that");
        openThat = config.getString("messages.open-that", "open that");
        takeThat = config.getString("messages.take-that", "take that");
        sleep = config.getString("messages.sleep", "sleep");
        useAnchors = config.getString("messages.use-anchors", "use anchors");
        useExplosives = config.getString("messages.use-explosives", "use explosives");
        placeVehicles = config.getString("messages.place-vehicles", "place vehicles");
        dropItems = config.getString("messages.drop-items", "drop items");
        dropXp = config.getString("messages.drop-xp", "drop xp");
        useLingeringPotions = config.getString("messages.use-lingering-potions", "use lingering potions");
        placeThings = config.getString("messages.place-things", "place things");
        breakVehicles = config.getString("messages.break-vehicles", "break vehicles");
        pickUpItems = config.getString("messages.pick-up-items", "pick up items");
        breakThings = config.getString("messages.break-things", "break things");
        changeThat = config.getString("messages.change-that", "change that");
        hitThat = config.getString("messages.hit-that", "hit that");
        pvp = config.getString("messages.pvp", "engage in pvp");
        damageThat = config.getString("messages.damage-that", "damage that");
        harmThat = config.getString("messages.harm-that", "harm that");
        rideThat = config.getString("messages.ride-that", "ride that");
        placeFire = config.getString("messages.place-fire", "place fire");
        useFrostWalker = config.getString("messages.use-frost-walker", "use frost walker");
        placeThatBlock = config.getString("messages.place-that-block", "place that block");
        useThatBlock = config.getString("messages.use-that-block", "use that block");
        breakThatBlock = config.getString("messages.break-that-block", "break that block");
        useDynamite = config.getString("messages.use-dynamite", "use dynamite");


        // ====================================================================
        // Region store drivers
        // ====================================================================

        boolean useSqlDatabase = config.getBoolean("regions.sql.use", false);
        String sqlDsn = config.getString("regions.sql.dsn", "jdbc:mysql://localhost/worldguard");
        String sqlUsername = config.getString("regions.sql.username", "worldguard");
        String sqlPassword = config.getString("regions.sql.password", "worldguard");
        String sqlTablePrefix = config.getString("regions.sql.table-prefix", "");
        if (!useSqlDatabase) {
            config.removeProperty("regions.sql");
        } else {
            log.warning("SQL support for WorldGuard region storage is deprecated for removal in a future version. Please migrate to YAML storage.");
            log.warning("For details, see https://worldguard.enginehub.org/en/latest/regions/storage/");
        }

        DataSourceConfig dataSourceConfig = new DataSourceConfig(sqlDsn, sqlUsername, sqlPassword, sqlTablePrefix);
        SQLDriver sqlDriver = new SQLDriver(dataSourceConfig);
        DirectoryYamlDriver yamlDriver = new DirectoryYamlDriver(getWorldsDataFolder(), "regions.yml");

        this.regionStoreDriverMap = ImmutableMap.<DriverType, RegionDriver>builder()
                .put(DriverType.MYSQL, sqlDriver)
                .put(DriverType.YAML, yamlDriver)
                .build();
        this.selectedRegionStoreDriver = useSqlDatabase ? sqlDriver : yamlDriver;

        postLoad();

        config.setHeader(CONFIG_HEADER);
    }

    public void postLoad() {}

    public YAMLProcessor getConfig() {
        return config;
    }

    @Override
    public void disableUuidMigration() {
        config.setProperty("regions.uuid-migration.perform-on-next-start", false);
        if (!config.save()) {
            log.severe("Error saving configuration!");
        }
    }
}
