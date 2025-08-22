package hjsonpp.expand;

import arc.Core;
import arc.func.Func;
import arc.math.Mathf;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.storage.CoreBlock;

public class GeneratorCoreBlock extends CoreBlock{
    public float powerProduction = 60 / 60f;

    public GeneratorCoreBlock(String name){
        super(name);
        hasPower = true;
        conductivePower= true;
        outputsPower = true;
        consumesPower = false;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("poweroutput", (GeneratorCoreBlockBuild entity) ->
                new Bar(() -> Core.bundle.format("bar.poweroutput", Strings.fixed(powerProduction * 60 + 0.0001f, 1)), () -> Pal.powerBar, () -> 1f));
        addBar("power", makePowerBalance());
    }

    public static Func<Building, Bar> makePowerBalance(){
        return entity -> new Bar(() ->
                Core.bundle.format("bar.powerbalance",
                        ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(entity.power.graph.getPowerBalance() * 60 + 0.0001f)))),
                () -> Pal.powerBar,
                () -> Mathf.clamp(entity.power.graph.getLastPowerProduced() / entity.power.graph.getLastPowerNeeded())
        );
    }

    public class GeneratorCoreBlockBuild extends CoreBuild{
        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            if(!allowUpdate()){
                enabled = false;
            }
        }

        @Override
        public float getPowerProduction(){
            return enabled ? powerProduction : 0f;
        }
    }
}
