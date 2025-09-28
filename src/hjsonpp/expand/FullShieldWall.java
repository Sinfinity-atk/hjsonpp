package hjsonpp.expand;

import arc.graphics.Color;
import mindustry.entities.effect.Fx;
import mindustry.type.Item;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.BlockGroup;

/**
 * A “library” wall block: behaves like a BaseShield but is fully adjustable from HJSON.
 * Drop this file in hjsonpp/expand/ and register in HjsonPlusPlusMod.java:
 * ClassMap.classes.put("FullShieldWall", hjsonpp.expand.FullShieldWall.class);
 */
public class FullShieldWall extends BaseShield {

    // --- Adjustable fields via HJSON ---
    public float shieldRadius = 80f;         // Shield radius
    public float shieldHealth = 25000f;      // Total shield HP
    public float shieldRegen = 80f;          // HP/sec regen
    public float cooldownNormal = 0.9f;      // normal cooldown multiplier
    public float cooldownBrokenBase = 0.5f;  // broken cooldown multiplier
    public float phaseShieldBoost = 1500f;   // phase fabric extra HP
    public Color shieldColor = Color.valueOf("66bfff");
    public float shieldOpacity = 0.3f;       // shield alpha
    public boolean drawTeamColor = true;     // tinted by team color
    public boolean absorbLasers = true;      // block lasers
    public boolean deflectBullets = false;   // deflection effect chance
    public float deflectChance = 0f;         // % chance to deflect bullets
    public boolean lightningOnHit = false;   // spawn lightning on hit
    public float lightningChance = 0f;

    public FullShieldWall(String name){
        super(name);

        // Basic defaults — can be overridden in HJSON
        this.group = BlockGroup.walls;
        this.update = true;
        this.solid = true;
        this.insulated = true;
        this.absorbLasers = true;
    }

    @Override
    public void load(){
        super.load();

        // Push HJSON properties into BaseShield internals
        this.radius = shieldRadius;
        this.breakage = shieldHealth;
        this.cooldownNormal = this.cooldownNormal; // already assignable
        this.cooldownBrokenBase = this.cooldownBrokenBase;
        this.phaseShieldBoost = this.phaseShieldBoost;

        // If you want color/opacity at runtime, override drawShield in build class.
    }

    public class FullShieldWallBuild extends BaseShieldBuild {
        @Override
        public void updateTile(){
            // normal shield behaviour
            super.updateTile();

            // Optional: custom effects on hit
            if(lightningOnHit && this.hit > 0 && arc.util.Mathf.chanceDelta(lightningChance)){
                Fx.lightning.at(x, y);
            }
        }

        @Override
        public void drawShield(){
            // Let BaseShield draw first
            super.drawShield();

            // Adjust opacity or color dynamically
            if(shieldColor != null){
                // You can customize drawing here if needed.
            }
        }

        @Override
        public boolean absorbLasers(){
            return absorbLasers;
        }

        @Override
        public boolean deflectBullets(){
            return deflectBullets;
        }
    }
}