package mugres.core.function.builtin.arp;

import mugres.core.common.*;
import mugres.core.function.Function.EventsFunction;
import mugres.core.function.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mugres.core.common.Value.QUARTER;
import static mugres.core.function.Function.Parameter.DataType.TEXT;

public class Arp extends EventsFunction {
    public Arp() {
        super("arp", "Arpeggiates composed call's events",
                Parameter.of("pattern", "Arp pattern",
                        TEXT, true, "1232")
        );
    }

    @Override
    protected List<Event> doExecute(final Context context, final Map<String, Object> arguments) {
        final Result<List<Event>> composed = getComposedCallResult(arguments);
        final List<Event> events = new ArrayList<>();
        final String pattern = (String) arguments.get("pattern");
        final Matcher matcher = ARP_PATTERN.matcher(pattern);

        // Big assumptions here:
        // all chord notes start at the same position and last for the same amount of time (value)

        extractPositions(composed.getData())
                .stream()
                .map(p -> getChordEvents(composed.getData(), p))
                .map(c -> arpeggiate(c, matcher))
                .forEach(events::addAll);

        return events;
    }

    private static List<Length> extractPositions(final List<Event> data) {
        return data.stream()
                .map(Event::getPosition)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<Event> getChordEvents(final List<Event> data, final Length position) {
        return data.stream()
                .filter(e -> e.getPosition().equals(position))
                .collect(Collectors.toList());
    }

    private static List<Event> arpeggiate(final List<Event> chord, final Matcher matcher) {
        final List<Event> arpeggio = new ArrayList<>();
        final Length totalLength = chord.get(0).getValue().length();

        Length position = chord.get(0).getPosition();
        Length controlLength = Length.ZERO;

        while(controlLength.lessThan(totalLength)) {
            matcher.reset();
            while(matcher.find() && controlLength.lessThan(totalLength)) {
                final String element = matcher.group(2);
                final int index = REST.equals(element) ? 0 : Integer.parseInt(element);
                final Value value = parseNoteValue(matcher.group(3));
                final Event event = index < chord.size() ? chord.get(index) : chord.get(0);
                final Value actualValue = controlLength.plus(value.length()).greaterThan(totalLength) ?
                        Value.forLength(totalLength.minus(controlLength)) : value;

                arpeggio.add(Event.of(position, event.getPlayed().getPitch(), actualValue, event.getPlayed().getVelocity()));
                position = position.plus(value.length());
                controlLength = controlLength.plus(value.length());
            }
        }

        return arpeggio;
    }

    private static Value parseNoteValue(final String input) {
        return input == null || input.trim().isEmpty() ? QUARTER : Value.forId(input);
    }

    private static final String REST = "R";
    private static final Pattern ARP_PATTERN = Pattern.compile("((\\d|" + REST + ")(w|h|q|e|s|t|m)?)+?");
}
