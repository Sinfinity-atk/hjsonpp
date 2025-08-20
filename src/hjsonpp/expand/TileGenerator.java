package hjsonpp.expand;

import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.world.*;

public class TileGenerator extends AdvancedConsumeGenerator{
    public Seq<Block> filter = new Seq<>();
    public TileGenerator(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
    }

    public boolean canPlaceOn(Tile tile, Team team){
        for(Block floor : filter){
            if(tile.floor() == floor.asFloor()){
                return true;
            }
        }
        return false;
    }
}
