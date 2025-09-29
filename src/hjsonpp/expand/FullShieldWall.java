package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.gen.Unit;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.blocks.BlockBuild; // <-- correct base

public class FullShieldWall extends Block {

    public float shieldRadius = 40f;
    public float shieldHealth = 8000f;
    public float regenPerSec = 100f;
    public float pushStrength = 1.5f;

    public boolean lightningOnHit = true;
    public float lightningChance = 0.05f;
    public float lightningDamage = 40f;

    public Color shieldColor = Color.valueOf("7f7fff");
    public float shieldOpacity = 0.4f;

    // shape options
    public int shieldBlockRadius = 2; // 1=smaller,2=equal,3=bigger
    public float shieldBlockRadiusAmount = 1f;

    public FullShieldWall(String name) {
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.walls;
        category = Category.defense;
        destructible = true;
    }

    public class FullShieldWallBuild extends BlockBuild {   // <-- was Building

        float shield = shieldHealth;

        @Override
        public void updateTile() {
            // regen shield
            if (shield < shieldHealth) {
                shield += regenPerSec * Time.delta / 60f;
                if (shield > shieldHealth) shield = shieldHealth;
            }

            // push units in block area
            float r = realRadius();
            Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, u -> {
                if (u.within(x, y, r)) {
                    // push outward with fixed force
                    float dx = u.x - x;
                    float dy = u.y - y;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len < 0.001f) len = 0.001f;
                    dx /= len;
                    dy /= len;
                    u.vel.add(dx * pushStrength, dy * pushStrength);
                    // lightning effect like surge walls
                    if (lightningOnHit && Mathf.chanceDelta(lightningChance)) {
                        Lightning.create(team, shieldColor, lightningDamage, x, y, Mathf.random(360f), 10);
                        u.damage(lightningDamage);
                    }
                }
            });
        }

        public float realRadius() {
            // decide actual radius based on shieldBlockRadius
            float base = shieldRadius;
            if (shieldBlockRadius == 1) {
                base = size * Vars.tilesize / 2f * shieldBlockRadiusAmount;
            } else if (shieldBlockRadius == 2) {
                base = size * Vars.tilesize / 2f; // same as block
            } else if (shieldBlockRadius == 3) {
                base = size * Vars.tilesize / 2f * shieldBlockRadiusAmount;
            }
            return base;
        }

        @Override
        public void draw() {
            super.draw();

            float r = realRadius();
            if (r <= 0f) return;

            Draw.z(0.1f);
            Color c = shieldColor;
            Draw.color(c, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
        }
    }
}