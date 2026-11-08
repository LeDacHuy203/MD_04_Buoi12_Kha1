package com.example.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống")
        String name,

        String description,

        @NotNull(message = "Giá sản phẩm không được null")
        @Positive(message = "Giá sản phẩm phải lớn hơn 0")
        BigDecimal price,

        String imageUrl
) {}
