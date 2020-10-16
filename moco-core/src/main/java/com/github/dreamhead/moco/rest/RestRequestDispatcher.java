package com.github.dreamhead.moco.rest;

import com.github.dreamhead.moco.HttpMethod;
import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.Moco;
import com.github.dreamhead.moco.RequestMatcher;
import com.github.dreamhead.moco.ResponseHandler;
import com.github.dreamhead.moco.RestIdMatcher;
import com.github.dreamhead.moco.RestSetting;
import com.github.dreamhead.moco.handler.JsonResponseHandler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.status;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Moco.with;
import static com.github.dreamhead.moco.rest.RestIdMatchers.eq;
import static com.github.dreamhead.moco.util.URLs.join;
import static com.github.dreamhead.moco.util.URLs.resourceRoot;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public final class RestRequestDispatcher {
    private static final ResponseHandler NOT_FOUND_HANDLER = status(HttpResponseStatus.NOT_FOUND.code());
    private static final ResponseHandler BAD_REQUEST_HANDLER = status(HttpResponseStatus.BAD_REQUEST.code());

    private final RestIdMatcher name;
    private final RequestMatcher allMatcher;
    private final RequestMatcher singleMatcher;
    private final CompositeRestSetting<RestAllSetting> getAllSettings;
    private final CompositeRestSetting<RestSingleSetting> getSingleSettings;
    private final CompositeRestSetting<RestAllSetting> postSettings;
    private final CompositeRestSetting<RestSingleSetting> putSettings;
    private final CompositeRestSetting<RestSingleSetting> deleteSettings;
    private final CompositeRestSetting<RestSingleSetting> headSettings;
    private final CompositeRestSetting<RestAllSetting> headAllSettings;
    private final CompositeRestSetting<RestSingleSetting> patchSettings;
    private final Iterable<SubResourceSetting> subResourceSettings;

    public RestRequestDispatcher(final String name, final Iterable<RestSetting> settings) {
        this.name = eq(name);

        this.getAllSettings = filterSettings(settings, RestAllSetting.class, HttpMethod.GET);
        this.getSingleSettings = filterSettings(settings, RestSingleSetting.class, HttpMethod.GET);
        this.postSettings = filterSettings(settings, RestAllSetting.class, HttpMethod.POST);
        this.putSettings = filterSettings(settings, RestSingleSetting.class, HttpMethod.PUT);
        this.deleteSettings = filterSettings(settings, RestSingleSetting.class, HttpMethod.DELETE);
        this.headSettings = filterSettings(settings, RestSingleSetting.class, HttpMethod.HEAD);
        this.headAllSettings = filterSettings(settings, RestAllSetting.class, HttpMethod.HEAD);
        this.patchSettings = filterSettings(settings, RestSingleSetting.class, HttpMethod.PATCH);
        this.subResourceSettings = filter(settings, SubResourceSetting.class);
        this.allMatcher = by(uri(resourceRoot(name)));
        this.singleMatcher = Moco.match(uri(join(resourceRoot(name), "[^/]*")));
    }

    private <T extends SimpleRestSetting> CompositeRestSetting<T> filterSettings(final Iterable<RestSetting> settings,
                                                                                 final Class<T> type,
                                                                                 final HttpMethod method) {
        return new CompositeRestSetting<>(filter(settings, type, method));
    }

    private <T extends SimpleRestSetting> Iterable<T> filter(final Iterable<RestSetting> settings,
                                                             final Class<T> type,
                                                             final HttpMethod method) {
        return filter(settings, type).stream()
                .filter(input -> input.isSimple() && input.isFor(method))
                .collect(Collectors.toList());
    }

    private <T extends RestSetting> ImmutableList<T> filter(final Iterable<RestSetting> settings,
                                                            final Class<T> type) {
        return StreamSupport.stream(settings.spliterator(), false)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(toImmutableList());
    }

    private Optional<ResponseHandler> getSingleOrAllHandler(final HttpRequest httpRequest,
                                                            final CompositeRestSetting<RestSingleSetting> single,
                                                            final CompositeRestSetting<RestAllSetting> all,
                                                            final RestIdMatcher name) {
        Optional<ResponseHandler> singleHandler = single.getMatched(name, httpRequest);
        if (singleHandler.isPresent()) {
            return singleHandler;
        }

        return all.getMatched(name, httpRequest);

    }

    private Optional<ResponseHandler> getHeadHandler(final HttpRequest httpRequest) {
        Optional<ResponseHandler> handler = getSingleOrAllHandler(httpRequest,
                headSettings,
                headAllSettings,
                name);
        if (handler.isPresent()) {
            return handler;
        }

        return of(NOT_FOUND_HANDLER);
    }

    private Optional<ResponseHandler> getGetHandler(final HttpRequest httpRequest) {
        Optional<ResponseHandler> matchedSetting = getSingleOrAllHandler(httpRequest,
                getSingleSettings,
                getAllSettings, name);
        if (matchedSetting.isPresent()) {
            return matchedSetting;
        }

        if (allMatcher.match(httpRequest)) {
            Iterable<RestSingleSetting> settings = getSingleSettings.getSettings();
            if (!Iterables.isEmpty(settings)
                    && StreamSupport.stream(settings.spliterator(), false)
                    .allMatch(setting -> setting.getHandler() instanceof JsonResponseHandler)) {
                List<Object> result = StreamSupport.stream(settings.spliterator(), false)
                        .map((Function<SimpleRestSetting, JsonResponseHandler>) setting -> JsonResponseHandler.class.cast(setting.getHandler()))
                        .map(JsonResponseHandler::getPojo)
                        .collect(Collectors.toList());
                return of(with(Moco.json(result)));
            }
        }

        return of(NOT_FOUND_HANDLER);
    }

    private Optional<ResponseHandler> getPostHandler(final HttpRequest request) {
        Optional<ResponseHandler> handler = postSettings.getMatched(name, request);
        if (handler.isPresent()) {
            return handler;
        }

        if (singleMatcher.match(request)) {
            return of(NOT_FOUND_HANDLER);
        }

        return of(BAD_REQUEST_HANDLER);
    }

    private Optional<ResponseHandler> getSingleResponseHandler(
            final CompositeRestSetting<RestSingleSetting> settings,
            final HttpRequest httpRequest) {
        Optional<ResponseHandler> handler = settings.getMatched(name, httpRequest);
        if (handler.isPresent()) {
            return handler;
        }

        return of(NOT_FOUND_HANDLER);
    }

    public Optional<ResponseHandler> getResponseHandler(final HttpRequest httpRequest) {
        if (allMatcher.match(httpRequest) || this.singleMatcher.match(httpRequest)) {
            return doGetResponseHandler(httpRequest);
        }

        return getSubResponseHandler(httpRequest);
    }

    private Optional<ResponseHandler> getSubResponseHandler(final HttpRequest httpRequest) {
        for (SubResourceSetting subResourceSetting : subResourceSettings) {
            Optional<ResponseHandler> matched = subResourceSetting.getMatched(name, httpRequest);
            if (matched.isPresent()) {
                return matched;
            }
        }

        return empty();
    }

    private Optional<ResponseHandler> doGetResponseHandler(final HttpRequest httpRequest) {
        HttpMethod method = httpRequest.getMethod();
        if (HttpMethod.GET == method) {
            return getGetHandler(httpRequest);
        }

        if (HttpMethod.POST == method) {
            return getPostHandler(httpRequest);
        }

        if (HttpMethod.PUT == method) {
            return getSingleResponseHandler(putSettings, httpRequest);
        }

        if (HttpMethod.DELETE == method) {
            return getSingleResponseHandler(deleteSettings, httpRequest);
        }

        if (HttpMethod.HEAD == method) {
            return getHeadHandler(httpRequest);
        }

        if (HttpMethod.PATCH == method) {
            return getSingleResponseHandler(patchSettings, httpRequest);
        }

        return empty();
    }
}
