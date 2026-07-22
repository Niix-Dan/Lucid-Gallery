package com.niixlabs.lucidgallery.mixin;

import com.niixlabs.lucidgallery.client.gui.screen.GalleryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void lucidGallery$addPauseButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.lucidgallery.gallery_button"), b -> {
            Minecraft.getInstance().setScreen(new GalleryScreen());
        }).bounds(this.width - 105, this.height - 25, 100, 20).build());
    }
}