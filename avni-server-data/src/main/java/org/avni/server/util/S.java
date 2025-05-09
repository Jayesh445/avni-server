package org.avni.server.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class S {
    public static boolean isEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String getLastStringAfter(String originalString, String separator) {
        return originalString.substring(originalString.lastIndexOf(separator) + 1);
    }

    public static String joinLongToList(List<Long> lists) {
        return lists.isEmpty() ? "" : lists.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public static String[] splitMultiSelectAnswer(String answerValue) {
        /* For multi-select answers, expected input format would be:
           1. Answer 1, Answer 2, ...
           2. Answer 1, "Answer2, has, commas", Answer 3, ...
           ... etc.
        */
        return Stream.of(answerValue.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(value -> value.trim().replaceAll("\"", ""))
                .toArray(String[]::new);
    }

    public static String unDoubleQuote(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public static String doubleQuote(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str;
        }
        return "\"" + str + "\"";
    }
}
