package hjsonpp.expand;

import arc.util.Nullable;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;

public class DrawableBlock extends Block{
    public @Nullable DrawBlock drawer;
    public DrawableBlock(String name){
        super(name);
    }

    public class DrawableBlockBuild extends Building{
        @Override
        public void draw(){
            drawer.draw(this);
        }
    }
}
