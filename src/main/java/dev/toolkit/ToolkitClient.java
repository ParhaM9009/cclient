package dev.toolkit;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ToolkitClient implements ClientModInitializer {
    public static KeyMapping openKey;               // rebindable in Options > Controls
    public static List<Module> modules;
    public static final List<String> LOG = new ArrayList<>();

    public static void log(String s) { LOG.add(s); if (LOG.size() > 200) LOG.remove(0); }

    @Override public void onInitializeClient() {
        modules = ToolkitModules.create();
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.toolkit.open", GLFW.GLFW_KEY_Z,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("toolkit", "main"))));
        log("Toolkit loaded. Press Z to open.");

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("toolkit", "arraylist"), (g, dt) -> drawHud(g));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.gui.setScreen(new ToolkitScreen());
            if (mc.player == null || mc.level == null || mc.gameMode == null) return;
            for (Module m : modules) {
                if (m.key != -1 && mc.gui.screen() == null) {            // per-module keybinds
                    boolean down = InputConstants.isKeyDown(mc.getWindow(), m.key);
                    if (down && !m.wasDown) m.toggle();
                    m.wasDown = down;
                }
                if (m.enabled) m.tick(mc);
            }
        });
    }

    static void drawHud(net.minecraft.client.gui.GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Module hud = null;
        for (Module m : modules) if (m.name.equals("HUD")) hud = m;
        if (hud == null || !hud.enabled || mc.player == null) return;
        Draw.g = g;
        boolean rgb = hud.settings.get(0).on();
        if (hud.settings.get(1).on()) Draw.text("Toolkit", 4, 4, rgb ? Util.rainbow(0f) : 0xFF55FF55);
        List<Module> on = new ArrayList<>();
        for (Module m : modules) if (m.enabled && m != hud) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> -Draw.width(m.name)));
        int w = mc.getWindow().getGuiScaledWidth(), y = 2;
        for (int i = 0; i < on.size(); i++) {
            String n = on.get(i).name; int tw = Draw.width(n), x = w - tw - 5;
            int col = rgb ? Util.rainbow(i * 0.06f) : 0xFF55FF55;
            Draw.rect(x - 2, y - 1, tw + 7, 11, 0x90000000);
            Draw.rect(w - 2, y - 1, 2, 11, col);
            Draw.text(n, x, y + 1, col);
            y += 11;
        }
    }
}
