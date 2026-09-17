package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.model.RegisteredUser;

public interface RegisterUserUseCase {
    RegisteredUser register(RegisterCommand command);
}
