package com.github.dreamhead.moco.handler;

import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.MutableHttpResponse;
import com.github.dreamhead.moco.MutableResponse;
import com.github.dreamhead.moco.Request;
import com.github.dreamhead.moco.Response;
import com.github.dreamhead.moco.internal.SessionContext;
import com.github.dreamhead.moco.model.MessageContent;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

public abstract class AbstractContentResponseHandler extends AbstractResponseHandler {
    private final HeaderDetector detector = new HeaderDetector();

    protected abstract MessageContent responseContent(Request request);
    protected abstract MediaType getContentType(HttpRequest request);

    @Override
    public final void writeToResponse(final SessionContext context) {
        Request request = context.getRequest();
        Response response = context.getResponse();

        if (request instanceof HttpRequest && response instanceof MutableHttpResponse) {
            HttpRequest httpRequest = (HttpRequest) request;
            MutableHttpResponse httpResponse = (MutableHttpResponse) response;
            doWriteToResponse(httpRequest, httpResponse);
            return;
        }

        MutableResponse mutableResponse = (MutableResponse) response;
        mutableResponse.setContent(requireResponseContent(request));
    }

    private void doWriteToResponse(final HttpRequest httpRequest, final MutableHttpResponse httpResponse) {
        MessageContent content = requireResponseContent(httpRequest);
        httpResponse.setContent(content);
        httpResponse.addHeader(HttpHeaders.CONTENT_LENGTH, content.getContent().length);

        if (!detector.hasContentType(httpResponse)) {
            httpResponse.addHeader(HttpHeaders.CONTENT_TYPE, getContentType(httpRequest));
        }
    }

    private MessageContent requireResponseContent(final Request request) {
        MessageContent content = responseContent(request);
        if (content == null) {
            throw new IllegalStateException("Message content is expected. Please make sure responseContent method has been implemented correctly");
        }
        return content;
    }
}
