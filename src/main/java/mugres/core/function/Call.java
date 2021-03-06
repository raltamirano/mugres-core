package mugres.core.function;

import mugres.core.common.Context;
import mugres.core.common.DrumKit;
import mugres.core.common.Length;
import mugres.core.common.Note;
import mugres.core.common.Pitch;
import mugres.core.common.Scale;
import mugres.core.common.Value;
import mugres.core.function.Function.Parameter.Variant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mugres.core.function.Function.COMPOSED_CALL_RESULT_PARAMETER;
import static mugres.core.function.Function.LENGTH_PARAMETER;

/** Function call. */
public class Call<T> {
    protected final Function<T> function;
    protected final Map<String, Object> arguments = new HashMap<>();

    protected Call(final Function<T> function, final Map<String, Object> arguments) {
        this.function = function;
        this.arguments.putAll(arguments);
    }

    private Call(final Function<T> function, final int lengthInMeasures) {
        this.function = function;
        this.arguments.put(LENGTH_PARAMETER.name(), lengthInMeasures);
    }

    public static <X> Call<X> of(final String functionName, final Map<String, Object> arguments) {
        final Function function = Function.forName(functionName);
        if (function == null)
            throw new RuntimeException("Unknown function: " + functionName);

        return of(function, arguments);
    }

    public static <X> Call of(final Function<X> function, final Map<String, Object> arguments) {
        return new Call(function, arguments);
    }

    public static <X> Call of(final String functionName, final int lengthInMeasures) {
        final Function function = Function.forName(functionName);
        if (function == null)
            throw new RuntimeException("Unknown function: " + functionName);

        return of(function, lengthInMeasures);
    }

    public static <X> Call of(final Function<X> function, final int lengthInMeasures) {
        return new Call(function, lengthInMeasures);
    }

    public static <X> Call of(final String functionName, final int lengthInMeasures,
            final Map<String, Object> arguments) {
        final Map<String, Object> theArgs = arguments != null ? arguments : Collections.emptyMap();
        theArgs.put(LENGTH_PARAMETER.name(), lengthInMeasures);
        return of(functionName, theArgs);
    }

    public static <X> Call of(final Function<X> function, final int lengthInMeasures,
                              final Map<String, Object> arguments) {
        final Map<String, Object> theArgs = arguments != null ? arguments : Collections.emptyMap();
        theArgs.put(LENGTH_PARAMETER.name(), lengthInMeasures);
        return of(function, theArgs);
    }

    public static <X> Call<X> parse(final String input) {
        return parse(input, Collections.emptyMap());
    }

    public static <X> Call<X> parse(final String input, final Map<String, String> argumentsMap) {
        if (input == null || input.trim().isEmpty())
            throw new IllegalArgumentException("input");

        if (!Pattern.matches("^" + FUNCTION_CALL.pattern() + "$", input))
            throw new IllegalArgumentException("Invalid function call format: " + input);

        final Matcher functionCallMatcher = FUNCTION_CALL.matcher(input);
        functionCallMatcher.matches();

        // Get function
        final String functionName = functionCallMatcher.group(1);
        final Function function = getFunction(functionName);

        // Get arguments
        final String functionArguments = functionCallMatcher.group(2).trim();
        final Matcher argumentsMatcher = NAMED_ARGS_LIST.matcher(functionArguments);

        final Map<String, String> allArguments = new HashMap<>(argumentsMap);
        // Override any arguments provided with the arguments map with those
        // provided inline in the call specification itself.
        while(argumentsMatcher.find()) {
            final String parameterName = argumentsMatcher.group(1);
            final String argumentString = argumentsMatcher.group(2).trim();
            allArguments.put(parameterName, argumentString);
        }

        final Map<String, Object> arguments = new HashMap<>();
        for(Map.Entry<String, String> entry : allArguments.entrySet()) {
            final String parameterName = entry.getKey();
            final String argumentString = entry.getValue().trim();

            final Function.Parameter parameter = function.parameter(parameterName);
            if (parameter == null)
                throw new RuntimeException(String.format("Unexpected parameter '%s' for function '%s'",
                        parameterName, functionName));

            if (parameter.isMultiple())
                throw new RuntimeException(String.format("Unsupported attribute 'multiple' on parameter '%s' for function '%s'",
                        parameterName, functionName));

            Object argument = null;
            if (argumentString != null && !argumentString.isEmpty()) {
                switch(parameter.dataType()) {
                    case LENGTH:
                        argument = Length.of(Integer.parseInt(argumentString));
                        break;
                    case VALUE:
                        argument = Value.valueOf(argumentString);
                        break;
                    case NOTE:
                        argument = Note.of(argumentString);
                        break;
                    case PITCH:
                        argument = Pitch.of(argumentString);
                        break;
                    case SCALE:
                        argument = Scale.of((areTextDelimitersPresent(argumentString) ? removeTextDelimiters(argumentString) : argumentString));
                        break;
                    case TEXT:
                        if (areTextDelimitersPresent(argumentString))
                            argument = removeTextDelimiters(argumentString);
                        else
                            throw new IllegalArgumentException("TEXT function parameter's values must be " +
                                    "enclosed in single quotes (')");
                        break;
                    case INTEGER:
                        argument = Integer.parseInt(argumentString);
                        break;
                    case BOOLEAN:
                        argument = Boolean.parseBoolean(argumentString);
                        break;
                    case DRUM_KIT:
                        argument = DrumKit.valueOf(argumentString);
                        break;
                    case VARIANT:
                        argument = Variant.valueOf(argumentString);
                        break;
                }
            }

            if (argument != null)
                arguments.put(parameterName, argument);
        }

        if (arguments.isEmpty() && !functionArguments.isEmpty())
            throw new IllegalArgumentException("Invalid function call arguments format: " + functionArguments);

        return of(function, arguments);
    }

    private static boolean areTextDelimitersPresent(final String input) {
        return input.startsWith("'") && input.endsWith("'");
    }

    private static String removeTextDelimiters(final String input) {
        return input.substring(1, input.length() - 1);
    }

    private static Function getFunction(final String functionName) {
        final Function<Object> fn = Function.forName(functionName);
        if (fn == null)
            throw new RuntimeException("Unknown function: " + functionName);

        return fn;
    }

    public Function<T> getFunction() {
        return function;
    }

    public Call<T> compose(final String functionName, final Map<String, Object> arguments) {
        final Function function = Function.forName(functionName);
        if (function == null)
            throw new RuntimeException("Unknown function: " + functionName);

        return compose(function, arguments);
    }

    public Call<T> compose(final Function<T> function, final Map<String, Object> arguments) {
        return new ComposedCall<>(this, function, arguments);
    }

    public Result<T> execute(final Context context) {
        try {
            return new Result(function.execute(context, arguments));
        } catch (final Throwable t) {
            return new Result(t);
        }
    }

    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }

    public int getLengthInMeasures() {
        return (int)arguments.get(LENGTH_PARAMETER.name());
    }

    private static final Pattern FUNCTION_CALL = Pattern.compile("([a-z][0-9a-zA-Z_-]+[0-9a-zA-Z])\\((.*)\\)");
    private static final Pattern NAMED_ARGS_LIST = Pattern.compile("([a-z][0-9a-zA-Z_-]*[0-9a-zA-Z])\\=(\\'(?:[#\\[\\]\\{\\}\\|\\s0-9a-zA-Z_-]+)\\'|(?:\\-?\\d+(?:\\.\\d+)?)|true|false|yes|no|y|n|(?:[0-9a-zA-Z_-]+))");

    private static class ComposedCall<X> extends Call<X> {
        private final Call<X> wrapped;

        ComposedCall(final Call<X> wrapped,
                             final Function<X> functionToApply,
                             final Map<String, Object> functionToApplyArguments) {
            super(functionToApply, functionToApplyArguments);

            this.wrapped = wrapped;
        }

        @Override
        public Result<X> execute(Context context) {
            try {
                final Result<X> wrappedResult = wrapped.execute(context);
                arguments.put(COMPOSED_CALL_RESULT_PARAMETER.name(), wrappedResult);
                return new Result(function.execute(context, arguments));
            } catch (final Throwable t) {
                return new Result(t);
            }
        }
    }
}
