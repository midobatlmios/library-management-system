package com.library.payload.dto;

import com.library.domain.PaymentReferenceType;
import com.library.domain.PaymentStatus;
import com.library.model.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentDTO {
    private String paymentId;
    private Long userId;
    private PaymentReferenceType referenceType;
    private Long referenceId;
    private String description;
    private Long amount;
    private BigDecimal amountInMajorUnits;
    private String currency;
    private PaymentStatus status;
    private String provider;
    private String providerTransactionId;
    private Integer attempts;
    private String lastError;
    private String cardLast4;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static PaymentDTO from(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(p.getPaymentId());
        dto.setUserId(p.getUser().getId());
        dto.setReferenceType(p.getReferenceType());
        dto.setReferenceId(p.getReferenceId());
        dto.setDescription(p.getDescription());
        dto.setAmount(p.getAmount());
        dto.setAmountInMajorUnits(p.getAmountInMajorUnits());
        dto.setCurrency(p.getCurrency());
        dto.setStatus(p.getStatus());
        dto.setProvider(p.getProvider());
        dto.setProviderTransactionId(p.getProviderTransactionId());
        dto.setAttempts(p.getAttempts());
        dto.setLastError(p.getLastError());
        dto.setCardLast4(p.getCardLast4());
        dto.setExpiresAt(p.getExpiresAt());
        dto.setCompletedAt(p.getCompletedAt());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }

    public static List<PaymentDTO> fromList(List<Payment> list) {
        return list.stream().map(PaymentDTO::from).toList();
    }
}
