package com.niixlabs.lucidgallery;

import net.fabricmc.api.ClientModInitializer;

public class LucidGalleryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommonClass.init();
    }
}
