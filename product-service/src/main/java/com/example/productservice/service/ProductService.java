package com.example.productservice.service;

import com.example.productservice.dto.ProductCreateRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductUpdateRequest;

public interface ProductService {
    ProductResponse getProductById(Long id);
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);
    ProductResponse createProduct(ProductCreateRequest request);
    void deleteProduct(Long id);
}
