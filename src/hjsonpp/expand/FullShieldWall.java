package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;

import mindustry.Vars;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

/**
 * A tunable shield wall block for Hjson++ mods.
 * This acts like a wall but with a regenerating shield radius.
 */
public class FullShieldWall extends Wall {

    public float shieldRadius = 60f;       // pixels radius of the shield
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;        // shield regen per second
    public String shieldColor = "7f7fff";  // default color

    public boolean absorbLasers = true;
    public boolean deflectBullets = true;

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

            // bullet interaction inside shield radius
            float r = shieldRadius;
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

            Draw.z(Layer.block + 0.1f);
            Draw.color(colorCached, 0.3f);
            Fill.circle(x, y, r);
            Draw.color();
        }
    }
}