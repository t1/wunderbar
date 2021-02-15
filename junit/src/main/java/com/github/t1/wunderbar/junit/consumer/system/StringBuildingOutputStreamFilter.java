package com.github.t1.wunderbar.junit.consumer.system;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class StringBuildingOutputStreamFilter extends FilterOutputStream {
    private final StringBuilder stringBuilder;

    public StringBuildingOutputStreamFilter(OutputStream outputStream, StringBuilder stringBuilder) {
        super(outputStream);
        this.stringBuilder = stringBuilder;
    }

    @Override public void write(int b) throws IOException {
        this.stringBuilder.append((char) b);
        super.write(b);
    }
}
