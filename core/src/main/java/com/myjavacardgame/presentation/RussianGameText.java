package com.myjavacardgame.presentation;

import com.badlogic.gdx.utils.JsonReader;
import com.myjavacardgame.domain.resources.ResourceType;
import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Presentation-only Russian text; domain identifiers and rejection contracts stay unchanged. */
public final class RussianGameText {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z]+)\\}");
    private final Map<String, String> translations = new LinkedHashMap<>();

    /** Loads a catalog from an injected reader without owning or closing it. */
    public RussianGameText(Reader reader) {
        var root = new JsonReader().parse(reader);
        for (var entry = root.child; entry != null; entry = entry.next) {
            String source = entry.getString("en"), translated = entry.getString("ru");
            if (entry.getString("context").isBlank() || !placeholders(source).equals(placeholders(translated))) {
                throw new IllegalArgumentException("Invalid translation: " + entry.name);
            }
            if (translations.putIfAbsent(entry.name, translated) != null) throw new IllegalArgumentException("Duplicate translation");
        }
    }

    /** Returns a required translated label, rejecting missing keys rather than leaking IDs. */
    public String text(String key) {
        String value = translations.get(key);
        if (value == null) throw new IllegalArgumentException("Missing translation: " + key);
        return value;
    }

    /** Substitutes named values, requiring an exact placeholder set. */
    public String format(String key, Map<String, ?> values) {
        String result = text(key);
        if (!placeholders(result).equals(values.keySet())) throw new IllegalArgumentException("Invalid placeholders: " + key);
        var matcher = PLACEHOLDER.matcher(result);
        var out = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(String.valueOf(values.get(matcher.group(1)))));
        matcher.appendTail(out);
        return out.toString();
    }

    /** Formats named key/value pairs for short UI call sites. */
    public String format(String key, Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Expected named pairs");
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) values.put((String) pairs[index], pairs[index + 1]);
        return format(key, values);
    }

    /** Renders resource amounts in stable enum order, never via Map.toString(). */
    public String amounts(Map<ResourceType, Integer> amounts) {
        return amounts.entrySet().stream().filter(entry -> entry.getValue() != 0).sorted(Map.Entry.comparingByKey())
                .map(entry -> text("resource." + entry.getKey().name()) + ": " + entry.getValue()).collect(Collectors.joining(", "));
    }

    /** Translates a known rejection, otherwise returns a safe generic Russian message. */
    public String rejection(String reason) { return translations.getOrDefault("reject." + reason, text("unavailable")); }

    /** Renders participants without exposing fixture identifiers. */
    public String participant(String id) {
        if (id.equals("player")) return text("player");
        if (id.matches("bot-[0-9]+")) return format("bot", "number", id.substring(4));
        return text("player");
    }

    /** Formats a known enum in a named catalog category. */
    public String label(String category, Enum<?> value) { return text(category + "." + value.name()); }

    private static Set<String> placeholders(String text) {
        Set<String> names = new HashSet<>();
        var matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) names.add(matcher.group(1));
        return names;
    }
}
