package org.jtrim.swing.component;

/**
 *
 * @author Kelemen Attila
 */
public final class RenderingArgs<ArgType, DataType> {
    private final ArgType arg;
    private final DataType data;

    public RenderingArgs(ArgType arg, DataType data) {
        this.arg = arg;
        this.data = data;
    }

    public ArgType getArg() {
        return arg;
    }

    public DataType getData() {
        return data;
    }
}
