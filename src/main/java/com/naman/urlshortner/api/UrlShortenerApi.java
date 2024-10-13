package com.naman.urlshortner.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping
public interface UrlShortenerApi {

    String SHORTEN_URL_API = "/shorten";
    String HANDLE_URL_REQUEST_API = "/{shortUrl}";

    @PostMapping(SHORTEN_URL_API)
    ResponseEntity<String> shortenUrl(@RequestParam String originalUrl, @RequestParam int expirationHours);

    @GetMapping(HANDLE_URL_REQUEST_API)
    ResponseEntity<?> handleUrlRequest(@PathVariable String shortUrl, HttpServletResponse response);

}
