package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.ProfileUpdateCommand;
import com.fcv.citas.domain.model.UserProfile;

public interface ProfileUseCase {
    UserProfile getProfile(long userId);

    UserProfile updateProfile(long userId, ProfileUpdateCommand command);
}
