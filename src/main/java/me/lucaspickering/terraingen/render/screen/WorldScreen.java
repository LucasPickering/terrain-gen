package me.lucaspickering.terraingen.render.screen;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.lucaspickering.terraingen.render.ColorTexture;
import me.lucaspickering.terraingen.render.event.KeyEvent;
import me.lucaspickering.terraingen.render.event.MouseButtonEvent;
import me.lucaspickering.terraingen.render.screen.gui.MouseTextBox;
import me.lucaspickering.terraingen.util.Funcs;
import me.lucaspickering.terraingen.util.Point;
import me.lucaspickering.terraingen.util.TilePoint;
import me.lucaspickering.terraingen.world.World;
import me.lucaspickering.terraingen.world.WorldHelper;
import me.lucaspickering.terraingen.world.tile.Tile;

public class WorldScreen extends Screen {

    // Maximum time a click can be held down to be considered a click and not a drag
    private static final int MAX_CLICK_TIME = 250;

    private static final float OUTLINE_WIDTH = 1.5f;

    private final World world;
    private final MouseTextBox mouseOverTileInfo;

    // The last position of the mouse while dragging. Null if not dragging.
    private Point lastMouseDragPos;

    // The time at which the user pressed the mouse button down
    private long mouseDownTime;

    public WorldScreen(World world) {
        this.world = world;
        mouseOverTileInfo = new MouseTextBox();
        mouseOverTileInfo.setVisible(false); // Hide this for now
        addGuiElement(mouseOverTileInfo);
    }

    @Override
    public void draw(Point mousePos) {
        // If the mouse is being dragged, shift the world getCenter based on it
        if (lastMouseDragPos != null) {
            // Shift the world
            final Point diff = mousePos.minus(lastMouseDragPos);
            world.setWorldCenter(world.getWorldCenter().plus(diff));
            lastMouseDragPos = mousePos; // Update the mouse pos
        }
        final Point worldCenter = world.getWorldCenter();

        final Map<TilePoint, Tile> tileMap = world.getTiles();

        // Get all the tiles that are on-screen (those are the ones that will be drawn)
        final List<Tile> onScreenTiles = tileMap.values().stream()
            .filter(this::containsTile)
            .collect(Collectors.toList());

        // Draw each tile. For each one, check if it is the
        final Point shiftedMousePos = mousePos.minus(worldCenter);
        final TilePoint mouseOverPos = WorldHelper.pixelToTile(shiftedMousePos);

        // Draw each tile
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldCenter.x(), worldCenter.y(), 0f);

            // Draw the tiles themselves
            onScreenTiles.forEach(this::drawTile);

            // Draw the overlays
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            onScreenTiles.forEach(tile -> drawTileOverlays(tile, tile.pos().equals(mouseOverPos)));
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);

            GL11.glPopMatrix();
        }

        // Update mouseOverTileInfo for the tile that the mouse is over. This HAS to be done
        // after all the tiles are drawn, otherwise it would be underneath some of them.
        final Tile mouseOverTile = tileMap.get(mouseOverPos);
        if (mouseOverTile != null) {
            // Set the text and show the element
            mouseOverTileInfo.setText(mouseOverTile.info()).setVisible(true);
        }

        super.draw(mousePos); // Draw GUI elements
        mouseOverTileInfo.setVisible(false); // Hide the tile info, to be updated on the next frame
    }

    private boolean containsTile(Tile tile) {
        // If any of the 4 corners of the tile are on-screen, the tile is on-screen
        final Point worldCenter = world.getWorldCenter();
        return contains(worldCenter.plus(tile.getTopLeft()))
               || contains(worldCenter.plus(tile.getTopRight()))
               || contains(worldCenter.plus(tile.getBottomRight()))
               || contains(worldCenter.plus(tile.getBottomLeft()));
    }

    /**
     * Draws the given tile.
     *
     * @param tile the tile to draw
     */
    private void drawTile(Tile tile) {
        // Shift to the tile and draw the background
        GL11.glPushMatrix();
        GL11.glTranslatef(tile.getCenter().x(), tile.getCenter().y(), 0f);
        drawTileBackground(tile);
        // Could draw tile outlines here
        GL11.glPopMatrix();
    }

    private void drawTileBackground(Tile tile) {
        // Set the color then draw a hexagon
        Funcs.setGlColor(tile.backgroundColor());
        GL11.glBegin(GL11.GL_POLYGON);
        for (Point vertex : Tile.VERTICES) {
            GL11.glVertex2i(vertex.x(), vertex.y());
        }
        GL11.glEnd();
    }

    private void drawTileOutline(Tile tile) {
        for (int i = 0; i < Tile.NUM_SIDES; i++) {
            // Get the two vertices that the line will be between
            final Point vertex1 = Tile.VERTICES[i];
            final Point vertex2 = Tile.VERTICES[(i + 1) % Tile.NUM_SIDES];

            // The line width is based on the elevation between this tile and the adjacent one
            GL11.glLineWidth(OUTLINE_WIDTH);
            Funcs.setGlColor(tile.outlineColor());
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2i(vertex1.x(), vertex1.y());
            GL11.glVertex2i(vertex2.x(), vertex2.y());
            GL11.glEnd();
        }
    }

    /**
     * Draw the appropriate overlays for the given tile.
     *
     * @param tile      the tile to draw
     * @param mouseOver is the mouse currently over this tile?
     */
    private void drawTileOverlays(Tile tile, boolean mouseOver) {
        // Translate to this tile
        GL11.glPushMatrix();
        GL11.glTranslatef(tile.getTopLeft().x(), tile.getTopLeft().y(), 0f);

        // If the mouse is over this tile, draw the mouse-over overlay
        if (mouseOver) {
            ColorTexture.mouseOver.draw(0, 0, Tile.WIDTH, Tile.HEIGHT);
        }

        GL11.glPopMatrix();
    }

    @Override
    public void onKey(KeyEvent event) {
        if (event.action == GLFW.GLFW_RELEASE) {
            switch (event.key) {
                case GLFW.GLFW_KEY_ESCAPE:
                    setNextScreen(new PauseScreen(this)); // Open the pause menu
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event) {
        if (event.button == GLFW.GLFW_MOUSE_BUTTON_1) {
            if (event.action == GLFW.GLFW_PRESS) {
                lastMouseDragPos = event.mousePos;
                mouseDownTime = System.currentTimeMillis();
            } else if (event.action == GLFW.GLFW_RELEASE) {
                // If the elapsed time between mouse down and up is below a threshold, call it a click
                if (System.currentTimeMillis() - mouseDownTime <= MAX_CLICK_TIME) {
                    super.onClick(event);
                }
                lastMouseDragPos = null; // Wipe this out
            }
        }
    }
}
