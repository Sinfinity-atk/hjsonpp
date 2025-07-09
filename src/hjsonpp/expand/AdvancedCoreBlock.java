package hjsonpp.expand;

import arc.util.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.draw.DrawBlock;

public class AdvancedCoreBlock extends CoreBlock {
    public @Nullable DrawBlock drawer;
    public AdvancedCoreBlock(String name){
        super(name);
    }

    public class AdvancedCoreBuild extends CoreBuild{
        @Override
        public void draw(){
            drawer.draw(this);
        }
    }
}