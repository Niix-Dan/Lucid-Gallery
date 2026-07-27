package com.niixlabs.lucidgallery.client.gui.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.niixlabs.lucidgallery.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TextureLoader {
    private static final Map<String, DynamicTexture> LOADED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> LOCATIONS = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();

    public static ResourceLocation getOrLoadTexture(File file) {
        String path = file.getAbsolutePath();

        if (LOCATIONS.containsKey(path)) {
            return LOCATIONS.get(path);
        }

        if (LOADING.contains(path)) {
            return null;
        }
        LOADING.add(path);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try (InputStream stream = new FileInputStream(file)) {
                NativeImage image = NativeImage.read(stream);
                RenderSystem.recordRenderCall(() -> {
                    try {
                        DynamicTexture texture = new DynamicTexture(image);
                        String safeName = file.getName().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
                        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "screenshot_" + safeName);

                        Minecraft.getInstance().getTextureManager().register(location, texture);
                        LOADED_TEXTURES.put(path, texture);
                        LOCATIONS.put(path, location);
                    } finally {
                        LOADING.remove(path);
                    }
                });
            } catch (Exception e) {
                LOADING.remove(path);
            }
        });

        return null;
    }

    public static void unloadTexture(File file) {
        String path = file.getAbsolutePath();

        if (LOCATIONS.containsKey(path)) {
            Minecraft.getInstance().getTextureManager().release(LOCATIONS.get(path));
            LOCATIONS.remove(path);
        }

        if (LOADED_TEXTURES.containsKey(path)) {
            LOADED_TEXTURES.get(path).close();
            LOADED_TEXTURES.remove(path);
        }

        LOADING.remove(path);
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
        LOADING.clear();
    }
}