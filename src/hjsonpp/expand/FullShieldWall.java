package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;

import mindustry.entities.Units;
import mindustry.entities.effect.Fx;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

/**
 * Full Shield Wall - hybrid of Wall + BaseShield behavior.
 */
public class FullShieldWall extends Wall {

    // tunables via HJSON
    public float shieldRadius = 60f;
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;          // shield regen per second
    public float wallRegenPerSec = 0f;       // wall HP regen per sec
    public String shieldColor = "7f7fff";
    public float shieldOpacity = 0.3f;
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
            // shield regen
            if (shield < shieldHealthCustom) {
                shield += regenPerSec * Time.delta / 60f;
                if (shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            // wall HP regen
            if (wallRegenPerSec > 0 && health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            float r = shieldRadius;

            // bullet blocking
            if (r > 0f) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if (b.team == team) return;
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if (dx * dx + dy * dy > r * r) return;

                    if (deflectBullets) {
                        b.vel.setAngle(b.vel.angle() + 180f); // reflect
                    } else {
                        b.remove();
                    }
                });
            }

            // --- BaseShield-style unit blocking ---
            if (blockUnits && r > 0f) {
                Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, unit -> {
                    if (unit.dead()) return;

                    float overlapDst = (unit.hitSize / 2f + r) - unit.dst(this);

                    if (overlapDst > 0) {
                        if (overlapDst > unit.hitSize * 1.5f) {
                            // instakill units stuck inside shield
                            unit.kill();
                        } else {
                            // stop movement
                            unit.vel.setZero();

                            // push out
                            Vec2 moveVec = Tmp.v1.set(unit).sub(this).setLength(overlapDst + 0.01f);
                            unit.move(moveVec);

                            if (Mathf.chanceDelta(0.12f * Time.delta)) {
                                Fx.circleColorSpark.at(unit.x, unit.y, team.color);
                            }
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

            Draw.z(Layer.shields);
            Draw.color(colorCached, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
        }
    }
}