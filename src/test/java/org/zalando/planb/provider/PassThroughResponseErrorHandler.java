package org.zalando.planb.provider;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public final class PassThroughResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(final ClientHttpResponse response) {
        return false;
    }

    @Override
    public void handleError(final ClientHttpResponse response) {

    }

}
