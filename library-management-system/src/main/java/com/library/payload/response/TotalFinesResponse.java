package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalFinesResponse {
    /** Minor units, e.g. 2500 */
    private long total;
    /** Major units, e.g. 25.00 */
    private BigDecimal totalInMajorUnits;
    private String currency;
}
