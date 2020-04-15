package mugres.core.common;

/** Live signal. Analog to {@link mugres.core.common.Event} in the notated world. */
public class Signal {
    private final long time;
    private final int channel;
    private final Played played;
    private final boolean active;

    private Signal(final long time, final int channel, final Played played, final boolean active) {
        this.time = time;
        this.channel = channel;
        this.played = played;
        this.active = active;
    }

    public static Signal on(final long time, final int channel, final Played played) {
        return new Signal(time, channel, played, true);
    }

    public static Signal off(final long time, final int channel, final Played played) {
        return new Signal(time, channel, played, false);
    }

    public long getTime() {
        return time;
    }

    public int getChannel() {
        return channel;
    }

    public Played getPlayed() {
        return played;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return String.format("%s [%d][%s]", played, channel, active ? "on" : "off");
    }
}