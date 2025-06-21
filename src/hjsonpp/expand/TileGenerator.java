package hjsonpp.expand;

import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class TileGenerator extends AdvancedConsumeGenerator{
    public Floor[] floors;
    public TileGenerator(String name){
        super(name);
    }
    public boolean canPlaceOn(Tile tile, Team team){
        for(Floor floor : floors){
            if(tile.floor() == floor){
                return true;
            }
        }
        return false;
    }
}
