package com.boyka.dildo;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class BoykaConfig {

    // --- Auto Drip ---
    public static boolean autoDripEnabled = false;
    public static int autoDripDelayTicks = 0; // 0 = instant

    // --- Trap Helper ---
    public static boolean trapHelperEnabled = false;
    public static int trapHelperDelayTicks = 0; // 0-5
    public static double trapHelperReach = 3.0; // max 3 bloki

    public static boolean destroyMinecartNormal = true;
    public static boolean destroyMinecartChest = true;
    public static boolean destroyMinecartFurnace = true;
    public static boolean destroyMinecartHopper = true;
    public static boolean destroyMinecartTnt = true;

    public static boolean destroyBanners = true;
    public static boolean destroyPaintings = true;
    public static boolean destroySigns = true;

    public static boolean autoCobweb = true;

    // --- Keybinds (modifiable from GUI) ---
    public static int keyMenu = InputUtil.GLFW_KEY_RIGHT_BRACKET;
    public static int keyAutoDrip = InputUtil.GLFW_KEY_G;
    public static int keyTrapHelper = InputUtil.GLFW_KEY_H;

    private static final File CONFIG_FILE =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "boykadildo.properties");

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            p.load(in);

            autoDripDelayTicks = Integer.parseInt(p.getProperty("autoDripDelayTicks", "0"));
            trapHelperDelayTicks = Integer.parseInt(p.getProperty("trapHelperDelayTicks", "0"));
            trapHelperReach = Double.parseDouble(p.getProperty("trapHelperReach", "3.0"));

            destroyMinecartNormal = Boolean.parseBoolean(p.getProperty("destroyMinecartNormal", "true"));
            destroyMinecartChest = Boolean.parseBoolean(p.getProperty("destroyMinecartChest", "true"));
            destroyMinecartFurnace = Boolean.parseBoolean(p.getProperty("destroyMinecartFurnace", "true"));
            destroyMinecartHopper = Boolean.parseBoolean(p.getProperty("destroyMinecartHopper", "true"));
            destroyMinecartTnt = Boolean.parseBoolean(p.getProperty("destroyMinecartTnt", "true"));

            destroyBanners = Boolean.parseBoolean(p.getProperty("destroyBanners", "true"));
            destroyPaintings = Boolean.parseBoolean(p.getProperty("destroyPaintings", "true"));
            destroySigns = Boolean.parseBoolean(p.getProperty("destroySigns", "true"));

            autoCobweb = Boolean.parseBoolean(p.getProperty("autoCobweb", "true"));

            keyMenu = Integer.parseInt(p.getProperty("keyMenu", String.valueOf(InputUtil.GLFW_KEY_RIGHT_BRACKET)));
            keyAutoDrip = Integer.parseInt(p.getProperty("keyAutoDrip", String.valueOf(InputUtil.GLFW_KEY_G)));
            keyTrapHelper = Integer.parseInt(p.getProperty("keyTrapHelper", String.valueOf(InputUtil.GLFW_KEY_H)));

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("autoDripDelayTicks", String.valueOf(autoDripDelayTicks));
        p.setProperty("trapHelperDelayTicks", String.valueOf(trapHelperDelayTicks));
        p.setProperty("trapHelperReach", String.valueOf(trapHelperReach));

        p.setProperty("destroyMinecartNormal", String.valueOf(destroyMinecartNormal));
        p.setProperty("destroyMinecartChest", String.valueOf(destroyMinecartChest));
        p.setProperty("destroyMinecartFurnace", String.valueOf(destroyMinecartFurnace));
        p.setProperty("destroyMinecartHopper", String.valueOf(destroyMinecartHopper));
        p.setProperty("destroyMinecartTnt", String.valueOf(destroyMinecartTnt));

        p.setProperty("destroyBanners", String.valueOf(destroyBanners));
        p.setProperty("destroyPaintings", String.valueOf(destroyPaintings));
        p.setProperty("destroySigns", String.valueOf(destroySigns));

        p.setProperty("autoCobweb", String.valueOf(autoCobweb));

        p.setProperty("keyMenu", String.valueOf(keyMenu));
        p.setProperty("keyAutoDrip", String.valueOf(keyAutoDrip));
        p.setProperty("keyTrapHelper", String.valueOf(keyTrapHelper));

        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "BoykaDILDO config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
