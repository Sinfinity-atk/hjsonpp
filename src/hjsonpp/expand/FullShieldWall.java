package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.blocks.defense.BaseShield.BaseShieldBuild;
import mindustry.world.meta.BlockGroup;
import mindustry.graphics.Layer;

/**
 * FullShieldWall as a BaseShield-based block.
 * Public fields = tunables for HJSON++ (.hjson must match names exactly).
 */
public class FullShieldWall extends BaseShield {

    // ---- Public tunables (HJSON must use these exact keys) ----
    public float maxShield = 25000f;        // capacity
    public float shieldRegen = 80f;         // HP per second
    public float shieldRadius = 80f;        // world units (pixels)
    public String shieldColorHex = "66bfff";
    public float shieldOpacity = 0.35f;
    public boolean absorbLasersFlag = true; // logical flag
    public boolean deflectBulletsFlag = false;
    public float deflectChance = 0f;
    public boolean lightningOnHit = false;
    public float lightningChance = 0f;
    public float breakCooldownSeconds = 3f; // seconds to wait after break before regen

    public FullShieldWall(String name){
        super(name);
        this.group = BlockGroup.walls;
        this.update = true;
        this.solid = true;
        this.insulated = true;
    }

    public class FullShieldWallBuild extends BaseShieldBuild {
        // per-tile shield state
        public float currentShield;
        private Color parsedColor;
        private float breakCooldownTimer = 0f;

        @Override
        public void created() {
            super.created();
            // read values from the block (HJSON should have already populated them)
            this.currentShield = FullShieldWall.this.maxShield;
            try {
                parsedColor = Color.valueOf(FullShieldWall.this.shieldColorHex);
            } catch (Throwable t) {
                parsedColor = Color.white;
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();

            float r = FullShieldWall.this.shieldRadius;

            // cooldown ticking
            if (breakCooldownTimer > 0f) {
                breakCooldownTimer = Math.max(0f, breakCooldownTimer - Time.delta);
            }

            // bullet interception while shield active and not cooling down
            if (r > 0f && currentShield > 0f && breakCooldownTimer <= 0f) {
                Groups.bullet.intersect(x - r, y - r, r*2f, r*2f, (Bullet b) -> {
                    try {
                        if (b == null) return;
                        if (b.team == team) return;
                        if (b.type == null) return;
                        if (!b.type.absorbable) return;

                        float dx = b.x - x, dy = b.y - y;
                        if (dx*dx + dy*dy > r*r) return;

                        // try deflect first if enabled
                        if (deflectBulletsFlag && Mathf.chance(deflectChance)) {
                            try {
                                // basic bounce: reverse velocity and flip team (best-effort)
                                b.vel.set(-b.vel.x, -b.vel.y);
                                b.team = team;
                            } catch (Throwable t) {
                                try { b.remove(); } catch (Throwable t2) {}
                            }
                            return;
                        }

                        try { b.absorb(); } catch (Throwable t) { try { b.remove(); } catch (Throwable t2) {} }

                        float dmg = 1f;
                        try { dmg = b.type.damage; } catch (Throwable t) { dmg = 1f; }
                        currentShield -= dmg;

                        if (lightningOnHit && Mathf.chanceDelta(lightningChance)) {
                            try { Fx.lightningShoot.at(x, y); } catch (Throwable t) {}
                        }

                        if (currentShield <= 0f) {
                            currentShield = 0f;
                            onShieldBroken();
                        }
                    } catch (Throwable ex) {}
                });
            }

            // regeneration when not broken and not cooling down
            if (currentShield > 0f && currentShield < FullShieldWall.this.maxShield && breakCooldownTimer <= 0f) {
                currentShield = Math.min(FullShieldWall.this.maxShield, currentShield + FullShieldWall.this.shieldRegen * Time.delta);
            }
        }

        protected void onShieldBroken() {
            breakCooldownTimer = FullShieldWall.this.breakCooldownSeconds;
            try { Fx.shieldBreak.at(x, y, FullShieldWall.this.shieldRadius, team.color); } catch (Throwable t) {}
        }

        @Override
        public float realRadius() {
            return FullShieldWall.this.shieldRadius;
        }

        @Override
        public float shieldHealth() {
            return currentShield;
        }

        @Override
        public void drawShield() {
            float r = realRadius();
            if (r <= 0f) return;
            if (currentShield <= 0f) return;

            Draw.z(Layer.block + 0.1f);
            Color c = parsedColor != null ? parsedColor : Color.white;
            Draw.color(c, FullShieldWall.this.shieldOpacity);
            Fill.circle(x, y, r);
            Draw.reset();

            super.drawShield();
        }

        /* Optional helpers (no @Override to avoid cross-version mismatch) */
        public boolean absorbLasers() { return FullShieldWall.this.absorbLasersFlag; }
        public boolean deflectBullets() { return FullShieldWall.this.deflectBulletsFlag; }
    }
}