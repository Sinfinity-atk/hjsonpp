package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;

import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

/**
 * Full Shield Wall - hybrid of Wall + Shield behavior.
 */
public class FullShieldWall extends Wall {

    // tunables via HJSON
    public float shieldRadius = 60f;           // fallback shield radius
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;            // shield regen per second
    public float wallRegenPerSec = 0f;         // wall HP regen per sec
    public String shieldColor = "7f7fff";
    public float shieldOpacity = 0.3f;

    public boolean blockUnits = true;          // block units
    public boolean pushUnits = false;          // push or stop units

    // --- NEW FIELDS ---
    public String shieldShape = "circle";      // "circle" or "square"
    public int shieldBlockRadius = 0;          // 0 = off, 1=smaller, 2=same, 3=larger
    public float shieldBlockRadiusAmount = 0.5f; // scale factor for small/large shield

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

            // bullet blocking (only enemy projectiles)
            if (r > 0f) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if (b.team == team) return; // ignore friendly bullets
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if (dx * dx + dy * dy > r * r) return;
                    b.remove(); // absorb enemy bullet
                });
            }

            // unit blocking
            if (blockUnits && r > 0f) {
                Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, (Unit u) -> {
                    if (u.dead()) return;

                    float dx = u.x - x;
                    float dy = u.y - y;
                    float dist2 = dx * dx + dy * dy;

                    if (dist2 < r * r) {
                        float dist = Mathf.sqrt(dist2);
                        if (dist < 0.001f) dist = 0.001f;

                        if (pushUnits) {
                            // push back mode
                            float push = 2f;
                            u.vel.add(dx / dist * push * Time.delta, dy / dist * push * Time.delta);
                        } else {
                            // stop mode (hard edge)
                            float edgeX = x + dx / dist * r;
                            float edgeY = y + dy / dist * r;
                            u.set(edgeX, edgeY);
                            u.vel.setZero();
                        }
                    }
                });
            }
        }

        // --- NEW: compute shield radius based on block size + shieldBlockRadius ---
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