package com.resume.model.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PurchaseRequest {

    @Min(1)
    private int amount;
}
