package hjsonpp.expand;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;

public class DrawTeam extends DrawBlock{
    public String suffix = "-team";
    public Team team = Team.sharded;
    public TextureRegion region;

    @Override
    public void draw(Building build){
        Draw.color(team.color);
        Draw.rect(region, build.x, build.y, build.rotation);
        Draw.reset();
    }

    @Override
    public void load(Block block){
        region = Core.atlas.find(block.name + suffix);
    }
}
