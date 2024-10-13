package com.naman.urlshortner.domain.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UrlShortenResponse(
        String originalUrl,
        LocalDateTime expirationTime
) {
}
