package com.alexlego19.ainpcs.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NPCProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("ainpc");
    private static final Path SKINS_DIR = CONFIG_DIR.resolve("skins");
    private static final File PROFILES_FILE = CONFIG_DIR.resolve("npcs.json").toFile();

    private static Map<String, NPCProfile> profiles = new HashMap<>();

    public static void load() {
        try {
            Files.createDirectories(SKINS_DIR);

            if (PROFILES_FILE.exists()) {
                try (FileReader reader = new FileReader(PROFILES_FILE)) {
                    Type type = new TypeToken<Map<String, NPCProfile>>(){}.getType();
                    profiles = GSON.fromJson(reader, type);
                    if (profiles == null) {
                        profiles = new HashMap<>();
                    }
                }
            } else {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(PROFILES_FILE)) {
                GSON.toJson(profiles, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, NPCProfile> getProfiles() {
        return profiles;
    }

    public static NPCProfile getProfile(String id) {
        return profiles.get(id);
    }

    public static void addProfile(NPCProfile profile) {
        profiles.put(profile.getId(), profile);
        save();
    }

    public static void removeProfile(String id) {
        profiles.remove(id);
        save();
    }

    public static Path getSkinsDir() {
        return SKINS_DIR;
    }
}
