package hjsonpp.expand;

import arc.math.Mathf;
import arc.util.io.*;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

//just a version where it can output. check advanced consume generator
public class AdvancedHeaterGenerator extends AdvancedConsumeGenerator {
    public float heatOutput = 10.0F;
    public float warmupRate = 0.15F;

    public AdvancedHeaterGenerator(String name) {
        super(name);

        drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        rotateDraw = false;
        rotate = true;
        canOverdrive = false;
        drawArrow = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (AdvancedHeaterGeneratorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat / heatOutput));
    }

    public class AdvancedHeaterGeneratorBuild extends AdvancedConsumeGenerator.AdvancedConsumeGeneratorBuild implements HeatBlock {
        public float heat;

        @Override
        public void updateTile(){
            super.updateTile();

            //heat approaches target at the same speed regardless of efficiency
            heat = Mathf.approachDelta(heat, heatOutput * efficiency, warmupRate * delta());
        }

        @Override
        public float heatFrac(){
            return heat / heatOutput;
        }

        @Override
        public float heat(){
            return heat;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
        }
    }
}