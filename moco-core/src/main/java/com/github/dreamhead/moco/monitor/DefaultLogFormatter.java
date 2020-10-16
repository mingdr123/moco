package com.github.dreamhead.moco.monitor;

import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.HttpResponse;
import com.github.dreamhead.moco.Request;
import com.github.dreamhead.moco.Response;
import com.github.dreamhead.moco.SocketRequest;
import com.github.dreamhead.moco.SocketResponse;
import com.github.dreamhead.moco.dumper.Dumper;
import com.github.dreamhead.moco.dumper.HttpRequestDumper;
import com.github.dreamhead.moco.dumper.HttpResponseDumper;
import com.github.dreamhead.moco.dumper.SocketRequestDumper;
import com.github.dreamhead.moco.dumper.SocketResponseDumper;
import com.google.common.collect.ImmutableMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public final class DefaultLogFormatter implements LogFormatter {
    private static final ImmutableMap<Class<? extends Request>, Dumper<Request>> REQUEST_DUMPERS = ImmutableMap.of(
            HttpRequest.class, new HttpRequestDumper(),
            SocketRequest.class, new SocketRequestDumper()
    );

    private static final ImmutableMap<Class<? extends Response>, Dumper<Response>> RESPONSE_DUMPERS = ImmutableMap.of(
            HttpResponse.class, new HttpResponseDumper(),
            SocketResponse.class, new SocketResponseDumper()
    );

    @Override
    public String format(final Request request) {
        return String.format("Request received:\n\n%s\n", findDumper(request, REQUEST_DUMPERS).dump(request));
    }

    @Override
    public String format(final Response response) {
        return String.format("Response return:\n\n%s\n", findDumper(response, RESPONSE_DUMPERS).dump(response));
    }

    @Override
    public String format(final Throwable e) {
        return String.format("Exception thrown:\n\n%s\n", stackTraceToString(e));
    }

    private String stackTraceToString(final Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private <T> Dumper<T> findDumper(final T target, final ImmutableMap<Class<? extends T>, Dumper<T>> dumperClasses) {
        Optional<Class<? extends T>> dumpClass = dumperClasses.keySet().stream()
                .filter(input -> input.isInstance(target))
                .findFirst();
        if (dumpClass.isPresent()) {
            return dumperClasses.get(dumpClass.get());
        }

        throw new IllegalArgumentException("Unknown target type:" + target.getClass());
    }
}
