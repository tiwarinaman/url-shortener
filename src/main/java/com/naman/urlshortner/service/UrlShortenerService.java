package com.naman.urlshortner.service;

import com.naman.urlshortner.domain.response.UrlShortenResponse;

import java.util.Optional;

public interface UrlShortenerService {

    String shortenUrl(String originalUrl, int expirationHours);

    Optional<UrlShortenResponse> fetchOriginalUrl(String shortUrl);

}
