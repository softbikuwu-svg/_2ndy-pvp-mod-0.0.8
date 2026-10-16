package com.secondy.client.module;

import com.secondy.client.util.Theme;

public class ChromaModule extends Module {
    public ChromaModule() {
        super("Chroma", "Rainbow text in the HUD and menu", Category.VISUAL);
    }

    @Override public void onEnable() { Theme.chroma = true; }
    @Override public void onDisable() { Theme.chroma = false; }
}
