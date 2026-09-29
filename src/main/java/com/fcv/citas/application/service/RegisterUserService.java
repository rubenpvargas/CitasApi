package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.DuplicateIdentifierException;
import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.model.RegisteredUser;
import com.fcv.citas.application.port.in.RegisterUserUseCase;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.User;

import java.util.Locale;
import java.util.Set;

public final class RegisterUserService implements RegisterUserUseCase {
    private final UserRepositoryPort users;
    private final PasswordHashPort passwords;
    private final TransactionPort transactions;

    public RegisterUserService(UserRepositoryPort users, PasswordHashPort passwords,
                               TransactionPort transactions) {
        this.users = users;
        this.passwords = passwords;
        this.transactions = transactions;
    }

    @Override
    public RegisteredUser register(RegisterCommand command) {
        String email = normalizeEmail(command.email());
        String documentType = command.documentType().trim().toUpperCase(Locale.ROOT);
        String documentNumber = command.documentNumber().trim().toUpperCase(Locale.ROOT);

        return transactions.required(() -> {
            if (users.existsByEmail(email) || users.existsByDocument(documentType, documentNumber)) {
                throw new DuplicateIdentifierException();
            }
            User saved = users.save(new User(null, command.firstName().trim(), command.lastName().trim(),
                    documentType, documentNumber, email, command.phone().trim(),
                    passwords.hash(command.password()), true, Set.of("USER")));
            return new RegisteredUser(saved.id(), saved.firstName(), saved.lastName(),
                    saved.documentType(), saved.documentNumber(), saved.email(), saved.phone(), saved.roles());
        });
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
