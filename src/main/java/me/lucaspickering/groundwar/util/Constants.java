package me.lucaspickering.groundwar.util;

public class Constants {

    // Tile constants
    /**
     * The distance between the center point of the hexagon and the center-point of one side of the
     * hexagon.
     */
    public static final int TILE_RADIUS = 100;
    public static final int TILE_WIDTH = (int) (TILE_RADIUS * 4 / Math.sqrt(3));
    public static final int TILE_HEIGHT = TILE_RADIUS * 2;
    public static final int NUM_SIDES = Direction.values().length;

    // Renderer constants
    /**
     * The width of the window that will be assumed when all textures, words, etc. are drawn to the
     * screen. Everything will be rendered to this resolution, then scaled to the actual resolution.
     */
    public static final int RES_WIDTH = 3840;
    /**
     * @see #RES_WIDTH
     */
    public static final int RES_HEIGHT = 2160;

    // World constants
    public static final Point BOARD_CENTER = new Point((RES_WIDTH - TILE_WIDTH) / 2,
                                                       (RES_HEIGHT - TILE_HEIGHT) / 2 + 75);

    // File paths
    public static final String TEXTURE_PATH = "/textures/%s.png";
    public static final String FONT_PATH = "/fonts/%s.ttf";
    public static final String SAVE_PATH = "/saves/%s.csv";

    // Texture names
    public static final String TILE_BG_NAME = "tile_background";
    public static final String TILE_OUTLINE_NAME = "tile_outline";
    public static final String BUTTON_NAME = "button";

    // Font names
    public static final String FONT1 = "bombardier";

    // Font sizes
    public static final float FONT_SIZE_TILE = 60f;
    public static final float FONT_SIZE_UI = 100f;
    public static final float FONT_SIZE_UI_LARGE = 150f;
    public static final float FONT_SIZE_TITLE = 250f;

}
