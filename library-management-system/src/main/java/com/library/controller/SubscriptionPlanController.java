package com.library.controller;

import com.library.domain.BillingCycle;
import com.library.exception.SubscriptionPlanException;
import com.library.payload.dto.SubscriptionPlanDTO;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PlanStatisticsResponse;
import com.library.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Viewing plans is public (pricing page, even before signup) and shows only active plans.
 * Creating, changing and deleting plans is admin only. Admins also see inactive plans.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlanDTO> createPlan(@Valid @RequestBody SubscriptionPlanDTO dto)
            throws SubscriptionPlanException {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(dto));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionPlanDTO>> createPlans(
            @RequestBody List<@Valid SubscriptionPlanDTO> dtos) throws SubscriptionPlanException {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlans(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanDTO> getPlan(@PathVariable Long id) throws SubscriptionPlanException {
        return ResponseEntity.ok(planService.getPlanById(id, isAdmin()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlanDTO> updatePlan(@PathVariable Long id,
                                                          @Valid @RequestBody SubscriptionPlanDTO dto)
            throws SubscriptionPlanException {
        return ResponseEntity.ok(planService.updatePlan(id, dto));
    }

    /** Soft delete: the plan is hidden from listings. Restore it with PUT and "active": true. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivatePlan(@PathVariable Long id) throws SubscriptionPlanException {
        planService.deactivatePlan(id);
        return ResponseEntity.ok(new ApiResponse("Subscription plan deactivated successfully", true));
    }

    /** Irreversible. */
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id) throws SubscriptionPlanException {
        planService.deletePlanPermanently(id);
        return ResponseEntity.ok(new ApiResponse("Plan permanently deleted", true));
    }

    /** List & filter. Example: /api/subscription-plans?billingCycle=MONTHLY&minPrice=99&maxPrice=999 */
    @GetMapping
    public ResponseEntity<PagesResponse<SubscriptionPlanDTO>> listPlans(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BillingCycle billingCycle,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) throws SubscriptionPlanException {

        // Non-admins always get active plans only, whatever they ask for.
        Boolean activeFilter = isAdmin() ? active : Boolean.TRUE;

        return ResponseEntity.ok(planService.searchPlans(
                activeFilter, billingCycle, minPrice, maxPrice, title, page, size, sortBy, sortDirection));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(planService.getStatistics());
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
