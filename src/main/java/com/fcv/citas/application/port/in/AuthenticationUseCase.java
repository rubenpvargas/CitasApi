package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.AuthenticatedSession;
import com.fcv.citas.application.model.TokenPair;

public interface AuthenticationUseCase {
    AuthenticatedSession login(String email, String password);
    TokenPair refresh(String refreshToken);
    void logout(String refreshToken);
}
