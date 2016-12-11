package me.lucaspickering.groundwar.render.screen;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

import me.lucaspickering.groundwar.render.ColorTexture;
import me.lucaspickering.groundwar.render.HorizAlignment;
import me.lucaspickering.groundwar.render.VertAlignment;
import me.lucaspickering.groundwar.render.event.KeyEvent;
import me.lucaspickering.groundwar.render.screen.gui.TextDisplay;
import me.lucaspickering.groundwar.util.Funcs;
import me.lucaspickering.groundwar.util.Point;
import me.lucaspickering.groundwar.world.World;
import me.lucaspickering.groundwar.world.tile.Tile;

public class WorldScreen extends MainScreen {

    private static final Point TILE_INFO_POS = new Point(20, -10);
    private static final int TILE_INFO_WIDTH = 370;
    private static final int TILE_INFO_HEIGHT = 200;

    private final World world;
    private final TextDisplay mouseOverTileInfo;

    public WorldScreen(World world) {
        this.world = world;
        addGuiElement(mouseOverTileInfo = new TextDisplay(null, new Point(), 0, 0,
                                                          HorizAlignment.LEFT,
                                                          VertAlignment.BOTTOM));
        mouseOverTileInfo.setVisible(false);
    }

    @Override
    public void draw(Point mousePos) {
        final Collection<Tile> tiles = world.getTiles().values();

        tiles.forEach(tile -> drawTile(tile, mousePos)); // Draw each tile

        // Update mouseOverTileInfo for the unit that the mouse is over
        for (Tile tile : tiles) {
            if (tile.contains(mousePos)) {
                mouseOverTileInfo.setText(tile.info());
                mouseOverTileInfo.setPos(mousePos.plus(TILE_INFO_POS));
                mouseOverTileInfo.setWidth(TILE_INFO_WIDTH);
                mouseOverTileInfo.setHeight(TILE_INFO_HEIGHT);
                mouseOverTileInfo.setVisible(true);
                break; // We don't need to check the rest of the tiles
            }
        }

        super.draw(mousePos); // Draw GUI elements
        mouseOverTileInfo.setVisible(false); // Hide the unit info, to be updated on the next frame
    }

    /**
     * Draws the given tile.
     *
     * @param tile     the tile to draw
     * @param mousePos the position of the mouse
     */
    private void drawTile(Tile tile, Point mousePos) {
        GL11.glPushMatrix();

        // Translate to the center of the tile
        final Point tilePos = tile.center();
        GL11.glTranslatef(tilePos.x(), tilePos.y(), 0f);

        final int width = Tile.TILE_WIDTH;
        final int height = Tile.TILE_HEIGHT;

        // Draw the tile background
        Funcs.setGlColor(tile.backgroundColor());
        GL11.glBegin(GL11.GL_POLYGON);
        for (Point vertex : Tile.VERTICES) {
            // Draw each vertex
            GL11.glVertex2i(vertex.x(), vertex.y());
        }
        GL11.glEnd();

        // Draw the outline by drawing each side as an individual line.
        Funcs.setGlColor(tile.outlineColor());
        GL11.glLineWidth(Tile.OUTLINE_WIDTH);
        for (int i = 0; i < Tile.NUM_SIDES; i++) {
            // Get the two vertices that the line will be between
            final Point vertex1 = Tile.VERTICES[i];
            final Point vertex2 = Tile.VERTICES[(i + 1) % Tile.NUM_SIDES];
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2i(vertex1.x(), vertex1.y());
            GL11.glVertex2i(vertex2.x(), vertex2.y());
            GL11.glEnd();
        }

//        GL11.glEnable(GL11.GL_BLEND);
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
//        // Draw the regular foreground
//        renderer().drawTexture(Constants.TILE_OUTLINE_NAME, 0, 0, width, height,
//                               tile.outlineColor());
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        GL11.glDisable(GL11.GL_BLEND);

        // Translate to the top-left of the tile
        GL11.glTranslatef(-Tile.TILE_WIDTH / 2, -Tile.TILE_HEIGHT / 2, 0f);

        drawTileOverlays(tile, mousePos); // Draw the tile overlays on top of everything else

        GL11.glPopMatrix();
    }

    /**
     * Draw the appropriate overlays for the given tile.
     *
     * @param tile     the tile to draw
     * @param mousePos the position of the mouse
     */
    private void drawTileOverlays(Tile tile, Point mousePos) {
        final int width = Tile.TILE_WIDTH;
        final int height = Tile.TILE_HEIGHT;

        // Draw mouse-over overlays
        if (tile.contains(mousePos)) { // If the mouse is over this tile...
            ColorTexture.mouseOver.draw(0, 0, width, height); // Draw the mouse-over overlay
        }
    }

    @Override
    public void onKey(KeyEvent event) {
        switch (event.key) {
            case GLFW.GLFW_KEY_SPACE:
                // todo pause generation here
                break;
        }
    }
}
