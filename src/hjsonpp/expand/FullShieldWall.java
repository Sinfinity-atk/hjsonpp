package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;

import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.content.Fx;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import arc.scene.ui.layout.Table;

import java.util.Locale;

/**
 * Full Shield Wall - hybrid of Wall + Shield behavior.
 * Blocks bullets, explosions, lasers, and unit crash damage with shield health,
 * and blocks units with BaseShield-style logic.
 */
public class FullShieldWall extends Wall {

    // tunables via HJSON
    public float shieldRadius = 60f;           // fallback shield radius
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;            // shield regen per second
    public float wallRegenPerSec = 0f;         // wall HP regen per sec
    public float shieldDowntime = 300f;        // frames until shield starts regenerating after breaking
    public String shieldColor = "ffffff";
    public float shieldOpacity = 1f;

    public boolean blockUnits = true;          // enable/disable unit blocking
    public String blockUnitsFrom = "shield";   // "shield", "block", or "both"

    // --- NEW FIELDS ---
    public String shieldShape = "square";      // "circle" or "square"
    public int shieldBlockRadius = 2;          // 0 = off, 1=smaller, 2=same, 3=larger
    public float shieldBlockRadiusAmount = 1f; // scale factor for small/large shield

    public FullShieldWall(String name) {
        super(name);
        group = BlockGroup.walls;
        solid = true;
        update = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.abilities, String.format(Locale.ROOT, "Shield HP: %.0f", shieldHealthCustom));
        stats.add(Stat.abilities, String.format(Locale.ROOT, "Shield Repair %.0f/s", regenPerSec));
        stats.add(Stat.abilities, String.format(Locale.ROOT, "Shield Downtime %.1fs", shieldDowntime / 60f));
        if (wallRegenPerSec > 0f) {
            stats.add(Stat.abilities, String.format(Locale.ROOT, "Wall Repair %.0f/s", wallRegenPerSec));
        }
    }

    public class FullShieldWallBuild extends WallBuild {
        public float shield = shieldHealthCustom;
        public Color colorCached;

        // downtime tracker (frames)
        public float cooldownTimer = 0f;

        // recharge state: when true, shield remains at 0 and will be rebuilt after rechargeProgress reaches shieldHealthCustom
        public boolean recharging = false;
        public float rechargeProgress = 0f;

        @Override
        public void updateTile() {
            float r = computeShieldRadius();

            // --- shield/downtime/recharge logic ---
            if (shield <= 0f) {
                // If not yet in recharging state, count downtime first
                if (!recharging) {
                    cooldownTimer += Time.delta;
                    if (cooldownTimer >= shieldDowntime) {
                        // Enter recharge phase
                        recharging = true;
                        rechargeProgress = 0f;
                        cooldownTimer = 0f;
                    }
                } else {
                    // We are in Recharge Phase: accumulate recharge progress using regenPerSec
                    // regenPerSec is in HP per second; Time.delta is in frames, so divide by 60
                    rechargeProgress += regenPerSec * Time.delta / 60f;

                    if (rechargeProgress >= shieldHealthCustom) {
                        // recharge complete — restore full shield
                        shield = shieldHealthCustom;
                        recharging = false;
                        rechargeProgress = 0f;
                        // optional effect: currently skipping unknown Fx to avoid compile issues
                        // Fx.shieldRegen.at(x, y, r, team.color); // if such Fx exists
                    }
                }
            } else {
                // shield > 0: normal passive regen to top up small losses
                if (shield < shieldHealthCustom) {
                    shield += regenPerSec * Time.delta / 60f;
                    if (shield > shieldHealthCustom) shield = shieldHealthCustom;
                }
                // ensure downtime/recharge state cleared
                cooldownTimer = 0f;
                recharging = false;
                rechargeProgress = 0f;
            }

            // wall HP regen
            if (wallRegenPerSec > 0 && health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // bullet blocking (enemy only, consumes shield health)
            if (r > 0f && shield > 0f) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if (b.team == team) return; // ignore friendly bullets
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if (dx * dx + dy * dy > r * r) return;

                    // absorb + reduce shield health
                    b.remove();
                    shield -= b.damage;

                    if (shield <= 0f) {
                        shield = 0f;
                        // start downtime on next update tick (cooldownTimer accumulates)
                        Fx.shieldBreak.at(x, y, r, team.color);
                    }
                });
            }

            // --- unit blocking ---
            if (blockUnits) {
                if ("shield".equalsIgnoreCase(blockUnitsFrom) || "both".equalsIgnoreCase(blockUnitsFrom)) {
                    if (r > 0f) {
                        Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, (Unit unit) -> {
                            float overlapDst = (unit.hitSize / 2f + r) - unit.dst(this);

                            if (overlapDst > 0) {
                                if (overlapDst > unit.hitSize * 1.5f) {
                                    unit.kill();
                                } else {
                                    unit.vel.setZero();
                                    unit.move(Tmp.v1.set(unit).sub(this).setLength(overlapDst + 0.01f));

                                    if (Mathf.chanceDelta(0.12f * Time.delta)) {
                                        Fx.circleColorSpark.at(unit.x, unit.y, team.color);
                                    }
                                }
                            }
                        });
                    }
                }
                if ("block".equalsIgnoreCase(blockUnitsFrom) || "both".equalsIgnoreCase(blockUnitsFrom)) {
                    float br = block.size * 8f;
                    Units.nearbyEnemies(team, x - br, y - br, br * 2f, br * 2f, (Unit unit) -> {
                        float overlapDst = (unit.hitSize / 2f + br) - unit.dst(this);

                        if (overlapDst > 0) {
                            if (overlapDst > unit.hitSize * 1.5f) {
                                unit.kill();
                            } else {
                                unit.vel.setZero();
                                unit.move(Tmp.v1.set(unit).sub(this).setLength(overlapDst + 0.01f));

                                if (Mathf.chanceDelta(0.12f * Time.delta)) {
                                    Fx.circleColorSpark.at(unit.x, unit.y, team.color);
                                }
                            }
                        }
                    });
                }
            }
        }

        // --- absorb explosions, lasers, direct block-target damage ---
        @Override
        public void damage(float damage){
            if(shield > 0f){
                shield -= damage;
                if(shield <= 0f){
                    shield = 0f;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                    // downtime/recharge progression will start in updateTile()
                }
                // do NOT pass damage to block while shield is up
            }else{
                super.damage(damage);
            }
        }

        @Override
        public void damagePierce(float damage){
            if(shield > 0f){
                shield -= damage;
                if(shield <= 0f){
                    shield = 0f;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                    // downtime/recharge progression will start in updateTile()
                }
                // still absorbed by shield
            }else{
                super.damagePierce(damage);
            }
        }

        // --- compute shield radius based on block size + shieldBlockRadius ---
        private float computeShieldRadius() {
            float base = block.size * 8f; // block size in world units
            if (shieldBlockRadius == 1) {
                return base * shieldBlockRadiusAmount; // smaller
            } else if (shieldBlockRadius == 2) {
                return base; // exactly block size
            } else if (shieldBlockRadius == 3) {
                return base * (1f + shieldBlockRadiusAmount); // larger
            } else {
                return shieldRadius; // fallback
            }
        }

        @Override
        public void draw() {
            super.draw();

            float r = computeShieldRadius();
            if (r <= 0f || shield <= 0f) return;

            if (colorCached == null) {
                try {
                    colorCached = Color.valueOf(FullShieldWall.this.shieldColor);
                } catch (Exception ex) {
                    colorCached = Color.white;
                }
            }

            Draw.z(Layer.shields);
            Draw.color(colorCached, shieldOpacity);

            if ("square".equalsIgnoreCase(shieldShape)) {
                Fill.square(x, y, r);
            } else {
                Fill.circle(x, y, r);
            }

            Draw.reset();
        }

        // --- Info Bars ---
        @Override
        public void displayBars(Table table) {
            super.displayBars(table);

            // Shield HP
            table.add(new Bar(
                    () -> "Shield HP: " + (int) shield + " / " + (int) shieldHealthCustom,
                    () -> Color.valueOf("ffff77"),
                    () -> shield / shieldHealthCustom
            )).row();

            // Shield Status / Downtime / Recharge
            table.add(new Bar(
                    () -> {
                        if (shield > 0f) {
                            return "Shield Active";
                        } else {
                            if (recharging) {
                                float pct = Math.min(1f, rechargeProgress / shieldHealthCustom);
                                return String.format(Locale.ROOT, "Shield Recharging (%.0f%%)", pct * 100f);
                            } else {
                                float timeLeft = Math.max(0f, shieldDowntime - cooldownTimer) / 60f;
                                return String.format(Locale.ROOT, "Shield Downtime (%.1fs)", timeLeft);
                            }
                        }
                    },
                    () -> shield > 0f ? Color.valueOf("77ff77") : Color.valueOf("ffff77"),
                    () -> {
                        if (shield > 0f) return 1f;
                        if (recharging) return Math.min(1f, rechargeProgress / shieldHealthCustom);
                        return Math.min(1f, cooldownTimer / shieldDowntime);
                    }
            )).row();

            // Shield Repair Speed
            table.add(new Bar(
                    () -> "Shield Repair " + (int) regenPerSec + "/s",
                    () -> Color.valueOf("77ff77"),
                    () -> 1f
            )).row();

            // Wall Repair Speed
            if (wallRegenPerSec > 0f) {
                table.add(new Bar(
                        () -> "Wall Repair " + (int) wallRegenPerSec + "/s",
                        () -> Color.valueOf("77ff77"),
                        () -> 1f
                )).row();
            }
        }
    }
}
