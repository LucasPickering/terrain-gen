package me.lucaspickering.terra.world;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import me.lucaspickering.terra.Main;
import me.lucaspickering.terra.util.Direction;
import me.lucaspickering.terra.util.Funcs;
import me.lucaspickering.terra.world.util.Chunk;
import me.lucaspickering.terra.world.util.HexPoint;
import me.lucaspickering.terra.world.util.HexPointable;
import me.lucaspickering.utils.range.DoubleRange;
import me.lucaspickering.utils.range.Range;

public class Tile implements HexPointable {

    // Only used for coloring, these values aren't enforced anywhere
    private static final Range<Double> WATER_LEVEL_RANGE = new DoubleRange(0.0, 10.0);

    private static final String INFO_STRING =
        "Biome: %s%nElevation: %d%nHumidity: %d%%";
    private static final String DEBUG_INFO_STRING = "%nPos: %s%nChunk: %s%nWater: %.2f|%.2f%n";

    public enum RiverConnection {
        ENTRY, EXIT
    }

    private final HexPoint pos; // The position of this tile in the world (NOT chunk-relative)

    private Chunk chunk; // The chunk that this tile belongs to

    private Biome biome = Biome.NONE;

    private int elevation;
    private double humidity;

    private double waterLevel;
    private double totalWaterTraversed;

    private final Map<Direction, RiverConnection> riverConnections = new EnumMap<>(Direction.class);

    public Tile(HexPoint pos, Chunk chunk) {
        Objects.requireNonNull(pos);
        Objects.requireNonNull(chunk);
        this.pos = pos;
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        Objects.requireNonNull(chunk);
        this.chunk = chunk;
    }

    public final HexPoint pos() {
        return pos;
    }

    @Override
    public final HexPoint toHexPoint() {
        return pos;
    }

    /**
     * Is the given tile adjacent to this tile? Two tiles are adjacent if the distance between them
     * is exactly 1.
     *
     * @param tile the other tile (non-null)
     * @return true if this tile and the other are adjacent, false otherwise
     * @throws NullPointerException if {@code tile == null}
     */
    public final boolean isAdjacentTo(Tile tile) {
        return pos.distanceTo(tile.pos()) == 1;
    }

    public final Biome biome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        Objects.requireNonNull(biome);
        this.biome = biome;
    }

    public final int elevation() {
        return elevation;
    }

    public void setElevation(int elevation) {
        // Coerce the elevation to be a valid value
        this.elevation = World.ELEVATION_RANGE.coerce(elevation);
    }

    public final double humidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        // Coerce the elevation to be a valid value
        this.humidity = World.HUMIDITY_RANGE.coerce(humidity);
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    /**
     * Gets the elevation of the top of the water on this tile. In other words, the elevation of
     * this tile plus the water level.
     *
     * @return elevation plus water level
     */
    public double getWaterElevation() {
        return elevation + waterLevel;
    }

    public double addWater(double water) {
        if (water < 0.0) {
            throw new IllegalArgumentException(String.format("Water must be positive, was [%f]",
                                                             water));
        }
        if (!biome.isWater()) {
            waterLevel += water;
            totalWaterTraversed += water;
            return water;
        }
        return 0.0;
    }

    public double removeWater(double water) {
        if (water < 0.0) {
            throw new IllegalArgumentException(String.format("Water must be positive, was [%f]",
                                                             water));
        }
//        if (water > waterLevel) {
//            throw new IllegalArgumentException(String.format(
//                "Cannot remove [%f] water from tile; only [%f] is available", water, waterLevel));
//        }
        final double toRemove = Math.min(waterLevel, water);
        waterLevel -= toRemove;
        return toRemove;
    }

    public void clearWater() {
        waterLevel = 0.0;
    }

    public double getTotalWaterTraversed() {
        return totalWaterTraversed;
    }

    public RiverConnection getRiverConnection(Direction dir) {
        return riverConnections.get(dir);
    }

    public void addRiverConnection(Direction dir, RiverConnection conn) {
        riverConnections.put(dir, conn);
    }

    public void removeRiverConnection(Direction dir) {
        riverConnections.remove(dir);
    }

    public final Color getColor(TileColorMode colorMode) {
        switch (colorMode) {
            case ELEVATION:
                return colorMode.interpolateColor(elevation, World.ELEVATION_RANGE);
            case HUMIDITY:
                // Water tiles are always blue
                if (biome.isWater()) {
                    return Color.BLUE;
                }
                return colorMode.interpolateColor(humidity, World.HUMIDITY_RANGE);
            case WATER_LEVEL:
                // Water tiles are always black
                if (biome.isWater()) {
                    return Color.BLACK;
                }
                return colorMode.interpolateColor(getWaterLevel(), WATER_LEVEL_RANGE);
            case BIOME:
                return biome.color();
            case COMPOSITE:
                final Color elevColor = getColor(TileColorMode.ELEVATION);
                float elevBrightness = Funcs.getColorBrightness(elevColor);
                elevBrightness = (float) Math.pow(elevBrightness, 0.75); // Make it brighter

                // Scale this biome color's brightness by the brightness of the elevation color
                final Color biomeColor = biome.color();
                return new Color((int) (biomeColor.getRed() * elevBrightness),
                                 (int) (biomeColor.getGreen() * elevBrightness),
                                 (int) (biomeColor.getBlue() * elevBrightness));
            default:
                throw new IllegalArgumentException("Unknown color mode: " + colorMode);
        }
    }

    public String info() {
        // If in debug mode, display extra debug info
        final String info = String.format(INFO_STRING, biome.displayName(), elevation(),
                                          (int) (humidity() * 100));
        if (Main.instance().getDebug()) {
            return info + String.format(DEBUG_INFO_STRING, pos, chunk.getPos(),
                                        waterLevel, totalWaterTraversed);
        }
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Tile)) {
            return false;
        }

        final Tile tile = (Tile) o;
        return Objects.equals(pos, tile.pos)
               && Objects.equals(biome, tile.biome)
               && Objects.equals(elevation, tile.elevation);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return "Tile@" + pos.toString();
    }

}
