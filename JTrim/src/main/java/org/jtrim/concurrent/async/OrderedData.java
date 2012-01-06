package org.jtrim.concurrent.async;

public final class OrderedData<DataType> {

    private final long index;
    private final DataType rawData;

    public OrderedData(long index, DataType rawData) {
        this.index = index;
        this.rawData = rawData;
    }

    public long getIndex() {
        return index;
    }

    public DataType getRawData() {
        return rawData;
    }

    @Override
    public String toString() {
        return "OrderedData{" + "index=" + index + ", Data=" + rawData + '}';
    }
}
