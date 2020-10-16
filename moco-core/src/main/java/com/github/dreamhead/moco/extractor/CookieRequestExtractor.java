package com.github.dreamhead.moco.extractor;

import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.HttpRequestExtractor;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public final class CookieRequestExtractor extends HttpRequestExtractor<String> {
    private final CookiesRequestExtractor extractor = new CookiesRequestExtractor();

    private final String key;

    public CookieRequestExtractor(final String key) {
        this.key = key;
    }

    @Override
    protected Optional<String> doExtract(final HttpRequest request) {
        Optional<ImmutableMap<String, String>> cookies = extractor.extract(request);
        if (cookies.isPresent()) {
            return ofNullable(cookies.get().get(this.key));
        }

        return empty();
    }
}
