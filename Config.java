package com.example.oneclickanchor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("oneclickanchor.json");

    public static Config INSTANCE = new Config();

    public boolean oneClick = true;       // 1 click charge + blast
    public boolean safeSelf = true;       // clicker ko anchor damage nahi
    public boolean consumeGlowstone = true; // false = free charge

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                INSTANCE = GSON.fromJson(Files.readString(FILE), Config.class);
                if (INSTANCE == null) INSTANCE = new Config();
            } else save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
