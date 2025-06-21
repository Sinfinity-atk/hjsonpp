package hjsonpp;

import mindustry.mod.*;

public class HjsonPlusPlusMod extends Mod{

    public HjsonPlusPlusMod(){
        ClassMap.classes.put("AdvancedConsumeGenerator", hjsonpp.expand.AdvancedConsumeGenerator.class);
        ClassMap.classes.put("AdvancedHeaterGenerator", hjsonpp.expand.AdvancedHeaterGenerator.class);
        ClassMap.classes.put("TileGenerator", hjsonpp.expand.TileGenerator.class);
        ClassMap.classes.put("AccelTurret", hjsonpp.expand.AccelTurret.class);
        ClassMap.classes.put("DrawTeam", hjsonpp.expand.DrawTeam.class);
    }

}
