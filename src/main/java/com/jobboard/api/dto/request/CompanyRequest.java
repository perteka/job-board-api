package com.jobboard.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Geçerli bir http(s) URL girin") String website,
        @Size(max = 255) String location,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "Geçerli bir http(s) URL girin") String logoUrl
) {
}
