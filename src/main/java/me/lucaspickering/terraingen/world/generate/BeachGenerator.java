package me.lucaspickering.terraingen.world.generate;

import java.util.Random;

import me.lucaspickering.terraingen.world.Biome;
import me.lucaspickering.terraingen.world.WorldBuilder;
import me.lucaspickering.terraingen.world.tile.Tile;

public class BeachGenerator implements Generator {

    // Generation parameters
    // Any tile <= this elevation that borders ocean will become beach
    private static final int MAX_BEACH_ELEV = 5;

    @Override
    public void generate(WorldBuilder worldBuilder, Random random) {
        for (Tile tile : worldBuilder.getTiles().values()) {
            final Biome biome = tile.biome();
            // No biome set, or already land, and within our elevation bound. Check the adjacent
            // tiles, and if there is an ocean tile adjacent, make this a beach.
            if ((biome == null || biome.isLand()) && tile.elevation() <= MAX_BEACH_ELEV) {
                for (Tile adj : tile.adjacents().values()) {
                    if (adj.biome() == Biome.OCEAN) {
                        // Possibly set elevation here to 0?
                        tile.setBiome(Biome.BEACH);
                        break; // Done with this tile
                    }
                }
            }
        }
    }
}
