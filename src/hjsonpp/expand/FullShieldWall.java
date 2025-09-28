package hjsonpp.expand;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.BlockGroup;

/** A full shield wall block with adjustable stats via HJSON */
public class FullShieldWall extends BaseShield {

    // tunable values loaded from HJSON
    public float shieldRadius = 80f;
    public float shieldHealthCustom = 25000f;
    public float shieldRegen = 80f;
    public Color shieldColor = Color.valueOf("66bfff");
    public float shieldOpacity = 0.3f;
    public boolean drawTeamColor = true;

    public boolean absorbLasers = true;
    public boolean deflectBullets = false;
    public float deflectChance = 0f;
    public boolean lightningOnHit = false;
    public float lightningChance = 0f;

    public FullShieldWall(String name){
        super(name);

        this.group = BlockGroup.walls;
        this.update = true;
        this.solid = true;
        this.insulated = true;
    }

    public class FullShieldWallBuild extends BaseShieldBuild {
        // These are our own shield stats, because BaseShieldBuild stores shieldHP internally.
        public float customShieldHealth = shieldHealthCustom;
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
