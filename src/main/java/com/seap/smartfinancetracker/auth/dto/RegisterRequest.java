package com.seap.smartfinancetracker.auth.dto;

import com.seap.smartfinancetracker.auth.constant.AuthValidationMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request object containing information required for user registration.
 *
 * @param email the user's email address
 * @param password the user's password
 * @param fullName the user's full name
 */
public record RegisterRequest(

        @NotBlank(message = AuthValidationMessage.EMAIL_REQUIRED)
        @Email(message = AuthValidationMessage.EMAIL_INVALID)
        @Size(max = 100, message = AuthValidationMessage.EMAIL_MAX_LENGTH)
        String email,

        @NotBlank(message = AuthValidationMessage.PASSWORD_REQUIRED)
        @Size(min = 8, max = 50, message = AuthValidationMessage.PASSWORD_LENGTH)
        String password,

        @NotBlank(message = AuthValidationMessage.FULL_NAME_REQUIRED)
        @Size(min = 2, max = 100, message = AuthValidationMessage.FULL_NAME_LENGTH)
        String fullName) {
}
