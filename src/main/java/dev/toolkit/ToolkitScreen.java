package dev.toolkit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** LiquidBounce-style ClickGUI: one panel per category.
 *  Left-click module = toggle | Right-click = open settings | Middle-click = set keybind.
 *  In settings: click left half = lower, right half = raise (On/Off switches toggle). */
public class ToolkitScreen extends Screen {
    static final int PW = 100, GAP = 4, HEAD = 16, ROW = 14, SROW = 12, TOP = 22;
    static final int PANEL = 0xF01A1F1C, SET = 0xF0121513, ON = 0xF0243326, TEXT = 0xFFE0E0E0, DIM = 0xFF8A938C, GREEN = 0xFF55FF55, YEL = 0xFFFFD34D;
    Module expanded, binding;

    public ToolkitScreen() { super(Component.literal("Toolkit")); }

    int panelX(int idx) {
        int n = Category.values().length, total = n * PW + (n - 1) * GAP;
        return Math.max(2, (width - total) / 2) + idx * (PW + GAP);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        Draw.g = g;
        Draw.rect(0, 0, width, height, 0x66000000);
        int idx = 0;
        for (Category c : Category.values()) {
            int x = panelX(idx), y = TOP;
            Draw.rect(x + 2, y + 2, PW, HEAD, 0x55000000);
            Draw.rect(x, y, PW, HEAD, 0xF00E110F);
            Draw.rect(x, y + HEAD - 2, PW, 2, Util.rainbow(idx * 0.15f));
            Draw.text(c.title, x + 6, y + 4, TEXT);
            y += HEAD;
            for (Module m : ToolkitClient.modules) {
                if (m.category != c) continue;
                Draw.rect(x, y, PW, ROW, m.enabled ? ON : PANEL);
                if (m.enabled) Draw.rect(x, y, 2, ROW, GREEN);
                Draw.text(m.name, x + 6, y + 3, m.enabled ? GREEN : TEXT);
                String tag = binding == m ? "[...]" : m.key != -1 ? "[" + ToolkitScreen.keyName(m.key) + "]" : (m.settings.isEmpty() ? "" : (expanded == m ? "-" : "+"));
                Draw.text(tag, x + PW - Draw.width(tag) - 4, y + 3, binding == m ? YEL : DIM);
                y += ROW;
                if (expanded == m) for (NumSetting s : m.settings) {
                    Draw.rect(x, y, PW, SROW, SET);
                    Draw.text(s.name, x + 8, y + 2, DIM);
                    String v = s.bool ? s.display() : "< " + s.display() + " >";
                    Draw.text(v, x + PW - Draw.width(v) - 4, y + 2, s.bool && s.on() ? GREEN : TEXT);
                    y += SROW;
                }
            }
            idx++;
        }
        Draw.text("Left: toggle   Right: settings   Middle: keybind   (Z closes)", 6, height - 12, DIM);
    }

    static String keyName(int k) {
        return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(k).getDisplayName().getString();
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y(); int b = e.button(), idx = 0;
        for (Category c : Category.values()) {
            int x = panelX(idx++), y = TOP + HEAD;
            for (Module m : ToolkitClient.modules) {
                if (m.category != c) continue;
                if (mx >= x && mx < x + PW && my >= y && my < y + ROW) {
                    if (b == 0) m.toggle();
                    else if (b == 1) expanded = expanded == m ? null : m;
                    else if (b == 2) binding = m;
                    return true;
                }
                y += ROW;
                if (expanded == m) for (NumSetting s : m.settings) {
                    if (mx >= x && mx < x + PW && my >= y && my < y + SROW) {
                        s.add(mx < x + PW / 2.0 ? -1 : 1);
                        return true;
                    }
                    y += SROW;
                }
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (binding != null) {
            int k = e.key();
            if (k == GLFW.GLFW_KEY_ESCAPE) { /* cancel */ }
            else if (k == GLFW.GLFW_KEY_BACKSPACE) { binding.key = -1; ToolkitClient.log(binding.name + " unbound"); }
            else { binding.key = k; ToolkitClient.log(binding.name + " bound to " + keyName(k)); }
            binding = null; return true;
        }
        if (ToolkitClient.openKey.matches(e)) { onClose(); return true; }
        return super.keyPressed(e);
    }

    @Override public boolean isPauseScreen() { return false; }
}
