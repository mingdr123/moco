package com.github.dreamhead.moco.recorder;

public class TapeRecorderFactory implements RecorderFactory {
    private final RecorderTape tape;

    public TapeRecorderFactory(final RecorderTape tape) {
        this.tape = tape;
    }

    @Override
    public final RequestRecorder newRecorder(final String name) {
        return new FileRequestRecorder(name, tape);
    }
}
