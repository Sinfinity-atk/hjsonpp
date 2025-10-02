package hjsonpp.expand;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;

import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.content.Fx;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import arc.scene.ui.layout.Table;

import java.util.Locale;

/**
 * Full Shield Wall - hybrid of Wall + Shield behavior.
 * Uses buildup like Force Projector: when buildup exceeds capacity, shield breaks, then recharges.
 */
public class FullShieldWall extends Wall {

    // tunables via HJSON
    public float shieldRadius = 60f;           
    public float shieldHealthCustom = 4000f;   // shield capacity (max buildup)
    public float regenPerSec = 20f;            
    public float wallRegenPerSec = 0f;         
    public String shieldColor = "ffffff";
    public float shieldOpacity = 1f;

    public boolean blockUnits = true;          
    public String blockUnitsFrom = "shield";   

    // --- NEW FIELDS ---
    public String shieldShape = "square";      
    public int shieldBlockRadius = 2;          
    public float shieldBlockRadiusAmount = 1f; 

    // buildup system params
    public float cooldownNormal = 1f;          
    public float cooldownBrokenBase = 0.25f;   

    public FullShieldWall(String name) {
        super(name);
        group = BlockGroup.walls;
        solid = true;
        update = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, String.format(Locale.ROOT, "Shield HP: %.0f", shieldHealthCustom));
        stats.add(Stat.abilities, String.format(Locale.ROOT, "Shield Repair %.0f/s", regenPerSec));
        if (wallRegenPerSec > 0f) {
            stats.add(Stat.abilities, String.format(Locale.ROOT, "Wall Repair %.0f/s", wallRegenPerSec));
        }
    }

    public class FullShieldWallBuild extends WallBuild {
        public float buildup = 0f;     
        public boolean broken = false; 
        public Color colorCached;

        @Override
        public void updateTile() {
            // buildup decays over time
            if(buildup > 0){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;
                buildup -= Time.delta * scale;
                if(buildup < 0) buildup = 0;
            }

            // restore shield if broken and cooled
            if(broken && buildup <= 0){
                broken = false;
            }

            float r = computeShieldRadius();

            // bullet blocking only if shield is up
            if(r > 0f && !broken){
                Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, (Bullet b) -> {
                    if(b.team == team) return;
                    float dx = b.x - x;
                    float dy = b.y - y;
                    if(dx * dx + dy * dy > r * r) return;

                    b.remove();
                    buildup += b.damage;

                    if(buildup >= shieldHealthCustom && !broken){
                        broken = true;
                        buildup = shieldHealthCustom;
                        Fx.shieldBreak.at(x, y, r, team.color);
                    }
                });
            }

            // wall HP regen
            if(wallRegenPerSec > 0 && health < maxHealth){
                health = Math.min(maxHealth, health + wallRegenPerSec * Time.delta / 60f);
            }

            // unit blocking only if shield is up
            if(blockUnits && !broken){
                if("shield".equalsIgnoreCase(blockUnitsFrom) || "both".equalsIgnoreCase(blockUnitsFrom)){
                    if(r > 0f){
                        Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, (Unit unit) -> {
                            float overlapDst = (unit.hitSize / 2f + r) - unit.dst(this);

                            if(overlapDst > 0){
                                if(overlapDst > unit.hitSize * 1.5f){
                                    unit.kill();
                                }else{
                                    unit.vel.setZero();
                                    unit.move(Tmp.v1.set(unit).sub(this).setLength(overlapDst + 0.01f));

                                    if(Mathf.chanceDelta(0.12f * Time.delta)){
                                        Fx.circleColorSpark.at(unit.x, unit.y, team.color);
                                    }
                                }
                            }
                        });
                    }
                }
                if("block".equalsIgnoreCase(blockUnitsFrom) || "both".equalsIgnoreCase(blockUnitsFrom)){
                    float br = block.size * 8f;
                    Units.nearbyEnemies(team, x - br, y - br, br * 2f, br * 2f, (Unit unit) -> {
                        float overlapDst = (unit.hitSize / 2f + br) - unit.dst(this);

                        if(overlapDst > 0){
                            if(overlapDst > unit.hitSize * 1.5f){
                                unit.kill();
                            }else{
                                unit.vel.setZero();
                                unit.move(Tmp.v1.set(unit).sub(this).setLength(overlapDst + 0.01f));

                                if(Mathf.chanceDelta(0.12f * Time.delta)){
                                    Fx.circleColorSpark.at(unit.x, unit.y, team.color);
                                }
                            }
                        }
                    });
                }
            }
        }

        // --- damage handling with buildup ---
        @Override
        public void damage(float damage){
            if(!broken){
                buildup += damage;
                if(buildup >= shieldHealthCustom){
                    broken = true;
                    buildup = shieldHealthCustom;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                }
            }else{
                super.damage(damage);
            }
        }

        @Override
        public void damagePierce(float damage){
            if(!broken){
                buildup += damage;
                if(buildup >= shieldHealthCustom){
                    broken = true;
                    buildup = shieldHealthCustom;
                    Fx.shieldBreak.at(x, y, computeShieldRadius(), team.color);
                }
            }else{
                super.damagePierce(damage);
            }
        }

        // --- compute shield radius based on block size + shieldBlockRadius ---
        private float computeShieldRadius() {
            float base = block.size * 8f; 
            if (shieldBlockRadius == 1) {
                return base * shieldBlockRadiusAmount; 
            } else if (shieldBlockRadius == 2) {
                return base; 
            } else if (shieldBlockRadius == 3) {
                return base * (1f + shieldBlockRadiusAmount); 
            } else {
                return shieldRadius; 
            }
        }

        @Override
        public void draw() {
            super.draw();

            if(broken) return;

            float r = computeShieldRadius();
            if(r <= 0f) return;

            if(colorCached == null) {
                try {
                    colorCached = Color.valueOf(FullShieldWall.this.shieldColor);
                } catch (Exception ex) {
                    colorCached = Color.white;
                }
            }

            Draw.z(Layer.shields);
            Draw.color(colorCached, shieldOpacity);

            if ("square".equalsIgnoreCase(shieldShape)) {
                Fill.square(x, y, r);
            } else {
                Fill.circle(x, y, r);
            }

            Draw.reset();
        }

        // --- Info Bars ---
        @Override
        public void displayBars(Table table) {
            super.displayBars(table);

            table.add(new Bar(
                    () -> broken ? "Shield Broken" : "Shield Active",
                    () -> broken ? Color.valueOf("ff7777") : Color.valueOf("77ff77"),
                    () -> 1f - (buildup / shieldHealthCustom)
            )).row();

            if (wallRegenPerSec > 0f) {
                table.add(new Bar(
                        () -> "Wall Repair " + (int) wallRegenPerSec + "/s",
                        () -> Color.valueOf("77ff77"),
                        () -> 1f
                )).row();
            }
			            // Shield Repair Speed
					if (wallRegenPerSec > 0f) {
            table.add(new Bar(
                    () -> "Shield Repair " + (int) regenPerSec + "/s",
                    () -> Color.valueOf("77ff77"),
                    () -> 1f
            )).row();
					}
        }
    }
}
