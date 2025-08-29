package hjsonpp.expand;

import arc.math.Mathf;
import arc.util.Time;
import hjsonpp.expand.meta.AdditionalStats;
import mindustry.world.blocks.production.GenericCrafter;

public class ColiderCrafter extends GenericCrafter{
    // what chance it to produce item/liquid. 1 = 100%
    public double produceChance = 0.5f;

    public ColiderCrafter(String name){
        super(name);
    }

    @Override
    public void setStats(){
        stats.add(AdditionalStats.produceChance, produceChance * 100f + "%");
        super.setStats();
    }

    public boolean chance(double produceChance){
        return Mathf.chance(produceChance);
    }

    public class ColiderCrafterBuild extends  GenericCrafterBuild{
        @Override
        public void craft(){
           consume();
           boolean chanced = Mathf.chance(produceChance);
            if(outputItems != null&chanced){
                for(var output : outputItems){
                    for(int i = 0; i < output.amount; i++){
                        offload(output.item);
                    }
                }
            }
            if(wasVisible&chanced){
                craftEffect.at(x, y);
            }
            progress %= 1f;
        }
    }
}
