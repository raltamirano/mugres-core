package mugres.ipc;

import mugres.ipc.protocol.Message;

public interface Listener {
    void onMessage(final Envelope<Message> message);
}
