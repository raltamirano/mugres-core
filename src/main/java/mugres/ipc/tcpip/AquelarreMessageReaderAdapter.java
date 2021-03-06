package mugres.ipc.tcpip;

import aquelarre.Envelope;
import aquelarre.MessageReader;
import mugres.ipc.protocol.Message;
import mugres.ipc.stream.DataInputStreamReader;

import java.io.DataInputStream;
import java.io.IOException;

import static mugres.ipc.tcpip.Utils.switchEnvelope;

public class AquelarreMessageReaderAdapter implements MessageReader<Message> {
    private final DataInputStreamReader dataInputStreamReader;

    public AquelarreMessageReaderAdapter(final DataInputStreamReader dataInputStreamReader) {
        if (dataInputStreamReader == null)
            throw new IllegalArgumentException("dataInputStreamReader");

        this.dataInputStreamReader = dataInputStreamReader;
    }

    @Override
    public Envelope<Message> read(final DataInputStream dataInputStream) throws IOException {
        return switchEnvelope(dataInputStreamReader.read(dataInputStream));
    }
}
