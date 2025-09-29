package hjsonpp.expand;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.Wall;

/**
 * FullShieldWall – a damageable wall with integrated shield projector + unit blocking.
 */
public class FullShieldWall extends Wall {

    // ───── CONFIG FIELDS ─────
    public float shieldRadius = 32f;
    public float shieldOpacity = 0.4f;
    public boolean shieldFill = true;
    public float shieldThickness = 4f;

    public boolean useForceTexture = false;  // use Mindustry force projector texture

    public boolean blockUnits = true;
    public float pushStrength = 2f;

    public float regenPerSec = 600f;        // shield regen
    public float wallRegenPerSec = 100f;    // wall regen
    public float shieldHealthCustom = 8000f;
    public String shieldColor = "7f7fff";

    // Shield-block-radius system
    public int shieldBlockRadius = 2;          // 1=smaller, 2=equal, 3=bigger
    public float shieldBlockRadiusAmount = 0.5f;

    // internal
    private Color colorCached;
    private TextureRegion forceTex;

    public FullShieldWall(String name) {
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void load() {
        super.load();
        try {
            colorCached = Color.valueOf(shieldColor);
        } catch (Exception ex) {
            colorCached = Color.white;
        }
        if (useForceTexture) {
            forceTex = Core.atlas.find("force-projector-shield", Core.atlas.find("clear")); // fallback
        }
    }

    public class FullShieldWallBuild extends WallBuild {
        public float shield = shieldHealthCustom;

        @Override
        public void updateTile() {
            // regen shield
            if (shield < shieldHealthCustom) {
                shield = Math.min(shieldHealthCustom, shield + regenPerSec * Time.delta / 60f);
            }
            // regen wall HP
            if (health < maxHealth) {
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // unit blocking
            if (blockUnits) {
                float pushR = calcBlockRadius(); // push radius based on block size

                Groups.unit.intersect(x - pushR, y - pushR, pushR * 2f, pushR * 2f, (Unit u) -> {
                    if (u.team == team || u.dead()) return;

                    float dx = u.x - x;
                    float dy = u.y - y;
                    float dist2 = dx * dx + dy * dy;
                    if (dist2 > pushR * pushR) return;

                    float dist = Mathf.sqrt(dist2);
                    if (dist < 1f) dist = 1f;

                    // constant push outward
                    u.vel.add(dx / dist * pushStrength * Time.delta,
                              dy / dist * pushStrength * Time.delta);
                });
            }
        }

        /** Calculate radius based on block size + ShieldBlockRadius setting */
        private float calcBlockRadius() {
            float base = (block.size * 8f) / 2f;
            switch (shieldBlockRadius) {
                case 1: // smaller
                    return base * (1f - shieldBlockRadiusAmount);
                case 2: // equal
                    return base;
                case 3: // bigger
                    return base * (1f + shieldBlockRadiusAmount);
                default:
                    return base;
            }
        }

        @Override
        public void draw() {
            super.draw();
            drawShield();
        }

        public void drawShield() {
            float r;
            // Use ShieldBlockRadius if 1–3, else fall back to shieldRadius
            if (shieldBlockRadius >= 1 && shieldBlockRadius <= 3) {
                r = calcBlockRadius();
            } else {
                r = shieldRadius;
            }

            if (r <= 0f) return;

            Draw.z(Layer.block + 0.1f);
            Color c = colorCached != null ? colorCached : Color.white;
            Draw.color(c, shieldOpacity);

            if (useForceTexture && forceTex != null) {
                // Use Mindustry force projector texture
                Draw.rect(forceTex, x, y, r * 2f, r * 2f);
            } else {
                // Draw as fill or ring
                if (shieldFill) {
                    Fill.circle(x, y, r);
                } else {
                    Lines.stroke(shieldThickness);
                    Lines.circle(x, y, r);
                    Lines.stroke(1f);
                }
            }
            Draw.reset();
        }
    }
}