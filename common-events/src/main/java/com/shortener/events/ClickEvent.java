package com.shortener.events;

import java.time.Instant;

public record ClickEvent(String shortCode, Instant clickedAt) {}
