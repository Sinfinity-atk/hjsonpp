package hjsonpp.expand;

import arc.Core;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import mindustry.entities.bullet.BulletType;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.ItemTurret;

public class OverHeatTurret extends ItemTurret{
    public float overheatAmount = 25f;

    public OverHeatTurret(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("overHeat", (OverHeatTurret.OverHeatTurretBuild entity) ->
                new Bar(
                        () -> Core.bundle.format("bar.overHeat", Strings.autoFixed((entity.overHeat)* 100, 0)),
                        () -> Pal.redDust,
                        () -> entity.overHeat / overheatAmount
                )
        );
    }

    public class  OverHeatTurretBuild extends ItemTurretBuild{
        public float overHeat = 0;
        @Override
        public void updateTile() {
            //coolDown progress
            if (!isShooting() || !hasAmmo()){
                overHeat = Mathf.lerpDelta(overHeat, 0, 1 / 20);
            }
            super.updateTile();
        }

        @Override
        public void shoot(BulletType type){
            if(overHeat <= overheatAmount){
                super.shoot(type);
                overHeat += 0.35f * Time.delta;
            }else{
                overHeat = 0;
            }
        }

    }
}
