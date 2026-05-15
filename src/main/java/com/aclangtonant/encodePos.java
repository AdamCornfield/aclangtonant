package com.aclangtonant;

/**
 * Simple function which is used to encode the x and y coordinates into one long integer.
 * It works by converting the xPos into a long and storing the xPos as the first 32 bits of this value.
 * It then combines the yPos which takes the last 32 bits of the long, creating a 64 bit value with the two coordinates.
 * 
 * The getter for x will then fetch the value from the first 32 bits.
 * The y value getter will just convert it to an integer which keeps the last 32 bits.
 */
public class encodePos {
    public static long encode(int xPos, int yPos) {
        return (((long) xPos) << 32) | (yPos & 0xffffffffL);
    }

    public static int getXPos(long key) {
        return (int) (key >> 32);
    }

    public static int getYPos(long key) {
        return (int) key;
    }
}
