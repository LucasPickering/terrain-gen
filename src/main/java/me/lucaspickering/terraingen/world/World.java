package me.lucaspickering.terraingen.world;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.lucaspickering.terraingen.TerrainGen;
import me.lucaspickering.terraingen.render.Renderer;
import me.lucaspickering.terraingen.util.InclusiveRange;
import me.lucaspickering.terraingen.util.Point;
import me.lucaspickering.terraingen.world.generate.BeachGenerator;
import me.lucaspickering.terraingen.world.generate.BiomePainter;
import me.lucaspickering.terraingen.world.generate.Generator;
import me.lucaspickering.terraingen.world.generate.LandRougher;
import me.lucaspickering.terraingen.world.generate.OceanFloorGenerator;
import me.lucaspickering.terraingen.world.generate.PeakGenerator;
import me.lucaspickering.terraingen.world.generate.WaterPainter;

public class World {

    // Every tile's elevation must be in this range
    public static final InclusiveRange ELEVATION_RANGE = new InclusiveRange(-50, 75);

    // World size
    private static final int DEFAULT_SIZE = 50;

    private static final Generator[] GENERATORS = new Generator[]{
        new BiomePainter(),
        new OceanFloorGenerator(),
        new LandRougher(),
        new PeakGenerator(),
        new WaterPainter(),
        new BeachGenerator()
    };

    private final Logger logger;
    private final Random random;
    private final Tiles tiles;

    // The pixel location of the center of the world
    private Point worldCenter;

    public World() {
        this(DEFAULT_SIZE);
    }

    // Package visible for benchmarking purposes
    World(int size) {
        logger = Logger.getLogger(getClass().getName());
        random = TerrainGen.instance().random();
        tiles = generateWorld(size);
        worldCenter = new Point(Renderer.RES_WIDTH / 2, Renderer.RES_HEIGHT / 2);
    }

    private Tiles generateWorld(int size) {
        final long startTime = System.currentTimeMillis(); // We're timing this
        final Tiles tiles = WorldHelper.initTiles(size);

        // Apply each generator in sequence (this is the heavy lifting)
        Arrays.stream(GENERATORS).forEach(gen -> gen.generate(tiles, random));

        final Tiles result = tiles.immutableCopy(); // Make an immutable copy
        logger.log(Level.FINE, String.format("World generation took %d ms",
                                             System.currentTimeMillis() - startTime));
        return result;
    }

    /**
     * Gets the world's tiles. No copy is made, but the returned object is immutable. Its
     * internal objects (tiles, etc.) may be mutable though, so do not change them!
     *
     * @return the world's tiles
     */
    public Tiles getTiles() {
        return tiles;
    }

    public Point getWorldCenter() {
        return worldCenter;
    }

    public void setWorldCenter(Point worldCenter) {
        this.worldCenter = worldCenter;
    }
}
