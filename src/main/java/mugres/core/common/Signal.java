package mugres.core.common;

import java.util.*;

/** Live signal. Analog to {@link mugres.core.common.Event} in the notated world. */
public class Signal {
    private final long time;
    private final int channel;
    private final Played played;
    private final boolean active;
    private Map<String, Object> attributes;
    private final Object attributesSyncObject = new Object();

    private Signal(final long time, final int channel, final Played played, final boolean active) {
        this.time = time;
        this.channel = channel;
        this.played = played;
        this.active = active;
    }

    public static Signal on(final long time, final int channel, final Played played) {
        return of(time, channel, played, true);
    }

    public static Signal off(final long time, final int channel, final Played played) {
        return of(time, channel, played, false);
    }

    public static Signal of(final long time, final int channel, final Played played, final boolean active) {
        return of(time, channel, played, active, null);
    }

    public static Signal of(final long time, final int channel, final Played played, final boolean active,
                            final Map<String, Object> attributes) {
        final Signal signal = new Signal(time, channel, played, active);
        if (attributes != null)
            for(String key : attributes.keySet())
                signal.setAttribute(key, attributes.get(key));
        return signal;
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

    public Signal modifiedPlayed(final Played newPlayed) {
        synchronized (attributesSyncObject) {
            return of(time, channel, newPlayed, active, attributes);
        }
    }

    public Signal modifiedTime(final long newTime) {
        synchronized (attributesSyncObject) {
            return of(newTime, channel, played, active, attributes);
        }
    }

    public Signal toOn() {
        if (active)
            return this;
        else
            synchronized (attributesSyncObject) {
                return of(time, channel, played, true, attributes);
            }
    }

    public Signal toOff() {
        if (active)
            synchronized (attributesSyncObject) {
                return of(time, channel, played, false, attributes);
            }
        else
            return this;
    }

    public Map<String, Object> getAttributes() {
        synchronized (attributesSyncObject) {
            return  attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
        }
    }

    public void setAttribute(final String name, final Object value) {
        synchronized (attributesSyncObject) {
            if (attributes == null)
                attributes = new HashMap<>();
            attributes.put(name, value);
        }
    }

    public <X> X getAttribute(final String name) {
        synchronized (attributesSyncObject) {
            return attributes == null ?
                null :
                    (X)attributes.get(name);
        }
    }

    public void removeAttribute(final String name) {
        synchronized (attributesSyncObject) {
            if (attributes != null)
                attributes.remove(name);
        }
    }

    public void addTag(final String tag) {
        if (tag == null || tag.trim().isEmpty())
            return;

        synchronized (attributesSyncObject) {
            Set<String> tags = getAttribute(TAGS);
            if (tags == null) {
                tags = new HashSet<>();
                setAttribute(TAGS, tags);
            }
            tags.add(tag);
        }
    }

    public boolean hasTag(final String tag) {
        if (tag == null || tag.trim().isEmpty())
            return false;

        final Set<String> tags = getAttribute(TAGS);
        return tags == null ? false : tags.contains(tag);
    }

    @Override
    public String toString() {
        return String.format("%s [%d][%s][%s]", played, channel, active ? "on" : "off", getAttribute(TAGS));
    }

    /** Tags attribute name */
    public static final String TAGS = "tags";
}
