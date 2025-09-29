package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.bullet.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

public class FullShieldWall extends Wall {

    public float shieldRadius = 60f;             // radius of the shield bubble
    public float shieldHealthCustom = 8000f;     // shield HP
    public float regenPerSec = 150f;             // shield regen per second
    public float shieldOpacity = 0.25f;
    public String shieldColor = "ffffff";
    public boolean absorbLasers = true;
    public boolean deflectBullets = true;

    public FullShieldWall(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.walls;
        buildType = FullShieldWallBuild::new;
    }

    public class FullShieldWallBuild extends WallBuild {

        public float shield = shieldHealthCustom;
        public Color colorCached = Color.valueOf(shieldColor);

        public void updateTile() {
            // simple regen
            if(shield < shieldHealthCustom){
                shield += regenPerSec * Vars.state.delta / 60f;
                if(shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            float r = shieldRadius;

            // stop bullets
            Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (b) -> {
                if (b.team == team) return;
                float dx = b.x - x, dy = b.y - y;
                if (dx * dx + dy * dy > r * r) return;

                // absorb or deflect
                if(deflectBullets){
                    b.vel.setAngle(b.vel.angle() + 180f);
                }else{
                    b.remove();
                }
            });

            // push units
            Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, u -> {
                float dx = u.x - x, dy = u.y - y;
                float dst2 = dx * dx + dy * dy;
                if(dst2 < r * r){
                    float ang = Mathf.angle(dx, dy);
                    u.vel.add(Mathf.cosDeg(ang) * 0.4f, Mathf.sinDeg(ang) * 0.4f);
                }
            });
        }

        public void drawShield() {
            float r = shieldRadius;
            if (r <= 0f) return;

            Draw.z(Layer.block + 0.1f);
            Color c = colorCached != null ? colorCached : Color.white;
            Draw.color(c, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.color();
            Lines.circle(x, y, r);
            Draw.reset();
        }

        @Override
        public void draw() {
            super.draw();
            drawShield();
        }
    }
}