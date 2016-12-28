package me.lucaspickering.terraingen.util;

import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Collection;
import java.util.Random;

public class Funcs {

    /**
     * Randomly selects one element from the given non-empty collection. Each element has an equal
     * chance of being chosen.
     *
     * @param random the {@link Random} to generate numbers from
     * @param coll   the collection to be chosen from (non-null, non-empty)
     * @param <T>    the type of the element in the collection
     * @return one randomly-selected, even-distributed element from the given collection
     */
    public static <T> T randomFromCollection(Random random, Collection<T> coll) {
        assert coll != null && !coll.isEmpty() : "Collection cannot be null or empty";

        // Select a random element from the collection. The error should never be thrown.
        return coll.stream().skip(random.nextInt(coll.size())).findFirst().orElseThrow(
            () -> new AssertionError("No random element selected despite non-empty collection"));
    }

    public static void setGlColor(Color color) {
        GL11.glColor4f(color.getRed() / 255f,
                       color.getGreen() / 255f,
                       color.getBlue() / 255f,
                       color.getAlpha() / 255f);
    }

    /**
     * Creates a {@link Color} from the given RGB code with alpha 255 (opaque).
     *
     * @param rgb the RGB code
     * @return the given color as a {@link Color} with alpha 255
     */
    public static Color colorFromRgb(int rgb) {
        return new Color(rgb); // Set alpha to 255
    }

    /**
     * Creates a {@link Color} from the given ARGB (alpha-red-green-blue) code.
     *
     * @param argb the ARGB code
     * @return the given color as a {@link Color}
     */
    public static Color colorFromArgb(int argb) {
        final int alpha = argb >> 24 & 0xff;
        final int red = argb >> 16 & 0xff;
        final int green = argb >> 8 & 0xff;
        final int blue = argb & 0xff;
        return new Color(red, green, blue, alpha);
    }

    /**
     * Converts the given color to a Hue-Saturation-Value array.
     *
     * @param color the color in RGB form
     * @return the color as a float array of Hue-Saturation-Value (in that order)
     */
    public static float[] toHSV(Color color) {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    }

    public static Color toRGB(float[] hsv) {
        return toRGB(hsv[0], hsv[1], hsv[2]);
    }

    public static Color toRGB(float hue, float saturation, float value) {
        return colorFromRgb(Color.HSBtoRGB(hue, saturation, value));
    }
}