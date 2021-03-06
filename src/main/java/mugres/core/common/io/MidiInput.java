package mugres.core.common.io;

import mugres.core.common.Instrument;
import mugres.core.common.InstrumentChange;
import mugres.core.common.Pitch;
import mugres.core.common.Played;
import mugres.core.common.Signal;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;
import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;
import static javax.sound.midi.ShortMessage.PROGRAM_CHANGE;

public class MidiInput extends Input {
    private final Transmitter transmitter;

    private MidiInput(final Transmitter transmitter) {
        this.transmitter = transmitter;
        this.transmitter.setReceiver(new MidiReceiver());
    }

    public static MidiInput of(final Transmitter inputPort) {
        return new MidiInput(inputPort);
    }

    private class MidiReceiver implements Receiver {
        @Override
        public void send(final MidiMessage message, long timeStamp) {
            if (!(message instanceof ShortMessage))
                return;
            final ShortMessage shortMessage = (ShortMessage) message;

            if (shortMessage.getCommand() == NOTE_ON)
                MidiInput.this.send(Signal.on(UUID.randomUUID(), currentTimeMillis(),
                        shortMessage.getChannel(),
                        Played.of(Pitch.of(shortMessage.getData1()), shortMessage.getData2())));
            else if (shortMessage.getCommand() == NOTE_OFF)
                MidiInput.this.send(Signal.off(UUID.randomUUID(), currentTimeMillis(),
                        shortMessage.getChannel(),
                        Played.of(Pitch.of(shortMessage.getData1()), 0)));
            else if (shortMessage.getCommand() == PROGRAM_CHANGE)
                MidiInput.this.send(InstrumentChange.of(shortMessage.getChannel(), Instrument.of(shortMessage.getData1())));
        }

        @Override
        public void close() {
        }
    }
}
