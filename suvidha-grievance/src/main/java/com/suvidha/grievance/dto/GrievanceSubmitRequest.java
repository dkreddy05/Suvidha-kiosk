package com.suvidha.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record GrievanceSubmitRequest(
        @NotBlank(message = "category is required") String category,
        @NotBlank(message = "description is required") @Size(min = 10, max = 500, message = "description must be 10-500 characters") String description,
        @URL(message = "photoUrl must be a valid URL") String photoUrl) {
}
