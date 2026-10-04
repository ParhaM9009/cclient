package dev.toolkit;
/** One setting: a number slider (click < >) or an On/Off switch. */
public class NumSetting {
    public final String name; public final double min, max, step; public double value; public final boolean bool;
    public NumSetting(String n, double min, double max, double step, double def, boolean bool) {
        name = n; this.min = min; this.max = max; this.step = step; value = def; this.bool = bool;
    }
    public int i() { return (int) Math.round(value); }
    public double d() { return value; }
    public boolean on() { return value >= 0.5; }
    public void add(int dir) {
        if (bool) { value = on() ? 0 : 1; return; }
        double v = Math.round((value + dir * step) / step) * step;
        value = Math.max(min, Math.min(max, v));
    }
    public String display() {
        if (bool) return on() ? "On" : "Off";
        return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format("%.1f", value);
    }
}
