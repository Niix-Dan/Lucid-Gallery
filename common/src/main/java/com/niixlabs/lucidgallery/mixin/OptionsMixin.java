package com.niixlabs.lucidgallery.mixin;

import com.niixlabs.lucidgallery.client.KeyBinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;

@Mixin(Options.class)
public class OptionsMixin {
    @Mutable @Shadow @Final public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lucidGallery$injectKeybind(net.minecraft.client.Minecraft minecraft, File file, CallbackInfo ci) {
        KeyMapping[] newMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        newMappings[newMappings.length - 1] = KeyBinds.OPEN_GALLERY;
        keyMappings = newMappings;
    }
}