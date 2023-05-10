package gazpromcps.smg.dto;

import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

public interface DTO {
    // MultiValueMap<String, String> json();

    class WebResponseException extends RuntimeException {
        public WebResponseException(final String message) {
            super(message);
        }
    }

    default ServerResponse get(final WebClient webClient,
                                final String url, final Duration timeout) {
        final ServerResponse response = webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(ServerResponse.class)
                .block(timeout);

        if (response == null) {
            throw new WebResponseException("Call other REST API failed. Invalid json response");
        }

        return response;
    }

    default <T extends DTO> ServerResponse post(final WebClient webClient,
                               final String url, final T body, final Duration timeout) {
        final ServerResponse response = webClient
                .post()
                .uri(url)
                // .body(BodyInserters.fromFormData(body.json()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ServerResponse.class)
                .block(timeout);

        if (response == null) {
            throw new WebResponseException("Call other REST API failed. Invalid json response");
        }

        return response;
    }
}
