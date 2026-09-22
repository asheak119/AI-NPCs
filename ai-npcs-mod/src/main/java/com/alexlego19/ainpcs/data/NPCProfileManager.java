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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class NPCProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("ainpc");
    private static final Path SKINS_DIR = CONFIG_DIR.resolve("skins");
    private static final File PROFILES_FILE = CONFIG_DIR.resolve("npcs.json").toFile();

    public static final String STEVE_PROFILE_ID = "steve";
    public static final NPCProfile STEVE_PROFILE = new NPCProfile(STEVE_PROFILE_ID, "Steve", "steve", false, true);
    public static final String ALEX_PROFILE_ID = "alex";
    public static final NPCProfile ALEX_PROFILE = new NPCProfile(ALEX_PROFILE_ID, "Alex", "alex", true, true);

    private static Map<String, NPCProfile> profiles = new HashMap<>();
    private static final Random RANDOM = new Random();

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
        enforceDefaultProfile();
    }

    private static void enforceDefaultProfile() {
        profiles.put(STEVE_PROFILE_ID, STEVE_PROFILE);
        profiles.put(ALEX_PROFILE_ID, ALEX_PROFILE);
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);

            Map<String, NPCProfile> toSave = new HashMap<>(profiles);
            toSave.remove(STEVE_PROFILE_ID);
            toSave.remove(ALEX_PROFILE_ID);

            try (FileWriter writer = new FileWriter(PROFILES_FILE)) {
                GSON.toJson(toSave, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, NPCProfile> getProfiles() {
        return profiles;
    }

    public static NPCProfile getProfile(String id) {
        if (id == null) return STEVE_PROFILE;
        NPCProfile p = profiles.get(id);
        return p != null ? p : STEVE_PROFILE;
    }

    public static NPCProfile getRandomEnabledProfile() {
        List<NPCProfile> enabled = profiles.values().stream()
                .filter(p -> p.isEnabled() && !p.getId().equals(STEVE_PROFILE_ID) && !p.getId().equals(ALEX_PROFILE_ID))
                .collect(Collectors.toList());

        if (enabled.isEmpty()) {
            return RANDOM.nextBoolean() ? STEVE_PROFILE : ALEX_PROFILE;
        }
        return enabled.get(RANDOM.nextInt(enabled.size()));
    }

    public static void addProfile(NPCProfile profile) {
        if (!profile.getId().equals(STEVE_PROFILE_ID) && !profile.getId().equals(ALEX_PROFILE_ID)) {
            profiles.put(profile.getId(), profile);
            save();
        }
    }

    public static void removeProfile(String id) {
        if (!id.equals(STEVE_PROFILE_ID) && !id.equals(ALEX_PROFILE_ID)) {
            profiles.remove(id);
            save();
        }
    }

    public static Path getSkinsDir() {
        return SKINS_DIR;
    }
}
