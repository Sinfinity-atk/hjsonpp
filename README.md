![image](assets/icon.png)

# Hjson++

A library that adds new classes to work with!
Classes that are added:

- AdvancedConsumeGenerator - you can now output items/liquids and make a craft progress bar. 
variables: outputItem/Items/Liquid/Liquids, progressBar: true/false
- AdvancedHeaterGenerator - basically the same version as HeaterGenerator, but has the  same things as the previous class
variables: outputItem/Items/Liquid/Liquids, progressBar (boolean)
- TileGenerator - placeable on only specific tiles
variables: filter: []
- AdvancedCoreBlock - basically, you can use drawers
variables: drawers
- GeneratorCoreBlock - basic core block, but it can generate power
variables: powerProduction: n
- AccelTurret - a turret which have acceleration
variables: speedUpPerShoot: n, maxAccel: n, cooldownSpeed: n
- DrawTeam - drawer class. Draws -team sprite

- ## NEW!
- ColliderCrafter - basic GenericCrafter, but it outputs items/liquids with a specific chance.
variables: produceChance: n (1=100%)
- OverHeatTurret - A Turret that overheats after some shots.
variables: overHeatAmount: n, timeToCooldown: n
- AdjustableShieldWall - Basic shield wall, but you can turn it on and off., and can justify shield radius
variables: radius: n
- EffectWeapon - Weapon class where you can set a list of effects that will always be shown on the unit. 
variables: effects: [], effectInterval: n, effectX: n, effectY: n
- TiledFloor - deprecated Anuken class. Can make big floors (64x64, etc)
variables: tilingVariants: n, tilingSize: n
- AdjustableBeamNode - you can create infinite nodes from a beam node and change its angle
variables: beamDirections: [[n,n]]
Check the examples folder for more information

Wip
FullShieldWall - A shielded wall that is much more customizable. You can change Values such as Shield shape, shield color, shield radius, wall Regen, unit blocking, etc.
# WARNING
This is a personal forked repo. If you want, you can use it
