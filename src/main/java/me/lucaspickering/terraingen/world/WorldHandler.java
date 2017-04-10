package me.lucaspickering.terraingen.world;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import me.lucaspickering.terraingen.render.Renderer;
import me.lucaspickering.terraingen.world.generate.BeachGenerator;
import me.lucaspickering.terraingen.world.generate.BiomePainter;
import me.lucaspickering.terraingen.world.generate.ContinentClusterer;
import me.lucaspickering.terraingen.world.generate.Generator;
import me.lucaspickering.terraingen.world.generate.NoiseElevationGenerator;
import me.lucaspickering.terraingen.world.generate.NoiseHumidityGenerator;
import me.lucaspickering.terraingen.world.generate.WaterPainter;
import me.lucaspickering.terraingen.world.util.TilePoint;
import me.lucaspickering.utils.Point;
import me.lucaspickering.utils.range.DoubleRange;
import me.lucaspickering.utils.range.Range;

/**
 * A class with fields and methods that can entirely encapsulate a {@link World} and
 * perform useful operations on it. This handles functionality such as generating worlds and
 * finding tile pixel locations & sizes.
 *
 * This should be a singleton class.
 */
public class WorldHandler {

    private enum Generators {

        ELEV_GENERATOR(NoiseElevationGenerator.class),
        HUMID_GENERATOR(NoiseHumidityGenerator.class),
        WATER_PAINTER(WaterPainter.class),
        BIOME_PAINTER(BiomePainter.class),
        CONTINENT_CLUSTERER(ContinentClusterer.class),
        BEACH_GENERATOR(BeachGenerator.class);

        private final Class<? extends Generator> clazz;

        Generators(Class<? extends Generator> clazz) {
            this.clazz = clazz;
        }

        private Generator makeGenerator() {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating generator");
            }
        }
    }

    /**
     * A tile's radius (in pixels) must be in this range (this is essentially a zoom limit)
     */
    public static final Range<Double> VALID_TILE_RADII = new DoubleRange(10.0, 200.0);

    // World size
    private static final int DEFAULT_SIZE = 200;

    private final Logger logger;
    private final long seed;
    private final int size; // Radius of the world

    // Properties of the world
    private World world;

    private Point worldCenter; // The pixel location of the center of the world

    // Tile pixel dimensions
    private double tileRadius;
    private double tileWidth;
    private double tileHeight;
    private Point[] tileVertices;

    public WorldHandler(long seed) {
        this(seed, DEFAULT_SIZE);
    }

    // Package visible for benchmarking purposes
    WorldHandler(long seed, int size) {
        this.logger = Logger.getLogger(getClass().getName());
        this.seed = seed;
        this.size = size;

        logger.log(Level.FINE, String.format("Using seed '%d'", seed));
        worldCenter = new Point(Renderer.RES_WIDTH / 2, Renderer.RES_HEIGHT / 2);
        setTileRadius(VALID_TILE_RADII.lower());
    }

    /**
     * Generates a new set of tiles to represent this world in parallel with the current thread.
     * The generation process is executed in another thread, so this method will return
     * immediately, before the generation is completed.
     */
    public void generateParallel() {
        // Launch the generation process in a new thread
        new Thread(this::generate).start();
    }

    /**
     * Generates a new set of tiles to represent this world. This method does not return until
     * the generation process is complete.
     */
    public void generate() {
        // Initialize generators outside the timer. This may get its own timer later?
        final List<Generator> generators = Arrays.stream(Generators.values()).map
            (Generators::makeGenerator).collect(Collectors.toList());

        final long startTime = System.currentTimeMillis(); // We're timing this
        final World world = new World(size);

        // Apply each generator in sequence (this is the heavy lifting)
        generators.forEach(gen -> runGenerator(gen, world));

        this.world = world.immutableCopy(); // Make an immutable copy and save it for the class
        logger.log(Level.FINE, String.format("World generation took %d ms",
                                             System.currentTimeMillis() - startTime));
    }

    private void runGenerator(Generator generator, World world) {
        final long startTime = System.currentTimeMillis();
        final Random random = new Random(seed);
        generator.generate(world, random);
        final long runTime = System.currentTimeMillis() - startTime;
        logger.log(Level.FINER, String.format("Generator stage %s took %d ms",
                                              generator.getClass().getSimpleName(), runTime));
    }

    /**
     * Gets the current world for this handler. No copy is made, but the returned object is
     * immutable.
     *
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    public Point getWorldCenter() {
        return worldCenter;
    }

    public void setWorldCenter(Point worldCenter) {
        this.worldCenter = worldCenter;
    }

    public double getTileRadius() {
        return tileRadius;
    }

    public void setTileRadius(double radius) {
        tileRadius = VALID_TILE_RADII.coerce(radius);
        tileWidth = tileRadius * 2;
        tileHeight = Math.sqrt(3) * tileRadius;
        tileVertices = new Point[]{
            new Point(-tileWidth / 4, -tileHeight / 2),
            new Point(tileWidth / 4, -tileHeight / 2),
            new Point(tileRadius, 0),
            new Point(tileWidth / 4, tileHeight / 2),
            new Point(-tileWidth / 4, tileHeight / 2),
            new Point(-tileRadius, 0)
        };
    }

    public double getTileWidth() {
        return tileWidth;
    }

    public double getTileHeight() {
        return tileHeight;
    }

    public Point[] getTileVertices() {
        return tileVertices;
    }

    public final Point getTileCenter(Tile tile) {
        return tileToPixel(tile.pos());
    }

    public final Point getTileTopLeft(Point tileCenter) {
        return tileCenter.plus(-getTileWidth() / 2, -getTileHeight() / 2);
    }

    public Point getTileTopRight(Point tileCenter) {
        return tileCenter.plus(getTileWidth() / 2, -getTileHeight() / 2);
    }

    public Point getTileBottomRight(Point tileCenter) {
        return tileCenter.plus(getTileWidth() / 2, getTileHeight() / 2);
    }

    public Point getTileBottomLeft(Point tileCenter) {
        return tileCenter.plus(-getTileWidth() / 2, getTileHeight() / 2);
    }


    /**
     * Converts a {@link TilePoint} in this world to a {@link Point} on the screen.
     *
     * @param tile the position of the tile as a {@link TilePoint}
     * @return the position of that tile's center on the screen
     */
    @NotNull
    public Point tileToPixel(@NotNull TilePoint tile) {
        final double x = getTileWidth() * tile.x() * 0.75;
        final double y = -getTileHeight() * (tile.x() / 2.0 + tile.y());
        return getWorldCenter().plus(x, y);
    }

    /**
     * Converts a {@link Point} on the screen to a {@link TilePoint} in this world. The returned
     * point is the location of the tile that contains the given screen point. It doesn't
     * necessarily exist in this world; it is just the position of a theoretical tile that could
     * exist there. The given point does not need to be shifted based on the world center before
     * calling this function.
     *
     * @param pos any point on the screen
     * @return the position of the tile that encloses the given point
     */
    @NotNull
    public TilePoint pixelToTile(@NotNull Point pos) {
        final Point shiftedPos = pos.minus(getWorldCenter());
        // Convert it to a fractional tile point
        final double fracX = shiftedPos.x() * 4.0 / 3.0 / getTileWidth();
        final double fracY = -(shiftedPos.x() + Math.sqrt(3.0) * shiftedPos.y())
                             / (getTileRadius() * 3.0);
        final double fracZ = -fracX - fracY; // We'll need this later

        // Return the rounded point
        return TilePoint.roundPoint(fracX, fracY, fracZ);
    }
}
