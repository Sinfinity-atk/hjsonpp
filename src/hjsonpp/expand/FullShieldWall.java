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
 * Blocks bullets with shield health, and units with BaseShield-style logic.
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
        public float buildup = 0f;      // how much damage shield has absorbed
        public boolean broken = false;  // true if shield is down
        public Color colorCached;

        @Override
        public void updateTile() {
            // Shield recharge logic
            if (buildup > 0f) {
                float cooldown = broken ? shieldDowntime / 60f : 1f; // recharge slower when broken
                buildup -= regenPerSec * Time.delta / 60f / cooldown;
                if (buildup < 0f) buildup = 0f;
            }

            if (broken && buildup <= 0f) {
                broken = false;
                Fx.absorb.at(x, y, computeShieldRadius(), team.color);
            }

            float r = computeShieldRadius();

            // bullet blocking (enemy only, uses buildup system)
            if (r > 0f && !broken) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if (b.team == team) return;
                    float dx = b.x - x, dy = b.y - y;
                    if (dx * dx + dy * dy > r * r) return;

                    b.remove();
                    buildup += b.damage;
                    if (buildup >= shieldHealthCustom) {
                        broken = true;
                        buildup = shieldHealthCustom; // cap
                        Fx.shieldBreak.at(x, y, r, team.color);
                    }
                });
            }

            // wall HP regen
            if (wallRegenPerSec > 0 && health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // --- unit blocking (unchanged) ---
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

        // Shield damage handling (buildup system)
        @Override
        public void damage(float damage){
            if (!broken) {
                buildup += damage;
                if (buildup >= shieldHealthCustom) {
                    broken = true;
                    buildup = shieldHealthCustom;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                }
            } else {
                // during downtime or recharge → wall takes damage
                super.damage(damage);
            }
        }

        @Override
        public void damagePierce(float damage){
            if (!broken) {
                buildup += damage;
                if (buildup >= shieldHealthCustom) {
                    broken = true;
                    buildup = shieldHealthCustom;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                }
            } else {
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
            if (r <= 0f || broken) return;

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

            // Shield HP Bar (reverse: full when not broken)
            table.add(new Bar(
                () -> "Shield HP: " + (int)(shieldHealthCustom - buildup) + " / " + (int)shieldHealthCustom,
                () -> broken ? Color.valueOf("ffff77") : Color.valueOf("77ff77"),
                () -> broken ? 0f : 1f - buildup / shieldHealthCustom
            )).row();

            // Shield Status
            table.add(new Bar(
                () -> broken ? "Shield Recharging..." : "Shield Active",
                () -> broken ? Color.valueOf("ffff77") : Color.valueOf("77ff77"),
                () -> broken ? buildup / shieldHealthCustom : 1f
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
