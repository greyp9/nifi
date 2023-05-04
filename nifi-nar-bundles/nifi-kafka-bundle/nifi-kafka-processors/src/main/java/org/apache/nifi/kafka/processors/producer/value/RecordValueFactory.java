package org.apache.nifi.kafka.processors.producer.value;

import org.apache.nifi.serialization.RecordSetWriter;
import org.apache.nifi.serialization.record.PushBackRecordSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RecordValueFactory implements ValueFactory {
    private final ByteArrayOutputStream os;
    private final RecordSetWriter writer;
    private final PushBackRecordSet recordSet;

    public RecordValueFactory(final ByteArrayOutputStream os,
                              final RecordSetWriter writer,
                              final PushBackRecordSet recordSet) {
        this.os = os;
        this.writer = writer;
        this.recordSet = recordSet;
    }

    @Override
    public byte[] getValue() throws IOException {
        writer.write(recordSet.next());
        writer.flush();
        final byte[] value = os.toByteArray();
        os.reset();
        return value;
    }
}
