package net.dongliu.proxy.ui.ico;

class Unsigns {

    public static int ensure(int value) {
        if (value < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return value;
    }
}
