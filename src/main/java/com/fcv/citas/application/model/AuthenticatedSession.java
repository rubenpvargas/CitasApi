package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.User;

public record AuthenticatedSession(User user, TokenPair tokens) {
}
