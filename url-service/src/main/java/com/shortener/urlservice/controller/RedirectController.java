package com.shortener.urlservice.controller;

import com.shortener.urlservice.dto.ShortUrlDto;
import com.shortener.urlservice.service.UrlShortenerService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
public class RedirectController {

    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortUrlDto> createShortUrl(@RequestParam String longUrl,
                                                   @RequestParam(required = false) Long expiresAtValue) {
        Instant expiresAt = expiresAtValue != null ? Instant.ofEpochSecond(expiresAtValue) : null;
        ShortUrlDto shortUrl = service.shortenUrl(longUrl, expiresAt);
        return new ResponseEntity<>(shortUrl, HttpStatus.CREATED);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectRequest(@PathVariable String shortCode) {
        return service.redirect(shortCode)
                .map(result -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(result.targetUrl()));
                    // 302 is kept (not 301) so the redirect is never irrevocably cached by
                    // the client, while Cache-Control still makes it edge-cacheable by CDNs.
                    headers.setCacheControl(
                            CacheControl.maxAge(result.cacheMaxAgeSeconds(), TimeUnit.SECONDS).cachePublic());
                    return new ResponseEntity<Void>(headers, HttpStatus.FOUND);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
