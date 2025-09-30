package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;

import mindustry.Vars;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;

/**
 * Shield Wall with tunable properties.
 */
public class FullShieldWall extends Wall {

    // --- tunables ---
    public float shieldRadius = 60f;       // shield radius (ignored if ShieldBlockRadius set)
    public float shieldHealthCustom = 4000f;
    public float regenPerSec = 20f;        // shield regen per second
    public float wallRegenPerSec = 0f;     // wall HP regen per sec
    public String shieldColor = "7f7fff";
    public float shieldOpacity = 0.3f;     // opacity of shield circle
    public boolean useDefaultShieldTexture = false; // vanilla projector style

    public boolean absorbLasers = true;
    public boolean deflectBullets = true;
    public boolean blockUnits = true;

    // --- new push toggle ---
    public boolean pushUnits = false;   // true = push, false = stop-solid
    public float pushStrength = 1.5f;  // force applied if pushUnits = true

    // --- shield block radius options ---
    public int shieldBlockRadius = 2;      // 1=smaller, 2=equal, 3=bigger
    public float shieldBlockRadiusAmount = 0.5f; // how much smaller/bigger

    // --- lightning effects ---
    public boolean lightningOnHit = false;
    public float lightningChance = 0.05f;
    public float lightningDamage = 10f;

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
            if(shield < shieldHealthCustom){
                shield += regenPerSec * Time.delta / 60f;
                if(shield > shieldHealthCustom) shield = shieldHealthCustom;
            }

            // regen wall HP
            if(wallRegenPerSec > 0 && health < maxHealth){
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // bullet interaction
            float r = realShieldRadius();
            if(r > 0f){
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if(b.team == team) return;
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if(dx * dx + dy * dy > r * r) return;

                    if(deflectBullets && Mathf.chanceDelta(100f)){
                        b.vel.setAngle(b.vel.angle() + 180f); // bounce back
                    }else{
                        try{
                            b.remove();
                        }catch(Throwable t){
                            // ignore
                        }
                    }
                });
            }

            // unit blocking
            if(blockUnits && r > 0f){
                Units.nearbyEnemies(team, x, y, r + 8f, (Unit u) -> {
                    if(u.team == team || u.dead()) return;

                    float dx = u.x - x;
                    float dy = u.y - y;
                    float dist2 = dx * dx + dy * dy;

                    if(dist2 < r * r){
                        float dist = Mathf.sqrt(dist2);
                        if(dist < 1f) dist = 1f;

                        if(pushUnits){
                            // constant push (not gradient)
                            float px = dx / dist * pushStrength * Time.delta;
                            float py = dy / dist * pushStrength * Time.delta;
                            u.vel.add(px, py);
                        }else{
                            // stop-solid like BaseShield
                            u.vel.setZero();
                            u.impulseNet(new Vec2(-dx / dist * 0.4f, -dy / dist * 0.4f));
                        }
                    }
                });
            }
        }

        // --- helpers ---
        private float realShieldRadius(){
            float base = (block.size * Vars.tilesize);
            if(shieldBlockRadius == 1) return base * (1f - shieldBlockRadiusAmount);
            if(shieldBlockRadius == 2) return base;
            if(shieldBlockRadius == 3) return base * (1f + shieldBlockRadiusAmount);
            return shieldRadius;
        }

        private Color parsedColor(){
            if(colorCached == null){
                try{
                    colorCached = Color.valueOf(FullShieldWall.this.shieldColor);
                }catch(Exception ex){
                    colorCached = Color.white;
                }
            }
            return colorCached;
        }

        public boolean absorbLasers(){ return FullShieldWall.this.absorbLasers; }
        public boolean deflectBullets(){ return FullShieldWall.this.deflectBullets; }

        @Override
        public void draw(){
            super.draw();

            float r = realShieldRadius();
            if(r <= 0f) return;

            Draw.z(Layer.shields);

            Color c = parsedColor();
            if(useDefaultShieldTexture){
                Draw.color(c, shieldOpacity);
                Lines.stroke(0f);
                Fill.square(x, y, r + Mathf.sin(Time.time / 6f, 3f, 1f));
            }else{
                Draw.color(c, shieldOpacity);
                Fill.circle(x, y, r);
            }

            Draw.reset();
        }
    }
}