package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReverseHelper {

    private ReverseHelper() {
        throw new UnsupportedOperationException();
    }

    public static <K, V> @NotNull Map<K, V> reversed(@NotNull final Map<K, V> map) {
        final List<Map.Entry<K, V>> list = map.entrySet().stream().toList();
        final Map<K, V> resultMap = new LinkedHashMap<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            final Map.Entry<K, V> entry = list.get(i);
            resultMap.put(entry.getKey(), entry.getValue());
        }
        return resultMap;
    }

}
