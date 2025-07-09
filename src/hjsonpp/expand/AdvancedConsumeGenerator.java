package hjsonpp.expand;

import arc.Core;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Bar;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.meta.*;

public class AdvancedConsumeGenerator extends ConsumeGenerator{
    // make them able to output multiple items and liquids
    @Nullable
    public ItemStack outputItem;
    @Nullable
    public ItemStack[] outputItems;
    @Nullable
    public LiquidStack outputLiquid;
    @Nullable
    public LiquidStack[] outputLiquids;
    public int[] liquidOutputDirections = new int[]{-1};

    // is production bar will be displayed
    public boolean progressBar = false;

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
        if (progressBar) {
            this.addBar("hj-bar.progress", (AdvancedConsumeGeneratorBuild entity) ->
                    new Bar(
                            () -> Core.bundle.format("bar.production-progress", Strings.fixed(entity.totalProgress() / itemDuration * 100, 1)),
                            () -> Pal.accent,
                            () -> entity.totalProgress() / itemDuration
                    )
            );
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
        @Override
        public void updateTile(){
            if (AdvancedConsumeGenerator.this.outputLiquids != null) {
                float inc = this.getProgressIncrease(1.0F);

                for(LiquidStack output : AdvancedConsumeGenerator.this.outputLiquids) {
                    this.handleLiquid(this, output.liquid, Math.min(output.amount * inc, AdvancedConsumeGenerator.this.liquidCapacity - this.liquids.get(output.liquid)));
                }
            }
            this.craft();
            this.dumpOutputs();
            super.updateTile();
        }
        public void craft() {
            this.consume();
            if (AdvancedConsumeGenerator.this.outputItems != null) {
                for(ItemStack output : AdvancedConsumeGenerator.this.outputItems) {
                    for(int i = 0; i < output.amount; ++i) {
                        this.offload(output.item);
                    }
                }
            }
        }

        public void dumpOutputs() {
            if (AdvancedConsumeGenerator.this.outputItems != null && this.timer(AdvancedConsumeGenerator.this.timerDump, (float)AdvancedConsumeGenerator.this.dumpTime / this.timeScale)) {
                for(ItemStack output : AdvancedConsumeGenerator.this.outputItems) {
                    this.dump(output.item);
                }
            }

            if (AdvancedConsumeGenerator.this.outputLiquids != null) {
                for(int i = 0; i < AdvancedConsumeGenerator.this.outputLiquids.length; ++i) {
                    int dir = AdvancedConsumeGenerator.this.liquidOutputDirections.length > i ? AdvancedConsumeGenerator.this.liquidOutputDirections[i] : -1;
                    this.dumpLiquid(AdvancedConsumeGenerator.this.outputLiquids[i].liquid, 2.0F, dir);
                }
            }

        }
    }
}
