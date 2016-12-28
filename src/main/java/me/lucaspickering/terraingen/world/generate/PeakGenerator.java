package me.lucaspickering.terraingen.world.generate;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import me.lucaspickering.terraingen.util.Direction;
import me.lucaspickering.terraingen.util.Funcs;
import me.lucaspickering.terraingen.util.InclusiveRange;
import me.lucaspickering.terraingen.util.TilePoint;
import me.lucaspickering.terraingen.world.WorldBuilder;
import me.lucaspickering.terraingen.world.WorldHelper;
import me.lucaspickering.terraingen.world.tile.Tile;

public class PeakGenerator implements Generator {

    // Generation parameters
    private static final InclusiveRange PEAK_COUNT_RANGE = new InclusiveRange(7, 10);
    private static final InclusiveRange PEAK_ELEVATION_RANGE = new InclusiveRange(45, 60);
    private static final int MIN_PEAK_SEPARATION = 2; // Min distance between two peak
    private static final int SMOOTHING_SLOP = 4; // Variation in each direction for smoothing elev

    @Override
    public void generate(WorldBuilder worldBuilder, Random random) {
        final Map<TilePoint, Tile> tiles = worldBuilder.getTiles();

        // Copy the key set because we're going to be modifying it
        final Set<TilePoint> potentialPeaks = new HashSet<>(tiles.keySet());
        final Set<TilePoint> peaks = new HashSet<>();
        final int peaksToGen = PEAK_COUNT_RANGE.randomIn(random);

        while (peaks.size() < peaksToGen && !potentialPeaks.isEmpty()) {
            // Pick a random peak from the set of potential peaks
            final TilePoint peak = Funcs.randomFromCollection(random, potentialPeaks);
            peaks.add(peak); // Add it to the set

            // Get all the tiles that are too close to this one to be peaks themselves,
            // and remove them from the set of potential peaks
            final Set<TilePoint> tooClose = WorldHelper.getTilesInRange(tiles.keySet(), peak,
                                                                        MIN_PEAK_SEPARATION);
            potentialPeaks.removeAll(tooClose);
        }

        for (TilePoint peakPoint : peaks) {
            final Tile peak = tiles.get(peakPoint);
            final int peakElev = peak.elevation() + PEAK_ELEVATION_RANGE.randomIn(random);
            // Pick a random elevation for the peak and assign it
            peak.setElevation(peakElev);

            // Adjust the elevation of the adjacent tiles
            for (Map.Entry<Direction, Tile> entry : peak.adjacents().entrySet()) {
                final Direction dir = entry.getKey();
                final Tile adjTile = entry.getValue();

                // The tile on the opposite side of adjTile from the peak
                final Tile oppTile = tiles.get(dir.shift(adjTile.pos()));
                final int oppElev = oppTile != null ? oppTile.elevation() : 0;

                // Average peakElev and oppElev, then apply a small random slop
                // Random number in range [-slop, slop]
                final int slop = random.nextInt(SMOOTHING_SLOP * 2 + 1) - SMOOTHING_SLOP;
                final int adjElev = (peakElev + oppElev) / 2 + slop;
                adjTile.setElevation(adjElev);
            }
        }
    }
}
