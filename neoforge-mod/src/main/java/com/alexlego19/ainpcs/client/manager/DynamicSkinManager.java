package com.alexlego19.ainpcs.client.manager;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicSkinManager {
    private static final Map<String, ResourceLocation> skinCache = new HashMap<>();

    // We can use a deterministic UUID approach or random UUID to fetch default skins.
    // DefaultPlayerSkin uses UUID to decide whether to return Alex or Steve.
    private static final UUID STEVE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ALEX_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static ResourceLocation getSkin(String skinId, boolean isSlim) {
        if (skinId == null || skinId.isEmpty()) {
            return isSlim ? DefaultPlayerSkin.getDefaultSkin(ALEX_UUID) : DefaultPlayerSkin.getDefaultSkin(STEVE_UUID);
        }

        if (skinCache.containsKey(skinId)) {
            return skinCache.get(skinId);
        }

        Path skinPath = NPCProfileManager.getSkinsDir().resolve(skinId + (skinId.endsWith(".png") ? "" : ".png"));
        if (Files.exists(skinPath)) {
            try (InputStream is = new FileInputStream(skinPath.toFile())) {
                NativeImage nativeImage = NativeImage.read(is);
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                ResourceLocation resourceLocation = new ResourceLocation(AiNpcsMod.MODID, "dynamic_skin_" + skinId.toLowerCase().replaceAll("[^a-z0-9_.-]", ""));

                // Needs to be run on the main render thread ideally, but typically fetching textures this way happens there anyway
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
                });

                skinCache.put(skinId, resourceLocation);
                return resourceLocation;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Fallback if loading fails
        return isSlim ? DefaultPlayerSkin.getDefaultSkin(ALEX_UUID) : DefaultPlayerSkin.getDefaultSkin(STEVE_UUID);
    }

    public static void clearCache() {
        skinCache.clear();
    }
}
