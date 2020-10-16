package com.github.dreamhead.moco.recorder;

import java.util.HashMap;
import java.util.Map;

import static com.github.dreamhead.moco.recorder.RecorderFactory.IN_MEMORY;

public class RecorderRegistry {
    private final RecorderFactory factory;
    private final String groupName;
    private Map<String, RequestRecorder> recorders = new HashMap<>();

    public RecorderRegistry(final RecorderFactory factory, final String name) {
        this.factory = factory;
        this.groupName = name;
    }

    public String getGroup() {
        return groupName;
    }

    public final RequestRecorder recorderOf(final String name) {
        RequestRecorder recorder = recorders.get(name);

        if (recorder != null) {
            return recorder;
        }

        return recorders.computeIfAbsent(name, s -> factory.newRecorder(name));
    }

    private static final RecorderRegistry REGISTRY = new RecorderRegistry(IN_MEMORY, null);

    public static RecorderRegistry defaultRegistry() {
        return REGISTRY;
    }

    private static Map<String, RecorderRegistry> registries;

    public static RecorderRegistry registryOf(final String name, final RecorderFactory factory) {
        if (registries == null) {
            registries = new HashMap<>();
        }

        return registries.computeIfAbsent(name, s -> new RecorderRegistry(factory, name));
    }
}
