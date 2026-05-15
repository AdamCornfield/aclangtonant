package com.aclangtonant;

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
