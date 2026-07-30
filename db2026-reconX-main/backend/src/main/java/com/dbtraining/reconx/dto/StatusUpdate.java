package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for the trade status sub-resource. */
public record StatusUpdate(
        @NotBlank
        @Pattern(regexp = "PENDING|MATCHED|UNMATCHED|DISPUTED|CANCELLED")
        String status
) {}
