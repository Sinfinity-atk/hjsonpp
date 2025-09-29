package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.gen.Bullet;            // ✅ correct Bullet import
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.Wall;

public class FullShieldWall extends Wall {

    public float shieldRadius = 40f;        // radius of the shield
    public float shieldHealthCustom = 1000; // shield health
    public float regenPerSecond = 10f;      // regen per second
    public Color shieldColor = Color.valueOf("84f491"); // default shield color
    public float shieldOpacity = 0.4f;      // transparency
    public boolean absorbLasers = true;
    public boolean deflectBullets = true;
    public boolean lightningOnHit = false;
    public float lightningChance = 0.1f;

    public FullShieldWall(String name){
        super(name);
        update = true;
        solid = true;
        destructible = true;
    }

    @Override
    public void load(){
        super.load();
    }

    public class FullShieldWallBuild extends WallBuild {
        public float shield = shieldHealthCustom;
        private Color colorCached;

        @Override
        public void updateTile(){
            // simple regen
            if(shield < shieldHealthCustom){
                shield += regenPerSecond * Time.delta;
                if(shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            float r = shieldRadius;

            // absorb bullets inside radius
            if(r > 0f && shield > 0f){
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if(b.team == team) return;
                    // check distance
                    float dx = b.x - x, dy = b.y - y;
                    if(dx*dx + dy*dy > r*r) return;

                    // absorb bullet
                    try{ b.remove(); }catch(Throwable ignored){}
                    if(lightningOnHit && Mathf.chanceDelta(lightningChance)){
                        // optional lightning effect
                        //Fx.lightningShoot.at(x, y); // disabled by default
                    }
                    shield -= 5f; // subtract some shield health
                    if(shield < 0f) shield = 0f;
                });

                // also push units back if desired:
                Units.nearbyEnemies(team, x - r, y - r, r*2f, r*2f, (Unit u)->{
                    if(u.within(x,y,r)){
                        u.impulse((u.x - x)*0.05f,(u.y - y)*0.05f);
                    }
                });
            }
        }

        public float realRadius(){
            return shieldRadius;
        }

        // ⚠️ removed @Override — these aren’t in super
        public boolean absorbLasers(){
            return absorbLasers;
        }

        public boolean deflectBullets(){
            return deflectBullets;
        }

        @Override
        public void draw(){
            super.draw();
            drawShield();
        }

        public void drawShield(){
            float r = realRadius();
            if(r <= 0f || shield <= 0f) return;

            if(colorCached == null){
                try {
                    colorCached = shieldColor;
                } catch(Exception ignored){
                    colorCached = Color.white;
                }
            }

            Draw.z(115f); // above blocks
            Draw.color(colorCached, shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
        }
    }
}