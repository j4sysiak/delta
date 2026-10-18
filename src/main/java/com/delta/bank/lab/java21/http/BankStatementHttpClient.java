package com.delta.bank.lab.java21.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class BankStatementHttpClient {

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public BankStatementHttpClient(
            HttpClient httpClient,
            Duration requestTimeout
    ) {
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient must not be null"
        );
        this.requestTimeout = Objects.requireNonNull(
                requestTimeout,
                "requestTimeout must not be null"
        );

        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    public String getStatement(URI endpoint)
            throws IOException, InterruptedException {
        validateEndpoint(endpoint);

        var request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Accept", "text/plain")
                .GET()
                .build();

        var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BankApiException(response.statusCode());
        }

        return response.body();
    }

    private void validateEndpoint(URI endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");

        var scheme = endpoint.getScheme();
        if (!endpoint.isAbsolute()
                || !("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || endpoint.getFragment() != null) {
            throw new IllegalArgumentException(
                    "endpoint must be an absolute HTTP(S) URI without a fragment"
            );
        }
    }
}