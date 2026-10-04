package dev.toolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class Util {
    public static final double REACH = 4.5;
    public static boolean select(Minecraft mc, Item item) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) if (inv.getItem(i).is(item)) { inv.setSelectedSlot(i); return true; }
        return false;
    }
    public static boolean selectAnyExcept(Minecraft mc, Item item) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) if (!inv.getItem(i).is(item)) { inv.setSelectedSlot(i); return true; }
        return false;
    }
    /** Switch to a sword or axe if one is in the hotbar. */
    public static void selectWeapon(Minecraft mc) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            var st = inv.getItem(i);
            if (st.is(ItemTags.SWORDS) || st.is(ItemTags.AXES)) { inv.setSelectedSlot(i); return; }
        }
    }
    public static void attack(Minecraft mc, Entity e) {
        mc.gameMode.attack(mc.player, e);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
    public static void useOn(Minecraft mc, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
    public static boolean inReach(Minecraft mc, BlockPos p) {
        return mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(p)) <= REACH * REACH;
    }
    public static Player nearestPlayer(Minecraft mc, double range) {
        Player best = null; double bd = range * range;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceToSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }
    /** Rainbow colour (ARGB). offset spreads colours along a list. */
    public static int rainbow(float offset) {
        float h = (((System.currentTimeMillis() / 18) % 360) / 360f + offset) % 1f;
        float s = 0.75f, v = 1f; int i = (int) (h * 6); float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s), r, g, b;
        switch (i % 6) { case 0 -> { r = v; g = t; b = p; } case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; } case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; } default -> { r = v; g = p; b = q; } }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
