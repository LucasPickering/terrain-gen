package me.lucaspickering.terraingen.world.generate;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import me.lucaspickering.terraingen.world.Biome;
import me.lucaspickering.terraingen.world.Continent;
import me.lucaspickering.terraingen.world.Tile;
import me.lucaspickering.terraingen.world.World;
import me.lucaspickering.terraingen.world.util.TileSet;

/**
 * Simulates rainfall and uses that to generate lakes and rivers. The general approach is as
 * follows:
 * <ul>
 * <li>Drop x liters of water on each land tile</li>
 * <li>For each tile, move its water onto each adjacent tile that is lower than it; apply this
 * to tiles in descending order of elevation</li>
 * <li>If there is no where for a tile's water to go, and its water level hits some threshold,
 * that tile becomes a lake</li>
 * <li>At each step keep track of how much water has moved across each tile; if this hits some
 * threshold, that tile becomes a river</li>
 * </ul>
 */
public class FreshWaterGenerator implements Generator {

    private static final double RAINFALL = 0.5;
    private static final double LAKE_THRESHOLD = 1.0;
    private static final double RIVER_THRESHOLD = 10.0;

    @Override
    public void generate(World world, Random random) {
        // Generate on a continent-by-continent basis for efficiency purposes
        for (Continent continent : world.getContinents()) {
            generateForContinent(continent.getTiles());
        }
    }

    private void generateForContinent(TileSet tiles) {
        // Get all land tiles in order of descending elevation
        final List<Tile> elevSortedTiles = tiles.stream()
            .filter(t -> !t.biome().isWater()) // Filter out water tiles
            .sorted((t1, t2) -> Integer.compare(t2.elevation(), t1.elevation()))
            .collect(Collectors.toList());

        elevSortedTiles.forEach(t -> t.addWater(RAINFALL));
        for (Tile tile : elevSortedTiles) {
            spreadWaterDownhill(tiles, tile);
        }

        // Convert all appropriate tiles to lakes
        for (Tile tile : elevSortedTiles) {
            if (tile.getWaterLevel() >= LAKE_THRESHOLD) {
                tile.setBiome(Biome.LAKE);
            }
        }

        // Add rivers based on water traversal patterns
        // TODO
    }

    /**
     * Moves all the water on the given tile to tiels that are adjacent to and at a lower
     * elevation than that tile.
     *
     * @param allTiles used to find adjacent tiles
     * @param tile     the tile to spread water from
     */
    private void spreadWaterDownhill(TileSet allTiles, Tile tile) {
        // Get all tiles adjacent to this one with a lower elevation
        final TileSet lowerTiles = allTiles.getAdjacentTiles(tile).values().stream()
            .filter(adj -> adj.elevation() < tile.elevation())
            .collect(Collectors.toCollection(TileSet::new));

        // If there are any lower tiles to pass water onto, so that
        if (lowerTiles.size() > 0) {
            final double totalElevDiff = lowerTiles.stream()
                .mapToInt(Tile::elevation)
                .sum();
            // Amount of water to pass on to each lower tile
            final double waterToSpread = tile.getWaterLevel();
            for (Tile adjTile : lowerTiles) {
                // Runoff is distributed proportional to elevation difference
                final double runoffPercentage = (tile.elevation() - adjTile.elevation()) /
                                                totalElevDiff;
                adjTile.addWater(waterToSpread * runoffPercentage);
            }
            tile.clearWater(); // Remove all water from this tile
        }
    }

    /**
     * Spreads water from the given tile to
     */
    private void spreadWaterUphill() {

    }
}