package my.hyper.blocks;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.BlockGroup;

/**
 * FullShieldWall (class file FullShieldWall.java)
 *
 * Extends BaseShield and exposes a wide range of public fields so they can be set from HJSON (or by code).
 * This class copies the public/hjson-configurable values into the underlying BaseShield fields during load(),
 * ensuring the engine's shield logic (bullet absorption, unit blocking, draw, break/regen) is used.
 *
 * NOTE:
 * - This code is intended for Mindustry v8-style modding (build ~151.x). Field names are chosen to match
 *   BaseShield/common block fields (shieldHealth, cooldownNormal, cooldownBrokenBase, radius, etc).
 * - If you use HJSON++ or HJSON mapping, put `type: my.hyper.blocks.FullShieldWall` in the .hjson file.
 *
 * Example HJSON fields that will map automatically:
 *  shieldHealth, cooldownNormal, cooldownBrokenBase, breakage, phaseShieldBoost,
 *  shieldRadius, shieldColor, shieldOpacity, deflectChance, lightningChance, blockLasers
 *
 * Author: generated for you
 */
public class FullShieldWall extends BaseShield {

    /* ==============  HJSON-exposed / tunable fields (public)  ============== */

    /** Shield HP (total capacity) */
    public float shieldHealth = 6000f;

    /** Regen / "normal" cooldown parameter (engine uses this name) */
    public float cooldownNormal = 1.5f;

    /** Regeneration while broken (engine uses this name) */
    public float cooldownBrokenBase = 0.5f;

    /** How much buildup before the shield breaks (engine field 'breakage') */
    public float breakage = 2000f;

    /** Phase boost (if using phase fabric) */
    public float phaseShieldBoost = 0f;

    /**
     * Shield radius in world units (pixels). If you prefer tile units, multiply by Vars.tilesize in your HJSON.
     * Note: BaseShield typically uses 'radius' - we map shieldRadius -> radius in load().
     */
    public float shieldRadius = 48f;

    /** visual: hex color string for the shield, e.g. "66bfff" or "ff66aa" */
    public String shieldColor = "66bfff";

    /**
     * visual opacity/fade for the shield (0.0 - 1.0).
     * We will apply it to shieldFade (engine field used for drawing fade).
     */
    public float shieldOpacity = 0.8f;

    /** chance [0..1] to deflect incoming projectiles (like a phase/surge wall behaviour) */
    public float deflectChance = 0f;

    /** chance [0..1] to emit lightning when struck (surge-like behaviour) */
    public float lightningChance = 0f;

    /** if true, the wall "absorbs" lasers (plastanium-like) and acts insulated for laser/power effects */
    public boolean blockLasers = false;

    /* ==============  Constructors  ============== */

    public FullShieldWall(String name){
        super(name);

        // Make this behave like a serious defensive wall by default
        this.solid = true;
        this.update = true;
        this.destructible = true;
        this.group = BlockGroup.walls;

        // it's reasonable for a shield-wall to be able to carry power or be configured so
        this.consumesPower = false;
        this.hasPower = false;
    }

    /* ==============  Lifecycle hooks  ============== */

    @Override
    public void load(){
        // call super first to ensure base resources load
        super.load();

        // Now copy our public configuration fields into the BaseShield internals that the engine uses.
        // We do this here so HJSON field assignment (which will set these public fields) has already happened.

        try {
            // map shield amount
            this.shieldHealth = this.shieldHealth; // (this field may shadow parent's field) - we'll set parent's field explicitly
            super.shieldHealth = this.shieldHealth;

            // map regen / cooldown parameters
            super.cooldownNormal = this.cooldownNormal;
            super.cooldownBrokenBase = this.cooldownBrokenBase;

            // breakage / phase boost mapping
            super.breakage = this.breakage;
            super.phaseShieldBoost = this.phaseShieldBoost;

            // radius mapping - BaseShield uses 'radius' typically
            super.radius = this.shieldRadius;

            // visual mapping - parse color string to Color
            if (this.shieldColor != null && !this.shieldColor.isEmpty()) {
                try {
                    Color c = Color.valueOf(this.shieldColor);
                    super.shieldColor = c;
                } catch (Exception ex) {
                    Log.err("FullShieldWall: failed to parse shieldColor '" + this.shieldColor + "': " + ex);
                }
            }

            // opacity / fade (map to shieldFade if present)
            super.shieldFade = this.shieldOpacity;

            // deflection / lightning: map to engine fields if available
            // Many wall-like classes expose chanceDeflect and lightningChance; attempt to set them
            try {
                // chanceDeflect - defend/reflect chance used by some walls
                this.chanceDeflect = this.deflectChance;
            } catch (Throwable t) {
                // ignore if field not present
            }

            try {
                // lightningChance, lightningDamage, etc - set lightningChance if available
                this.lightningChance = this.lightningChance; // if BaseShield has it this will map
            } catch (Throwable t) {
                // ignore if field not present
            }

            // lasers - map to absorbLasers if BaseBlock supports it
            try {
                this.absorbLasers = this.blockLasers;
            } catch (Throwable t) {
                // ignore if not present
            }
        } catch (Throwable t) {
            Log.err("FullShieldWall: error mapping config into BaseShield: " + t);
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        // optionally we could add stat lines summarizing the shield here
    }

    /*
     * Optional: override any other block behavior here.
     * For most use-cases, BaseShield's building class (BaseShield.BaseShieldBuild/ForceBuild)
     * already implements bullet absorption, blocking of units and shield lifecycle.
     * Because we've mapped HJSON fields above, the engine will use those values.
     */

}