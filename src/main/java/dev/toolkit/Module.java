package dev.toolkit;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name, desc; public final Category category;
    public boolean enabled;
    public int key = -1;          // GLFW key, -1 = unbound
    boolean wasDown;
    public final List<NumSetting> settings = new ArrayList<>();
    protected Module(String n, Category c, String d) { name = n; category = c; desc = d; }
    protected NumSetting num(String n, double min, double max, double step, double def) {
        NumSetting s = new NumSetting(n, min, max, step, def, false); settings.add(s); return s; }
    protected NumSetting bool(String n, boolean def) {
        NumSetting s = new NumSetting(n, 0, 1, 1, def ? 1 : 0, true); settings.add(s); return s; }
    public void toggle() { enabled = !enabled; if (enabled) onEnable(); else onDisable();
        ToolkitClient.log(name + (enabled ? " enabled" : " disabled")); }
    public void onEnable() {}
    public void onDisable() {}
    public abstract void tick(Minecraft mc);
}
