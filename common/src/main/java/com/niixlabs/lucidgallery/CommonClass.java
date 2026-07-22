package com.niixlabs.lucidgallery;

import com.niixlabs.lucidgallery.config.LucidConfig;

public class CommonClass {
    public static void init() {
        LucidConfig.load();
        if (LucidConfig.useConfigWatcher) LucidConfig.startWatcher();
    }
}
