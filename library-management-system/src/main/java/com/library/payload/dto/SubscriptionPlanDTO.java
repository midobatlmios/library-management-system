package com.library.payload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.library.domain.BillingCycle;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /** Optional on create: generated from the title when missing (e.g. "Gold Annual Plan" -> GOLD_ANNUAL_PLAN). */
    @Size(min = 2, max = 50, message = "Plan code must be 2-50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Plan code must use only A-Z, 0-9 and _")
    private String planCode;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be 0 or more")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimals")
    private BigDecimal price;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. INR")
    private String currency;

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationInDays;

    private BillingCycle billingCycle;

    @Min(value = 0, message = "Trial days must be 0 or more")
    private Integer trialDays;

    @Min(value = 1, message = "Max books allowed must be at least 1")
    private Integer maxBooksAllowed;

    @Min(value = 1, message = "Max borrow days must be at least 1")
    private Integer maxBorrowDays;

    @Size(max = 20, message = "At most 20 features")
    private List<@NotBlank(message = "Feature cannot be empty")
                 @Size(max = 200, message = "Each feature must be at most 200 characters") String> features;

    private Boolean autoRenew;

    private Boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
