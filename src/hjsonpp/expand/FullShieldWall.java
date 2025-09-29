package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.Units.UnitQuery;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Category;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.defense.BaseShield;

public class FullShieldWall extends BaseShield {

    public float shieldRadius = 100f;
    public float shieldHealthCustom = 8000f;
    public float cooldownNormalCustom = 0.1f;
    public float cooldownBrokenBaseCustom = 0.02f;

    public Color shieldColor = Color.valueOf("ffffff");
    public float shieldOpacity = 0.3f;

    public boolean absorbLasers = true;
    public boolean deflectBullets = true;

    public FullShieldWall(String name) {
        super(name);
        category = Category.defense;
        solid = true;
        update = true;
        size = 2;
        health = 10000;
        armor = 20;

        // default stats
        this.buildType = FullShieldWallBuild::new;
    }

    public class FullShieldWallBuild extends BaseShieldBuild {
        public float currentShield = shieldHealthCustom;
        public Color colorCached;

        @Override
        public void updateTile() {
            super.updateTile();

            // regen shield
            if (currentShield < shieldHealthCustom) {
                currentShield = Mathf.lerpDelta(currentShield, shieldHealthCustom, cooldownNormalCustom);
            }

            // unit blocking (basic pushback)
            float r = shieldRadius;
            Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, u -> {
                if (u != null && !u.dead() && u.within(this, r)) {
                    float dx = u.x - x;
                    float dy = u.y - y;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len != 0f) {
                        float push = 2f; // push strength
                        u.vel.add(dx / len * push, dy / len * push);
                    }
                }
            });
        }

        // removed @Override annotations:
        public float realRadius() {
            return shieldRadius;
        }

        public float shieldHealth() {
            return currentShield;
        }

        public boolean absorbLasers() {
            return absorbLasers;
        }

        public boolean deflectBullets() {
            return deflectBullets;
        }

        @Override
        public void drawShield() {
            float r = shieldRadius;
            if (r <= 0f) return;

            Draw.z(mindustry.graphics.Layer.block + 0.1f);
            Color c = colorCached != null ? colorCached : shieldColor;
            Draw.color(c, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
            super.drawShield();
        }
    }
}