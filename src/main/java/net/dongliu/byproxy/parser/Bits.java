package net.dongliu.byproxy.parser;

/**
 * @author Liu Dong
 */
class Bits {
    /**
     * Get the bit value as pos, pos count from low to high
     */
    static int getBit(int value, int pos) {
        return (value >> pos) & 1;
    }

    /**
     * If the bit is set, pos count from low to high
     */
    static boolean bitSet(int value, int pos) {
        return ((value >> pos) & 1) == 1;
    }
}
