package com.library.service.impl;

import com.library.domain.BillingCycle;
import com.library.exception.SubscriptionPlanException;
import com.library.mapper.SubscriptionPlanMapper;
import com.library.model.SubscriptionPlan;
import com.library.payload.dto.SubscriptionPlanDTO;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PlanStatisticsResponse;
import com.library.repository.SubscriptionPlanRepository;
import com.library.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "price", "durationInDays", "billingCycle", "createdAt", "updatedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.default-currency:INR}")
    private String defaultCurrency;

    // ---------------------------------------------------------------- create

    @Override
    @Transactional
    public SubscriptionPlanDTO createPlan(SubscriptionPlanDTO dto) throws SubscriptionPlanException {
        return SubscriptionPlanMapper.toDTO(planRepository.save(buildNewPlan(dto)));
    }

    /** All or nothing: if one plan is invalid, none are saved. */
    @Override
    @Transactional(rollbackFor = SubscriptionPlanException.class)
    public List<SubscriptionPlanDTO> createPlans(List<SubscriptionPlanDTO> dtos) throws SubscriptionPlanException {
        if (dtos == null || dtos.isEmpty()) {
            throw new SubscriptionPlanException("At least one plan is required");
        }

        Set<String> titlesInRequest = new HashSet<>();
        Set<String> codesInRequest = new HashSet<>();
        List<SubscriptionPlan> plans = new ArrayList<>();

        for (SubscriptionPlanDTO dto : dtos) {
            SubscriptionPlan plan = buildNewPlan(dto);
            if (!titlesInRequest.add(plan.getTitle().toLowerCase(Locale.ROOT))) {
                throw new SubscriptionPlanException(
                        "Title '" + plan.getTitle() + "' appears twice in the request", HttpStatus.CONFLICT);
            }
            if (!codesInRequest.add(plan.getPlanCode())) {
                throw new SubscriptionPlanException(
                        "Plan code '" + plan.getPlanCode() + "' appears twice in the request", HttpStatus.CONFLICT);
            }
            plans.add(plan);
        }
        return SubscriptionPlanMapper.toDTOList(planRepository.saveAll(plans));
    }

    private SubscriptionPlan buildNewPlan(SubscriptionPlanDTO dto) throws SubscriptionPlanException {
        String title = dto.getTitle().trim();
        if (planRepository.existsByTitleIgnoreCase(title)) {
            throw duplicateTitle(title);
        }

        String code = dto.getPlanCode() != null ? dto.getPlanCode() : codeFromTitle(title);
        if (planRepository.existsByPlanCode(code)) {
            throw new SubscriptionPlanException(
                    "Plan with code '" + code + "' already exists.", HttpStatus.CONFLICT);
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCurrency(defaultCurrency);
        SubscriptionPlanMapper.copyToEntity(dto, plan);
        plan.setPlanCode(code);
        validateRules(plan);
        return plan;
    }

    // ---------------------------------------------------------------- read

    @Override
    public SubscriptionPlanDTO getPlanById(Long id, boolean includeInactive) throws SubscriptionPlanException {
        SubscriptionPlan plan = findPlan(id);
        if (!includeInactive && !plan.getActive()) {
            throw notFound(id);
        }
        return SubscriptionPlanMapper.toDTO(plan);
    }

    @Override
    public PagesResponse<SubscriptionPlanDTO> searchPlans(Boolean active, BillingCycle billingCycle,
                                                          BigDecimal minPrice, BigDecimal maxPrice, String title,
                                                          int page, int size, String sortBy, String sortDirection)
            throws SubscriptionPlanException {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new SubscriptionPlanException("minPrice cannot be greater than maxPrice");
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new SubscriptionPlanException("Cannot sort by '" + sortBy + "'. Allowed: " + SORTABLE_FIELDS);
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new SubscriptionPlanException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }

        Sort sort = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        String titleFilter = (title == null || title.isBlank()) ? null : title.trim();

        Page<SubscriptionPlan> result = planRepository.searchPlans(
                active, billingCycle, minPrice, maxPrice, titleFilter, PageRequest.of(page, size, sort));

        return new PagesResponse<>(
                SubscriptionPlanMapper.toDTOList(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast(),
                result.isFirst(),
                result.isEmpty());
    }

    @Override
    public PlanStatisticsResponse getStatistics() {
        long total = planRepository.count();
        long active = planRepository.countByActiveTrue();
        LocalDate today = LocalDate.now();

        Map<String, Long> byPlan = new TreeMap<>();
        for (Object[] row : subscriptionRepository.countValidByPlan(today)) {
            byPlan.put((String) row[0], (Long) row[1]);
        }
        return new PlanStatisticsResponse(total, active, total - active,
                subscriptionRepository.countValid(today), byPlan);
    }

    // ---------------------------------------------------------------- update / delete

    @Override
    @Transactional
    public SubscriptionPlanDTO updatePlan(Long id, SubscriptionPlanDTO dto) throws SubscriptionPlanException {
        SubscriptionPlan plan = findPlan(id);

        String title = dto.getTitle().trim();
        if (planRepository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
            throw duplicateTitle(title);
        }
        if (dto.getPlanCode() != null && planRepository.existsByPlanCodeAndIdNot(dto.getPlanCode(), id)) {
            throw new SubscriptionPlanException(
                    "Plan with code '" + dto.getPlanCode() + "' already exists.", HttpStatus.CONFLICT);
        }

        // Setting "active": true here restores a soft-deleted plan.
        SubscriptionPlanMapper.copyToEntity(dto, plan);
        validateRules(plan);
        return SubscriptionPlanMapper.toDTO(planRepository.save(plan));
    }

    @Override
    @Transactional
    public void deactivatePlan(Long id) throws SubscriptionPlanException {
        SubscriptionPlan plan = findPlan(id);
        plan.setActive(false);
        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void deletePlanPermanently(Long id) throws SubscriptionPlanException {
        SubscriptionPlan plan = findPlan(id);
        planRepository.delete(plan);
        // Flush now so a "plan still used by subscriptions" error becomes a clean 409 here.
        planRepository.flush();
    }

    // ---------------------------------------------------------------- helpers

    /** Rules that involve more than one field, so they can't be simple annotations on the DTO. */
    private void validateRules(SubscriptionPlan plan) throws SubscriptionPlanException {
        if (plan.getTrialDays() != null && plan.getTrialDays() > plan.getDurationInDays()) {
            throw new SubscriptionPlanException("Trial period (" + plan.getTrialDays()
                    + " days) must not exceed plan validity (" + plan.getDurationInDays() + " days)");
        }
    }

    private SubscriptionPlan findPlan(Long id) throws SubscriptionPlanException {
        return planRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static SubscriptionPlanException notFound(Long id) {
        return new SubscriptionPlanException("Subscription plan not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    private static SubscriptionPlanException duplicateTitle(String title) {
        return new SubscriptionPlanException(
                "Plan with title '" + title + "' already exists.", HttpStatus.CONFLICT);
    }

    /** "Gold Annual Plan" -> "GOLD_ANNUAL_PLAN" */
    private static String codeFromTitle(String title) throws SubscriptionPlanException {
        String code = title.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (code.length() > 50) {
            code = code.substring(0, 50).replaceAll("_+$", "");
        }
        if (code.length() < 2) {
            throw new SubscriptionPlanException("Could not build a plan code from the title; please send planCode");
        }
        return code;
    }
}
