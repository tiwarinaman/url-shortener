package com.naman.urlshortner.controller;

import com.naman.urlshortner.api.UrlShortenerApi;
import com.naman.urlshortner.domain.response.UrlShortenResponse;
import com.naman.urlshortner.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController implements UrlShortenerApi {

    private final UrlShortenerService urlShortenerService;

    @Override
    public ResponseEntity<String> shortenUrl(String originalUrl, int expirationHours) {
        String shortenUrl = urlShortenerService.shortenUrl(originalUrl, expirationHours);

        // ServletUriComponentsBuilder
        String fullShortenedUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/")
                .path(shortenUrl)
                .toUriString();

        return ResponseEntity.status(HttpStatus.CREATED).body(fullShortenedUrl);
    }

    @Override
    public ResponseEntity<?> handleUrlRequest(String shortUrl, HttpServletResponse response) {
        try {
            return urlShortenerService.fetchOriginalUrl(shortUrl)
                    .map(urlResponse -> {
                        try {
                            return redirectToOriginalUrl(urlResponse.originalUrl(), response);
                        } catch (IOException e) {
                            return handleRedirectError(e);
                        }
                    })
                    .orElseGet(() -> getUrlInformation(shortUrl));
        } catch (Exception e) {
            return handleRedirectError(new IOException("Failed to retrieve URL", e));
        }
    }

    private ResponseEntity<Void> redirectToOriginalUrl(String originalUrl, HttpServletResponse response) throws IOException {
        response.sendRedirect(originalUrl);
        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    private ResponseEntity<UrlShortenResponse> getUrlInformation(String shortUrl) {
        return urlShortenerService.fetchOriginalUrl(shortUrl)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private ResponseEntity<String> handleRedirectError(IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while attempting to redirect to the original URL.");
    }

}
