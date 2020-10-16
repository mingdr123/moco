package com.github.dreamhead.moco.handler;

import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.ResponseHandler;
import com.github.dreamhead.moco.handler.failover.Failover;

import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class ProxyResponseHandler extends AbstractProxyResponseHandler implements ResponseHandler {
    private final Function<HttpRequest, URL> url;

    public ProxyResponseHandler(final Function<HttpRequest, URL> url, final Failover failover) {
        super(failover);
        this.url = url;
    }

    @Override
    protected final Optional<String> doRemoteUrl(final HttpRequest request) {
        try {
            URL targetUrl = url.apply(request);
            if (targetUrl != null) {
                return of(targetUrl.toString());
            }

            return empty();
        } catch (IllegalArgumentException e) {
            return empty();
        }
    }
}
