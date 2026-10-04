package dev.toolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** ONLY file that touches the draw API. */
public final class Draw {
    public static GuiGraphicsExtractor g;
    public static void rect(int x, int y, int w, int h, int argb) { g.fill(x, y, x + w, y + h, argb); }
    public static void text(String s, int x, int y, int argb) { g.text(Minecraft.getInstance().font, s, x, y, argb); }
    public static int width(String s) { return Minecraft.getInstance().font.width(s); }
}
