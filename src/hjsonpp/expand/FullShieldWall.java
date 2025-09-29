package hjsonpp.expand;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.type.*;
import mindustry.world.blocks.defense.Wall;

public class FullShieldWall extends Wall {
    // Shield / wall config
    public float shieldRadius = 40f;
    public float shieldOpacity = 0.2f;
    public float regenPerSec = 20f;
    public float wallRegenPerSec = 5f;

    // pushback
    public float pushStrength = 0.5f;
    public int shieldBlockRadius = 2;         // 1=smaller 2=exact block 3=bigger
    public float shieldBlockRadiusAmount = 1; // multiplier for small/large modes

    // new properties
    public boolean absorbLasers = false;
    public boolean deflectBullets = false;

    public boolean lightningOnHit = false;
    public float lightningChance = 0.05f;
    public int lightningDamage = 30;

    public String shieldColor = "ffffff";
    public Color colorCached = Color.white;

    public FullShieldWall(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = false;
    }

    @Override
    public void load() {
        super.load();
        try {
            colorCached = Color.valueOf(shieldColor);
        } catch (Exception e) {
            colorCached = Color.white;
        }
    }

    public class FullShieldWallBuild extends WallBuild {
        public float shield = shieldRadius;

        @Override
        public void updateTile() {
            // wall self-heal
            if (health < maxHealth) {
                heal(wallRegenPerSec * Time.delta / 60f);
            }

            // shield regen
            if (shield < shieldRadius) {
                shield = Math.min(shieldRadius, shield + regenPerSec * Time.delta / 60f);
            }

            // compute push radius from block size + ShieldBlockRadius mode
            float baseRadius = block.size * 8f / 2f; // tileSize * size /2
            float r;
            if (shieldBlockRadius == 1) {
                r = baseRadius * shieldBlockRadiusAmount;
            } else if (shieldBlockRadius == 3) {
                r = baseRadius * shieldBlockRadiusAmount;
            } else {
                r = baseRadius; // exact size of block
            }

            // pushback units
            Groups.unit.intersect(x - r, y - r, r * 2, r * 2, u -> {
                if (u.team != team && !u.isFlying()) {
                    float dx = u.x - x;
                    float dy = u.y - y;
                    float len = Mathf.len(dx, dy);
                    if (len < r && len > 0.0001f) {
                        dx /= len;
                        dy /= len;
                        // constant push regardless of distance
                        u.vel.add(dx * pushStrength, dy * pushStrength);
                    }
                }
            });

            // deflect bullets if enabled
            if (deflectBullets || absorbLasers) {
                float bulletRadius = r;
                Groups.bullet.intersect(x - bulletRadius, y - bulletRadius, bulletRadius * 2f, bulletRadius * 2f, b -> {
                    if (b.team != team) {
                        // deflect vs absorb
                        if (deflectBullets && Mathf.chanceDelta(1f)) {
                            b.vel.setAngle(b.vel.angle() + 180f); // bounce back
                        }
                        if (absorbLasers && b.type.absorbable) {
                            b.remove();
                        }

                        // lightning on hit
                        if (lightningOnHit && Mathf.chanceDelta(lightningChance)) {
                            try {
                                Fx.lightningShoot.at(x, y);
                                if (b.owner instanceof Unit u) {
                                    u.damage(lightningDamage);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void draw() {
            super.draw();

            // draw shield
            float baseRadius = block.size * 8f / 2f;
            float r;
            if (shieldBlockRadius == 1) {
                r = baseRadius * shieldBlockRadiusAmount;
            } else if (shieldBlockRadius == 3) {
                r = baseRadius * shieldBlockRadiusAmount;
            } else {
                r = baseRadius;
            }

            Draw.z(Layer.block + 0.1f);
            Color c = colorCached;
            Draw.color(c, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
        }
    }
}