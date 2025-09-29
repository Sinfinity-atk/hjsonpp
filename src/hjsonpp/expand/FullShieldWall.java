package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;

import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

/**
 * Shield Wall with tunable properties.
 */
public class FullShieldWall extends Wall {

    // --- tunables ---
    public float shieldRadius = 60f;       // shield radius
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;        // shield regen per second
    public float wallRegenPerSec = 0f;     // new: wall HP regen per sec
    public String shieldColor = "7f7fff";
    public float shieldOpacity = 0.3f;     // new: opacity of shield circle
    public boolean useDefaultShieldTexture = false; // new: draw wave style like vanilla projector

    public boolean absorbLasers = true;
    public boolean deflectBullets = true;
    public boolean blockUnits = true;

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
            // regen shield
            if (shield < shieldHealthCustom) {
                shield += regenPerSec * Time.delta / 60f;
                if (shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            // regen wall HP
            if (wallRegenPerSec > 0 && health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            float r = shieldRadius;

            // bullet interaction
            if (r > 0f) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if (b.team == team) return;
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if (dx * dx + dy * dy > r * r) return;

                    if (deflectBullets && Mathf.chanceDelta(100f)) {
                        b.vel.setAngle(b.vel.angle() + 180f); // bounce back
                    } else {
                        try {
                            b.remove();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                });
            }

            // unit blocking
            if (blockUnits && r > 0f) {
                Groups.unit.intersect(x - r, y - r, r * 2f, r * 2f, (Unit u) -> {
                    if (u.team == team || u.dead()) return;
                    float dx = u.x - x;
                    float dy = u.y - y;
                    float dist2 = dx * dx + dy * dy;
                    if (dist2 < r * r) {
                        float dist = Mathf.sqrt(dist2);
                        if (dist < 1f) dist = 1f;
                        float push = (r - dist) * 0.5f;
                        u.vel.add(dx / dist * push * Time.delta / 6f, dy / dist * push * Time.delta / 6f);
                    }
                });
            }
        }

        public boolean absorbLasers() {
            return FullShieldWall.this.absorbLasers;
        }

        public boolean deflectBullets() {
            return FullShieldWall.this.deflectBullets;
        }

        @Override
        public void draw() {
            super.draw();

            float r = shieldRadius;
            if (r <= 0f) return;

            if (colorCached == null) {
                try {
                    colorCached = Color.valueOf(FullShieldWall.this.shieldColor);
                } catch (Exception ex) {
                    colorCached = Color.white;
                }
            }

            Draw.z(Layer.shields);

            if (useDefaultShieldTexture) {
                // vanilla style: Lines.circle
                Draw.color(colorCached, shieldOpacity);
                Lines.stroke(3f);
                Lines.circle(x, y, r + Mathf.sin(Time.time / 6f, 3f, 1f));
            } else {
                // solid fill
                Draw.color(colorCached, shieldOpacity);
                Fill.square(x, y, r);
            }

            Draw.reset();
        }
    }
}