package com.niixlabs.lucidgallery.client.gui.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.niixlabs.lucidgallery.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextureLoader {
    private static final Map<String, DynamicTexture> LOADED_TEXTURES = new HashMap<>();
    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();

    public static ResourceLocation getOrLoadTexture(File file) {
        String path = file.getAbsolutePath();
        if (LOCATIONS.containsKey(path)) {
            return LOCATIONS.get(path);
        }

        try (InputStream stream = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            String safeName = file.getName().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "screenshot_" + safeName);

            Minecraft.getInstance().getTextureManager().register(location, texture);
            LOADED_TEXTURES.put(path, texture);
            LOCATIONS.put(path, location);
            return location;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void clearCache() {
        for (Map.Entry<String, ResourceLocation> entry : LOCATIONS.entrySet()) {
            Minecraft.getInstance().getTextureManager().release(entry.getValue());
        }
        for (DynamicTexture texture : LOADED_TEXTURES.values()) {
            texture.close();
        }
        LOADED_TEXTURES.clear();
        LOCATIONS.clear();
    }
}