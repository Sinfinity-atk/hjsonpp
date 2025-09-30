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
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.BlockGroup;

/**
 * FullShieldWall (BaseShield-backed) with a Push toggle.
 *
 * - If push == true -> apply our constant push behavior (pushStrength).
 * - If push == false -> defer to BaseShield's default unit-handling (stops movement like vanilla).
 */
public class FullShieldWall extends BaseShield {

    // ---- Tunables (editable from HJSON) ----
    public float shieldRadius = 60f;                // fallback radius if shieldBlockRadius not used
    public String shieldColor = "7f7fff";
    public float shieldOpacity = 0.35f;
    public boolean shieldFill = true;               // fallback drawing choice (if used)
    public boolean useForceTexture = false;         // prefer engine shield visuals (usually true)

    public float regenPerSec = 20f;                 // shield regen
    public float wallRegenPerSec = 0f;              // wall health regen

    // Unit-blocking / push settings
    public boolean blockUnits = true;               // master switch for blocking
    public boolean push = false;                     // **NEW TOGGLE**: true => custom push, false => vanilla stop
    public float pushStrength = 2f;                 // constant push force (when push==true)

    // ShieldBlockRadius system
    public int shieldBlockRadius = 2;               // 1 = smaller, 2 = same as block, 3 = bigger
    public float shieldBlockRadiusAmount = 0.5f;

    // Lightning / surge options (optional)
    public boolean lightningOnHit = false;
    public float lightningChance = 0.02f;
    public int lightningDamage = 30;

    // parsed color
    protected Color parsedColor = Color.valueOf("7f7fff");

    public FullShieldWall(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.walls;
    }

    @Override
    public void load(){
        super.load();
        try {
            parsedColor = Color.valueOf(shieldColor);
        } catch(Exception e){
            parsedColor = Color.white;
        }
    }

    public class FullShieldWallBuild extends BaseShield.BaseShieldBuild {
        @Override
        public void updateTile(){
            // compute desired shield radius using ShieldBlockRadius system (if enabled)
            float desiredShieldRadius;
            if(shieldBlockRadius >= 1 && shieldBlockRadius <= 3){
                float base = (block.size * Vars.tilesize);
                if(shieldBlockRadius == 1) desiredShieldRadius = base * (1f - shieldBlockRadiusAmount);
                else if(shieldBlockRadius == 2) desiredShieldRadius = base;
                else desiredShieldRadius = base * (1f + shieldBlockRadiusAmount);
            } else {
                desiredShieldRadius = FullShieldWall.this.shieldRadius;
            }

            // assign to block radius so BaseShield code uses it
            FullShieldWall.this.radius = desiredShieldRadius;

            // wall HP regen (keeps current behaviour if any)
            if(wallRegenPerSec > 0f && health < maxHealth){
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // If we want vanilla behavior for unit handling, just call super.updateTile(),
            // which will let BaseShield run its own unitConsumer and bullet logic.
            if(!push){
                // Keep using BaseShield's full behavior (bullets + unit blocking)
                super.updateTile();
                return;
            }

            // If push == true, we want to use BaseShield's bullet handling, but replace unit behavior
            // with our constant push. We'll mimic enough of BaseShieldBuild.updateTile to use bulletConsumer.

            // Smooth radius behavior similar to BaseShield
            smoothRadius = Mathf.lerpDelta(smoothRadius, FullShieldWall.this.radius * efficiency, 0.05f);

            float rad = radius(); // smoothed radius (used for bullet interception)

            if(rad > 1f){
                // Use BaseShield's bulletConsumer (vanilla bullet absorption)
                paramBuild = this; // BaseShield's consumers rely on this static paramBuild
                Groups.bullet.intersect(x - rad, y - rad, rad * 2f, rad * 2f, BaseShield.bulletConsumer);

                // Custom unit handling: constant push using block-size as push radius
                if(blockUnits){
                    float pushBase = (block.size * Vars.tilesize) / 2f;
                    float pushR;
                    // decide push radius using same ShieldBlockRadius modes
                    if(shieldBlockRadius == 1) pushR = pushBase * (1f - shieldBlockRadiusAmount);
                    else if(shieldBlockRadius == 2) pushR = pushBase;
                    else pushR = pushBase * (1f + shieldBlockRadiusAmount);

                    // Nearby enemies within pushR + small buffer
                    Units.nearbyEnemies(team, x, y, pushR + 8f, u -> {
                        if(u.team == team || u.dead()) return;

                        float dx = u.x - x;
                        float dy = u.y - y;
                        float dist2 = dx*dx + dy*dy;
                        if(dist2 < pushR * pushR){
                            float dist = Mathf.sqrt(dist2);
                            if(dist < 1f) dist = 1f;

                            // CONSTANT push (frame-rate scaled)
                            u.vel.add(dx / dist * pushStrength * Time.delta, dy / dist * pushStrength * Time.delta);

                            // optional surge lightning effect/damage
                            if(lightningOnHit && Mathf.chanceDelta(lightningChance)){
                                try{
                                    Lightning.create(team, parsedColor, lightningDamage, x, y, Mathf.random(360f), 10);
                                } catch(Throwable t){}
                                u.damage(lightningDamage);
                            }
                        }
                    });
                }
            }
            // don't call super.updateTile() because we already ran bulletConsumer and replaced unit behavior
        }

        @Override
        public void draw(){
            // Let BaseShield draw the base visuals (it handles the animated shield)
            super.draw();

            // If BaseShield visuals are disabled or you want a fallback ring/fill, draw it
            if(!shieldFill){
                float r = radius();
                if(r > 0f){
                    Draw.z(50f);
                    Draw.color(parsedColor, FullShieldWall.this.shieldOpacity);
                    Fill.circle(x, y, r);
                    Draw.reset();
                }
            }
        }
    }
}