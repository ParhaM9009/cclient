package dev.toolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** All modules, grouped by category. */
public final class ToolkitModules {
    public static List<Module> create() {
        List<Module> l = new ArrayList<>();
        l.add(new KillAura()); l.add(new CrystalAura(false)); l.add(new CrystalAura(true));
        l.add(new AnchorAura()); l.add(new AutoTotem());
        l.add(new PlayerEsp()); l.add(new Fullbright());
        l.add(new SpinBot());
        Module hud = new Hud(); hud.enabled = true; l.add(hud);
        return l;
    }

    // ---------------------------------------------------------------- COMBAT
    static class KillAura extends Module {
        final NumSetting range = num("Range", 3, 6, 0.1, 4.5), players = bool("Players", true),
                         mobs = bool("Mobs", false), cooldown = bool("Cooldown", true);
        KillAura() { super("Kill Aura", Category.COMBAT, "Attacks targets in reach"); }
        public void tick(Minecraft mc) {
            if (cooldown.on() && mc.player.getAttackStrengthScale(0.5f) < 0.95f) return;
            double r = range.d(), bd = r * r; LivingEntity best = null;
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
                if (le instanceof Player ? !players.on() : !mobs.on()) continue;
                double d = mc.player.distanceToSqr(le);
                if (d < bd) { bd = d; best = le; }
            }
            if (best != null) { Util.selectWeapon(mc); Util.attack(mc, best); }
        }
    }

    /** Silent: targets the nearest player, never uses your camera/crosshair. Can also hit the enemy. */
    static class CrystalAura extends Module {
        final boolean spam; int delay;
        final NumSetting delaySet, burst, target, hitEnemy, obsidian;
        CrystalAura(boolean spam) {
            super(spam ? "Crystal Spam" : "Crystal Aura", Category.COMBAT,
                  spam ? "Fast obsidian + crystal + hit" : "Silent obsidian + crystal + hit");
            this.spam = spam;
            delaySet = num("Delay", 0, 10, 1, spam ? 0 : 1);   // ticks between rounds
            burst    = num("Burst", 1, 6, 1, spam ? 3 : 1);    // actions per round
            target   = num("Target", 3, 12, 1, 8);             // find enemy within X blocks
            hitEnemy = bool("HitEnemy", true);
            obsidian = bool("Obsidian", true);
        }
        public void tick(Minecraft mc) {
            if (delay-- > 0) return;
            for (int i = 0; i < burst.i(); i++) step(mc);
            delay = delaySet.i();
        }
        void step(Minecraft mc) {
            double bd = Util.REACH * Util.REACH;
            List<EndCrystal> crystals = new ArrayList<>();
            for (var e : mc.level.entitiesForRendering())
                if (e instanceof EndCrystal ec && mc.player.distanceToSqr(ec) < bd) crystals.add(ec);
            if (!crystals.isEmpty()) {                                   // 1) break crystals
                if (spam) for (EndCrystal c : crystals) Util.attack(mc, c); else Util.attack(mc, crystals.get(0));
                return;
            }
            Player t = Util.nearestPlayer(mc, target.d());
            if (t == null) return;
            if (hitEnemy.on() && mc.player.distanceToSqr(t) <= bd              // 2) hit the enemy
                && mc.player.getAttackStrengthScale(0.5f) >= 0.95f) { Util.selectWeapon(mc); Util.attack(mc, t); return; }
            BlockPos base = null; double best = Double.MAX_VALUE; BlockPos o = t.blockPosition();
            for (int x = -3; x <= 3; x++) for (int y = -2; y <= 0; y++) for (int z = -3; z <= 3; z++) {
                BlockPos p = o.offset(x, y, z);
                if (!valid(mc, p) || !Util.inReach(mc, p)) continue;
                double d = t.distanceToSqr(p.getX() + .5, p.getY() + 1, p.getZ() + .5);
                if (d < best) { best = d; base = p; }
            }
            if (base != null) {                                          // 3) crystal on a base
                if (Util.select(mc, Items.END_CRYSTAL)) Util.useOn(mc, base, Direction.UP);
                return;
            }
            if (!obsidian.on()) return;                                  // 4) no base -> obsidian
            BlockPos spot = null; double bs = Double.MAX_VALUE;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = o.offset(x, 0, z);
                if (!mc.level.getBlockState(p).isAir() || !mc.level.getBlockState(p.above()).isAir()
                    || !mc.level.getBlockState(p.above(2)).isAir() || !mc.level.getBlockState(p.below()).isSolid()
                    || !Util.inReach(mc, p)) continue;
                double d = t.distanceToSqr(p.getX() + .5, p.getY() + 1, p.getZ() + .5);
                if (d < bs) { bs = d; spot = p; }
            }
            if (spot != null && Util.select(mc, Items.OBSIDIAN)) Util.useOn(mc, spot.below(), Direction.UP);
        }
        boolean valid(Minecraft mc, BlockPos p) {
            BlockState s = mc.level.getBlockState(p);
            return (s.is(Blocks.OBSIDIAN) || s.is(Blocks.BEDROCK))
                && mc.level.getBlockState(p.above()).isAir() && mc.level.getBlockState(p.above(2)).isAir();
        }
    }

    /** Hold RMB: look at block -> places anchor; look at anchor -> glowstone -> detonate. Overworld/End only. */
    static class AnchorAura extends Module {
        BlockPos anchor; int delay, miss;
        final NumSetting hold = bool("HoldRMB", true), delaySet = num("Delay", 0, 5, 1, 1);
        AnchorAura() { super("Anchor Aura", Category.COMBAT, "Hold RMB: place, charge, detonate"); }
        public void onDisable() { anchor = null; }
        public void tick(Minecraft mc) {
            if (delay-- > 0) return;
            if (hold.on() && !mc.options.keyUse.isDown()) return;
            if (anchor != null && !mc.level.getBlockState(anchor).is(Blocks.RESPAWN_ANCHOR)) {
                if (++miss > 6) anchor = null;
                return;
            }
            miss = 0;
            if (anchor == null) {
                if (!(mc.hitResult instanceof BlockHitResult h) || h.getType() != HitResult.Type.BLOCK) return;
                BlockPos hp = h.getBlockPos();
                if (mc.level.getBlockState(hp).is(Blocks.RESPAWN_ANCHOR)) anchor = hp;
                else {
                    BlockPos np = hp.relative(h.getDirection());
                    if (mc.level.getBlockState(np).isAir() && Util.select(mc, Items.RESPAWN_ANCHOR)) {
                        Util.useOn(mc, hp, h.getDirection()); anchor = np; delay = delaySet.i();
                    }
                    return;
                }
            }
            BlockState st = mc.level.getBlockState(anchor);
            if (st.getValue(RespawnAnchorBlock.CHARGE) == 0) {
                if (Util.select(mc, Items.GLOWSTONE)) { Util.useOn(mc, anchor, Direction.UP); delay = delaySet.i(); }
            } else if (Util.selectAnyExcept(mc, Items.GLOWSTONE)) {
                Util.useOn(mc, anchor, Direction.UP); anchor = null; delay = delaySet.i() + 1;
            }
        }
    }

    /** Instant: keeps a totem in the offhand every tick, re-equips right after a pop. */
    static class AutoTotem extends Module {
        int delay;
        final NumSetting delaySet = num("Delay", 0, 10, 1, 0), health = num("Health", 0, 20, 1, 0),
                         inv = bool("Inventory", true);
        AutoTotem() { super("Auto Totem", Category.COMBAT, "Instant offhand totem"); }
        public void tick(Minecraft mc) {
            if (delay-- > 0) return;
            if (mc.player.containerMenu != mc.player.inventoryMenu) return;      // never while a chest etc. is open
            if (health.i() > 0 && mc.player.getHealth() > health.i()) return;    // 0 = always
            if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
            var menu = mc.player.inventoryMenu;
            for (int i = 36; i <= 44; i++)                                        // hotbar first
                if (menu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) { swap(mc, menu, i); return; }
            if (inv.on())
                for (int i = 9; i <= 35; i++)                                     // then main inventory
                    if (menu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) { swap(mc, menu, i); return; }
        }
        void swap(Minecraft mc, net.minecraft.world.inventory.InventoryMenu menu, int slot) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 40, ClickType.SWAP, mc.player); // 40 = offhand
            delay = delaySet.i();
        }
    }

    // ---------------------------------------------------------------- RENDER
    /** Glow outline through walls around players (client-side only). */
    static class PlayerEsp extends Module {
        final Set<Entity> marked = new HashSet<>();
        final NumSetting players = bool("Players", true), mobs = bool("Mobs", false);
        PlayerEsp() { super("Player ESP", Category.RENDER, "Glow outline around enemies"); }
        public void tick(Minecraft mc) {
            marked.removeIf(Entity::isRemoved);
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity) || e == mc.player) continue;
                boolean want = e instanceof Player ? players.on() : mobs.on();
                if (want) { e.setGlowingTag(true); marked.add(e); }
                else if (marked.remove(e)) e.setGlowingTag(false);
            }
        }
        public void onDisable() { for (Entity e : marked) e.setGlowingTag(false); marked.clear(); }
    }

    static class Fullbright extends Module {
        Fullbright() { super("Full Bright", Category.RENDER, "Client-side night vision"); }
        public void tick(Minecraft mc) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
        }
        public void onDisable() { var p = Minecraft.getInstance().player; if (p != null) p.removeEffect(MobEffects.NIGHT_VISION); }
    }

    // ---------------------------------------------------------------- PLAYER
    /** Silent spin: only other players see you spin. Your own camera never moves. */
    static class SpinBot extends Module {
        float yaw;
        final NumSetting speed = num("Speed", 1, 90, 1, 30), pitch = num("Pitch", -90, 90, 5, 90);
        SpinBot() { super("Spin Bot", Category.PLAYER, "Silent spin (others see it)"); }
        public void tick(Minecraft mc) {
            yaw = (yaw + (float) speed.d()) % 360f;
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, (float) pitch.d(),
                mc.player.onGround(), mc.player.horizontalCollision));
        }
        public void onDisable() {
            var p = Minecraft.getInstance().player;
            if (p != null) p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(),
                p.onGround(), p.horizontalCollision));
        }
    }

    // ---------------------------------------------------------------- CLIENT
    /** Rainbow module list (top-right). Drawn from ToolkitClient. */
    static class Hud extends Module {
        final NumSetting rainbow = bool("Rainbow", true), watermark = bool("Watermark", true);
        Hud() { super("HUD", Category.CLIENT, "RGB module list top-right"); }
        public void tick(Minecraft mc) {}
    }
}
