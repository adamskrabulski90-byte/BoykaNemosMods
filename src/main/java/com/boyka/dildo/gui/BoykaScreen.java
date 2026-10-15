package com.boyka.dildo.gui;

import com.boyka.dildo.BoykaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * Panel ustawień BoykaDILDO.
 * Styl: ciemnoniebieskie tło, jasnoniebieskie akcenty, napis "BOYKA" na środku u góry.
 */
public class BoykaScreen extends Screen {

    private static final int COLOR_BG            = 0xE6081028; // ciemny granat, lekko przezroczysty
    private static final int COLOR_PANEL         = 0xF00C1A3D; // panel niebieski
    private static final int COLOR_PANEL_BORDER  = 0xFF2E6FEA; // jasnoniebieska ramka
    private static final int COLOR_TITLE         = 0xFF4FA8FF; // jasny niebieski napis
    private static final int COLOR_TEXT          = 0xFFBFD9FF;
    private static final int COLOR_ACCENT        = 0xFF1C4FBF;

    private int listeningForKeyButtonIndex = -1; // -1 = brak nasłuchu

    private ButtonWidget btnMenuKey, btnDripKey, btnTrapKey;

    public BoykaScreen() {
        super(Text.literal("BOYKA"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 90;

        int colX = centerX - 140;
        int colW = 280;

        // --- Checkboxy kategorii ---
        int y = panelTop + 40;
        int step = 18;

        addCheckbox(colX, y, colW, "Minecart (normal)",
                () -> BoykaConfig.destroyMinecartNormal,
                v -> BoykaConfig.destroyMinecartNormal = v);
        y += step;

        addCheckbox(colX, y, colW, "Minecart + Chest",
                () -> BoykaConfig.destroyMinecartChest,
                v -> BoykaConfig.destroyMinecartChest = v);
        y += step;

        addCheckbox(colX, y, colW, "Minecart + Furnace",
                () -> BoykaConfig.destroyMinecartFurnace,
                v -> BoykaConfig.destroyMinecartFurnace = v);
        y += step;

        addCheckbox(colX, y, colW, "Minecart + Hopper",
                () -> BoykaConfig.destroyMinecartHopper,
                v -> BoykaConfig.destroyMinecartHopper = v);
        y += step;

        addCheckbox(colX, y, colW, "Minecart + TNT",
                () -> BoykaConfig.destroyMinecartTnt,
                v -> BoykaConfig.destroyMinecartTnt = v);
        y += step;

        addCheckbox(colX, y, colW, "Banners / Item Frames",
                () -> BoykaConfig.destroyBanners,
                v -> BoykaConfig.destroyBanners = v);
        y += step;

        addCheckbox(colX, y, colW, "Paintings",
                () -> BoykaConfig.destroyPaintings,
                v -> BoykaConfig.destroyPaintings = v);
        y += step;

        addCheckbox(colX, y, colW, "Auto Cobweb",
                () -> BoykaConfig.autoCobweb,
                v -> BoykaConfig.autoCobweb = v);
        y += step + 8;

        // --- Suwaki (jako przyciski +/-) ---
        y = addStepperRow(colX, y, colW, "Trap delay",
                () -> String.valueOf(BoykaConfig.trapHelperDelayTicks),
                () -> { if (BoykaConfig.trapHelperDelayTicks < 5) BoykaConfig.trapHelperDelayTicks++; },
                () -> { if (BoykaConfig.trapHelperDelayTicks > 0) BoykaConfig.trapHelperDelayTicks--; });

        y = addStepperRow(colX, y, colW, "Trap reach",
                () -> String.format("%.0f", BoykaConfig.trapHelperReach),
                () -> { if (BoykaConfig.trapHelperReach < 3) BoykaConfig.trapHelperReach++; },
                () -> { if (BoykaConfig.trapHelperReach > 1) BoykaConfig.trapHelperReach--; });

        y = addStepperRow(colX, y, colW, "Drip delay",
                () -> String.valueOf(BoykaConfig.autoDripDelayTicks),
                () -> { if (BoykaConfig.autoDripDelayTicks < 20) BoykaConfig.autoDripDelayTicks++; },
                () -> { if (BoykaConfig.autoDripDelayTicks > 0) BoykaConfig.autoDripDelayTicks--; });

        y += 6;

        // --- Bindy ---
        btnMenuKey = addKeyBindButton(colX, y, colW, "Menu key",
                BoykaConfig.keyMenu, 0);
        y += step + 2;

        btnDripKey = addKeyBindButton(colX, y, colW, "Auto Drip key",
                BoykaConfig.keyAutoDrip, 1);
        y += step + 2;

        btnTrapKey = addKeyBindButton(colX, y, colW, "Trap Helper key",
                BoykaConfig.keyTrapHelper, 2);
        y += step + 10;

        // --- Zamknij ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> {
            BoykaConfig.save();
            this.close();
        }).dimensions(colX, y, colW, 20).build());
    }

    private ButtonWidget addKeyBindButton(int x, int y, int w, String label, int currentKey, int index) {
        String keyName = InputUtil.fromKeyCode(currentKey, 0).getLocalizedText().getString();
        ButtonWidget btn = ButtonWidget.builder(
                Text.literal(label + ": " + keyName.toUpperCase()),
                b -> {
                    listeningForKeyButtonIndex = index;
                    b.setMessage(Text.literal(label + ": > wciśnij klawisz <"));
                }
        ).dimensions(x, y, w, 20).build();
        this.addDrawableChild(btn);
        return btn;
    }

    private void addCheckbox(int x, int y, int w, String label,
                              java.util.function.Supplier<Boolean> getter,
                              java.util.function.Consumer<Boolean> setter) {
        ButtonWidget btn = ButtonWidget.builder(
                Text.literal((getter.get() ? "[x] " : "[ ] ") + label),
                b -> {
                    boolean newVal = !getter.get();
                    setter.accept(newVal);
                    b.setMessage(Text.literal((newVal ? "[x] " : "[ ] ") + label));
                }
        ).dimensions(x, y, w, 16).build();
        this.addDrawableChild(btn);
    }

    private int addStepperRow(int x, int y, int w, String label,
                               java.util.function.Supplier<String> valueGetter,
                               Runnable increment, Runnable decrement) {
        int btnW = 20;

        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> decrement.run())
                .dimensions(x, y, btnW, 18).build();
        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> increment.run())
                .dimensions(x + w - btnW, y, btnW, 18).build();

        this.addDrawableChild(minus);
        this.addDrawableChild(plus);

        // etykieta rysowana ręcznie w render()
        this.labels.add(new Label(x + btnW + 4, y + 5, x + w - btnW - 4, label, valueGetter));

        return y + 20;
    }

    private final java.util.List<Label> labels = new java.util.ArrayList<>();

    private record Label(int x, int y, int rightEdge, String text,
                          java.util.function.Supplier<String> valueGetter) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKeyButtonIndex != -1) {
            switch (listeningForKeyButtonIndex) {
                case 0 -> BoykaConfig.keyMenu = keyCode;
                case 1 -> BoykaConfig.keyAutoDrip = keyCode;
                case 2 -> BoykaConfig.keyTrapHelper = keyCode;
            }
            listeningForKeyButtonIndex = -1;
            this.clearChildren();
            this.init();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Tło całego ekranu
        context.fill(0, 0, this.width, this.height, COLOR_BG);

        int panelW = 320;
        int panelH = 330;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;

        // Panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(context, panelX, panelY, panelW, panelH, COLOR_PANEL_BORDER);

        // Tytuł na środku
        Text title = Text.literal("BOYKA");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title,
                this.width / 2 - titleWidth / 2, panelY + 10, COLOR_TITLE, true);

        // Podtytuł
        Text sub = Text.literal("D I L D O   M O D");
        int subWidth = this.textRenderer.getWidth(sub);
        context.drawText(this.textRenderer, sub,
                this.width / 2 - subWidth / 2, panelY + 24, COLOR_TEXT, false);

        super.render(context, mouseX, mouseY, delta);

        // Etykiety steperów (rysowane po przyciskach, żeby nie kolidowały)
        for (Label l : labels) {
            String full = l.text() + ": " + l.valueGetter().get();
            context.drawText(this.textRenderer, Text.literal(full), l.x(), l.y(), COLOR_TEXT, false);
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        BoykaConfig.save();
        super.close();
    }
}
