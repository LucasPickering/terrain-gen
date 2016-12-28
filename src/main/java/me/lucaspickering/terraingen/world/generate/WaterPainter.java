package me.lucaspickering.terraingen.world.generate;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import me.lucaspickering.terraingen.world.Biome;
import me.lucaspickering.terraingen.world.WorldBuilder;
import me.lucaspickering.terraingen.world.tile.Tile;

/**
 * Paint tiles to be oceans and lakes. All tiles with negative elevation become candidates for
 * having water. Clusters of negative tiles over a certain size are guaranteed to become oceans.
 * All other clusters have a chance of becoming lakes. The larger the cluster, the higher the
 * chance.
 */
public class WaterPainter implements Generator {

    // Generation parameters
    // Minimum size of a cluster to be considered an ocean
    private static final int MIN_OCEAN_SIZE = 10;

    @Override
    public void generate(WorldBuilder worldBuilder, Random random) {
        // First isolate all tiles with negative elevations
        Set<Tile> candidates = worldBuilder.getTiles().values().stream()
            .filter(b -> b.elevation() < 0) // Filter down to only negative tiles
            .collect(Collectors.toSet()); // Collect to a list

        // All the tiles that have been put in a cluster
        List<Set<Tile>> clusters = new LinkedList<>();

        Set<Tile> currentCluster = null;
        while (!candidates.isEmpty()) {
            // Start a new cluster
            if (currentCluster == null) {
                currentCluster = new HashSet<>();
            }

            // Done with this cluster, add it to the list then get ready for a new one
            if (true) {
                clusters.add(currentCluster);
                currentCluster = null;
            }
        }

        for (Set<Tile> cluster : clusters) {
            // If this cluster is over the min ocean size, make everything in it an ocean
            if (cluster.size() >= MIN_OCEAN_SIZE) {
                for (Tile tile : cluster) {
                    tile.setBiome(Biome.OCEAN);
                }
            }
        }
    }
}
