package com.example.support.api;

import java.util.List;

public record AskResponse(String answer, List<String> sources, String safetyStatus) {}
