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
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

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

    public class FullShieldWallBuild extends WallBuild {
        public float shield = shieldHealthCustom;
        public Color colorCached;

        @Override
        public void updateTile() {
            // shield regen
            if (shield < shieldHealthCustom) {
                shield += regenPerSec * Time.delta / 60f;
                if (shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            // wall HP regen
            if (wallRegenPerSec > 0 && health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            float r = computeShieldRadius();

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
            if (r <= 0f) return;

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
    }
}