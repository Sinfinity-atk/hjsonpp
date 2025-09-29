package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.world.blocks.defense.Wall;
import mindustry.graphics.Layer;

/**
 * FullShieldWall for HJSON++:
 * - public fields are tunable from .hjson
 * - per-build shield bookkeeping (currentShield)
 * - bullet interception inside shieldRadius
 * - regeneration, break cooldown
 * - simple lightning FX + optional reflect attempt
 *
 * NOTE: This implementation favors compatibility with Mindustry v8 / hjsonpp style loader.
 */
public class FullShieldWall extends Wall {

    /* ========== tunable public fields (set these in .hjson) ========== */

    // shield HP
    public float maxShield = 25000f;

    // HP/sec
    public float shieldRegen = 80f;

    // radius in world units (pixels)
    public float shieldRadius = 80f;

    // color as hex string (e.g. "66bfff" or "#66bfff")
    public String shieldColorHex = "66bfff";

    // opacity 0..1
    public float shieldOpacity = 0.35f;

    // whether to block lasers (visual flag; actual engine laser handling may depend on other code)
    public boolean absorbLasersFlag = true;

    // simple deflect/bounce attempt for bullets
    public boolean deflectBulletsFlag = false;
    // deflect chance 0..1
    public float deflectChance = 0f;

    // lightning FX on hit
    public boolean lightningOnHit = false;
    public float lightningChance = 0f;

    // cooldown (seconds) after shield break before regen resumes
    public float breakCooldownSeconds = 3f;

    /* ========== constructor ========== */

    public FullShieldWall(String name){
        super(name);
        // default wall-like behaviour (HJSON can override size/health/etc)
        this.update = true;
        this.solid = true;
        this.destructible = true;
    }

    /* ========== Build (tile instance) ========== */

    public class FullShieldWallBuild extends WallBuild {
        // current shield HP for this build
        public float currentShield;
        // parsed color cached
        private Color parsedColor;
        // cooldown timer (seconds) remaining after break
        private float breakCooldownTimer = 0f;

        @Override
        public void created() {
            super.created();
            // set initial shield from block's (possibly HJSON-set) value
            this.currentShield = FullShieldWall.this.maxShield;

            // parse color safely
            try {
                String hex = FullShieldWall.this.shieldColorHex;
                if (hex == null) parsedColor = Color.white;
                else parsedColor = Color.valueOf(hex);
            } catch (Throwable t) {
                parsedColor = Color.white;
            }
        }

        @Override
        public void updateTile() {
            // call super to keep wall internal updates
            super.updateTile();

            float r = FullShieldWall.this.shieldRadius;

            // if we are on cooldown after break, tick it down
            if (breakCooldownTimer > 0f) {
                breakCooldownTimer = Math.max(0f, breakCooldownTimer - Time.delta);
            }

            // absorb bullets while shield active and not on cooldown
            if (r > 0f && currentShield > 0f && breakCooldownTimer <= 0f) {
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    try {
                        if (b == null) return;
                        if (b.team == team) return; // ignore friendly
                        if (b.type == null) return;
                        if (!b.type.absorbable) return;

                        // circle membership test
                        float dx = b.x - x, dy = b.y - y;
                        if (dx * dx + dy * dy > r * r) return;

                        // attempt deflect if enabled
                        if (deflectBulletsFlag && Mathf.chance(deflectChance)) {
                            // try a simple bounce: reverse velocity and flip team to ours
                            try {
                                // reverse vel
                                b.vel.set(-b.vel.x, -b.vel.y);
                                // set team to ours (so it won't hurt us), best-effort (may be final)
                                b.team = team;
                            } catch (Throwable t) {
                                // fallback - remove bullet
                                try { b.remove(); } catch (Throwable t2) {}
                            }
                            return;
                        }

                        // otherwise consume the bullet
                        try { b.absorb(); } catch (Throwable t) { try { b.remove(); } catch (Throwable t2) {} }

                        // subtract damage (best effort)
                        float dmg = 1f;
                        try { dmg = b.type.damage; } catch (Throwable t) { dmg = 1f; }
                        currentShield -= dmg;

                        // lightning FX optionally
                        if (lightningOnHit && Mathf.chanceDelta(lightningChance)) {
                            try { Fx.lightningShoot.at(x, y); } catch (Throwable t) {}
                        }

                        // if depleted, trigger break behavior
                        if (currentShield <= 0f) {
                            currentShield = 0f;
                            onShieldBroken();
                        }
                    } catch (Throwable ex) {
                        // defensive: ignore per-bullet exceptions
                    }
                });
            }

            // regen if not broken and no cooldown
            if (currentShield > 0f && currentShield < FullShieldWall.this.maxShield && breakCooldownTimer <= 0f) {
                currentShield = Math.min(FullShieldWall.this.maxShield, currentShield + FullShieldWall.this.shieldRegen * Time.delta);
            }
        }

        /** Called when shield depletes */
        protected void onShieldBroken() {
            // start cooldown
            breakCooldownTimer = FullShieldWall.this.breakCooldownSeconds;

            // spawn shield-break effect, safe-call
            try { Fx.shieldBreak.at(x, y, FullShieldWall.this.shieldRadius, team.color); } catch (Throwable t) {}
        }

        /* Drawing the shield bubble */
        @Override
        public void draw() {
            super.draw();
            drawShield();
        }

        public void drawShield() {
            float r = FullShieldWall.this.shieldRadius;
            if (r <= 0f) return;
            if (currentShield <= 0f) return; // don't draw broken shield

            Color c = parsedColor != null ? parsedColor : Color.white;
            Draw.z(Layer.block + 0.1f);
            Draw.color(c, FullShieldWall.this.shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();
        }

        /* Optional toggles (no @Override to be safe across versions) */

        public boolean absorbLasers() {
            return FullShieldWall.this.absorbLasersFlag;
        }

        public boolean deflectBullets() {
            return FullShieldWall.this.deflectBulletsFlag;
        }

        /* Expose some queryable values if something checks them */
        public float getCurrentShield() {
            return currentShield;
        }

        public float getMaxShield() {
            return FullShieldWall.this.maxShield;
        }
    }
}