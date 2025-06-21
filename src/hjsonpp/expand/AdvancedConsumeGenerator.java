package hjsonpp.expand;

import arc.util.Nullable;
import mindustry.type.*;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class AdvancedConsumeGenerator extends ConsumeGenerator{
    @Nullable
    public ItemStack outputItem;
    @Nullable
    public ItemStack[] outputItems;
    @Nullable
    public LiquidStack outputLiquid;
    @Nullable
    public LiquidStack[] outputLiquids;
    public AdvancedConsumeGenerator(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        if (this.outputItems != null) {
            this.stats.add(Stat.output, StatValues.items(this.itemDuration, this.outputItems));
        }
        if (this.outputLiquids != null) {
            this.stats.add(Stat.output, StatValues.liquids(1.0F, this.outputLiquids));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        if (this.outputLiquids != null && this.outputLiquids.length > 0) {
            this.removeBar("liquid");

            for(LiquidStack stack : this.outputLiquids) {
                this.addLiquidBar(stack.liquid);
            }
        }
    }

    @Override
    public void init(){
        if (this.outputItems == null && this.outputItem != null) {
            this.outputItems = new ItemStack[]{this.outputItem};
        }

        if (this.outputLiquids == null && this.outputLiquid != null) {
            this.outputLiquids = new LiquidStack[]{this.outputLiquid};
        }

        if (this.outputLiquid == null && this.outputLiquids != null && this.outputLiquids.length > 0) {
            this.outputLiquid = this.outputLiquids[0];
        }

        this.outputsLiquid = this.outputLiquids != null;
        if (this.outputItems != null) {
            this.hasItems = true;
        }

        if (this.outputLiquids != null) {
            this.hasLiquids = true;
        }

        super.init();
    }

    public boolean outputsItems() {
        return this.outputItems != null;
    }

    public class AdvancedConsumeGeneratorBuild extends ConsumeGeneratorBuild{

    }
}
