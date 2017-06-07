package net.dongliu.byproxy.parser;

/**
 * @author Liu Dong
 */
class BitUtils {
    /**
     * Get the bit value as pos, from right to left
     */
    static int getBit(int value, int pos) {
        return (value >> pos) & 1;
    }
}
