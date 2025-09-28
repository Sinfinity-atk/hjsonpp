package hjsonpp.expand;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.content.Fx;
import mindustry.entities.bullet.Bullet;
import mindustry.gen.Groups;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.BlockGroup;
import mindustry.graphics.Layer;

/**
 * FullShieldWall - tuned for HJSON++ usage.
 *
 * Public fields are intended to be set via HJSON.
 *
 * This version avoids referencing engine-internal Block fields that are not public,
 * and implements shield bookkeeping in the building instance.
 */
public class FullShieldWall extends BaseShield {

    // --- HJSON-exposed tunables (public so HJSON++ can set them) ---
    public float shieldRadius = 80f;          // world units (pixels)
    public float shieldHealth = 25000f;       // maximum shield HP
    public float shieldRegen = 80f;           // HP per second
    public String shieldColor = "66bfff";     // hex string
    public float shieldOpacity = 0.35f;       // 0..1 alpha

    public boolean absorbLasers = true;       // block lasers/electric beams
    public boolean deflectBullets = false;    // placeholder flag
    public float deflectChance = 0f;          // 0..1
    public boolean lightningOnHit = false;    // spawn lightning when hit
    public float lightningChance = 0f;        // 0..1

    public FullShieldWall(String name) {
        super(name);
        // make it behave like a wall-type block by default
        this.group = BlockGroup.walls;
        this.update = true;
        this.solid = true;
        this.insulated = true;
        // do not assume any BaseShield internals are present — we handle shield in the Build
    }

    /**
     * Build (tile) class for the shield wall. We maintain shield HP here and handle
     * bullet interception and shield regen.
     */
    public class FullShieldWallBuild extends BaseShieldBuild {
        // current shield HP on this build
        public float currentShield = FullShieldWall.this.shieldHealth;
        // cached color
        private Color colorCached = null;

        @Override
        public void created() {
            super.created();
            // initialize shield amount
            currentShield = FullShieldWall.this.shieldHealth;
            // parse color once
            try {
                colorCached = Color.valueOf(FullShieldWall.this.shieldColor);
            } catch (Exception ex) {
                colorCached = Color.white;
            }
        }

        @Override
        public void updateTile() {
            // First: let BaseShield do its internal work (range checks, warmup, etc.)
            super.updateTile();

            // Intercept bullets in the shield radius and reduce shield HP
            float r = FullShieldWall.this.shieldRadius;
            if (r > 0 && !isBroken()) { // only absorb when shield is active
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, b -> {
                    try {
                        // only enemy bullets & absorbable types
                        if (b == null) return;
                        if (b.team == team) return;
                        if (b.type == null) return;
                        if (!b.type.absorbable) return;

                        // quick circle check
                        float dx = b.x - x;
                        float dy = b.y - y;
                        if (dx * dx + dy * dy > r * r) return;

                        // consume the bullet
                        // prefer absorb() if present
                        try {
                            b.absorb();
                        } catch (Throwable t) {
                            // fallback: remove if absorb is not available
                            try { b.remove(); } catch (Throwable t2) { }
                        }

                        // reduce shield by bullet damage (using bullet type damage)
                        float dmg = 1f;
                        try {
                            if (b.type != null) dmg = b.type.damage;
                        } catch (Throwable t) {
                            dmg = 1f;
                        }

                        currentShield -= dmg;

                        // optional lightning / hit effect
                        if (lightningOnHit && Mathf.chanceDelta(lightningChance)) {
                            try { Fx.lightningShoot.at(x, y); } catch (Throwable t) { }
                        }

                        // If shield depletes, call onBreak behaviour
                        if (currentShield <= 0f) {
                            currentShield = 0f;
                            onShieldBroken();
                        }
                    } catch (Throwable err) {
                        // defensive: don't let one bullet crash the update
                    }
                });
            }

            // Regeneration: only regenerate if shield not at max and not broken (simple behavior)
            if (!isBroken() && currentShield < FullShieldWall.this.shieldHealth) {
                currentShield = Math.min(FullShieldWall.this.shieldHealth,
                        currentShield + FullShieldWall.this.shieldRegen * Time.delta);
            }
        }

        // Simple helper - consider "broken" if shield is 0
        public boolean isBroken() {
            return currentShield <= 0f;
        }

        // Called when shield breaks (you can expand to add cooldowns, etc.)
        protected void onShieldBroken() {
            // default: spawn shield break effect
            try { Fx.shieldBreak.at(x, y, FullShieldWall.this.shieldRadius, team.color); } catch (Throwable t) { }
            // Optional: you may implement cooldown logic here if desired
        }

        // Provide the real radius to the engine when it queries
        @Override
        public float realRadius() {
            return FullShieldWall.this.shieldRadius;
        }

        // Provide current shield HP to any engine queries if used
        @Override
        public float shieldHealth() {
            return currentShield;
        }

        // Draw a colored shield ring (semi-transparent)
        @Override
        public void drawShield() {
            float r = realRadius();
            if (r <= 0f) return;

            // draw simple filled circle with chosen color/opacity
            Draw.z(Layer.block + 0.1f);
            Color c = colorCached != null ? colorCached : Color.white;
            Draw.color(c, FullShieldWall.this.shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();

            // call super to preserve any base drawing behavior
            super.drawShield();
        }

        // optional toggles - these do not have @Override in case the engine doesn't expose them
        public boolean absorbLasers() {
            return FullShieldWall.this.absorbLasers;
        }

        public boolean deflectBullets() {
            return FullShieldWall.this.deflectBullets;
        }
    }
                                         }        public float customShieldHealth = shieldHealthCustom;
        public float shield = shieldHealthCustom;

        @Override
        public void updateTile(){
            // normal BaseShield update
            super.updateTile();

            // custom regen
            if(shield < customShieldHealth){
                shield = Math.min(customShieldHealth, shield + shieldRegen * delta());
            }

            // optional lightning on hit
            if(lightningOnHit && this.hit > 0 && Mathf.chanceDelta(lightningChance)){
                Fx.lightningShoot.at(x, y);
            }
        }

        // Called by BaseShieldBuild to know how large the shield is
        @Override
        public float realRadius(){
            return shieldRadius;
        }

        // Called to know current shield HP
        @Override
        public float shieldHealth(){
            return shield;
        }

        public boolean absorbLasers(){
            return absorbLasers;
        }

        public boolean deflectBullets(){
            return deflectBullets;
        }
    }
}        super.load();

        // Push HJSON properties into BaseShield internals
        this.radius = shieldRadius;
        this.shieldHealth = shieldHealthCustom;
        this.cooldownNormal = cooldownNormalCustom;
        this.cooldownBrokenBase = cooldownBrokenBaseCustom;
        this.phaseShieldBoost = phaseShieldBoostCustom;
    }

    public class FullShieldWallBuild extends BaseShieldBuild {
        @Override
        public void updateTile(){
            super.updateTile();
            if(lightningOnHit && this.hit > 0 && Mathf.chanceDelta(lightningChance)){
                Fx.lightningShoot.at(x, y);
            }
        }

        public boolean absorbLasers(){
            return absorbLasers;
        }

        public boolean deflectBullets(){
            return deflectBullets;
        }
    }
}
