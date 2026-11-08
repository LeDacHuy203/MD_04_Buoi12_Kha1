package com.example.productservice;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductUpdateRequest;
import com.example.productservice.service.ProductService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductServiceApplicationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @Order(1)
    void testCacheAsidePerformanceAndEviction() {
        Long productId = 1L;

        // Xóa cache trước để test chính xác
        redisTemplate.delete("products::" + productId);

        // Lần 1: Cache Miss -> Đọc DB (mô phỏng ~200ms)
        long start1 = System.currentTimeMillis();
        ProductResponse response1 = productService.getProductById(productId);
        long duration1 = System.currentTimeMillis() - start1;

        System.out.println("===> [TEST LẦN 1 - CACHE MISS] Thời gian: " + duration1 + " ms");
        assertThat(response1).isNotNull();
        assertThat(duration1).isGreaterThanOrEqualTo(190); // ~200ms

        // Kiểm tra Redis đã lưu key và có TTL ~30 phút (1800 giây)
        Boolean hasKey = redisTemplate.hasKey("products::" + productId);
        assertThat(hasKey).isTrue();

        Long ttl = redisTemplate.getExpire("products::" + productId, TimeUnit.SECONDS);
        System.out.println("===> [TEST REDIS TTL] TTL còn lại: " + ttl + " giây (~30 phút)");
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(1700L).isLessThanOrEqualTo(1800L);

        // Lần 2: Cache Hit -> Đọc từ Redis (<10ms)
        long start2 = System.currentTimeMillis();
        ProductResponse response2 = productService.getProductById(productId);
        long duration2 = System.currentTimeMillis() - start2;

        System.out.println("===> [TEST LẦN 2 - CACHE HIT] Thời gian: " + duration2 + " ms");
        assertThat(response2).isNotNull();
        assertThat(response2.name()).isEqualTo(response1.name());
        assertThat(duration2).isLessThan(50); // thường <10ms

        // Test @CacheEvict khi Update sản phẩm
        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Bàn phím cơ RK84 V2 Pro",
                "Đã cập nhật tính năng mới",
                new BigDecimal("1350000"),
                "https://example.com/rk84-v2.jpg"
        );
        productService.updateProduct(productId, updateRequest);

        // Sau khi update, key trong Redis phải bị xóa (Cache Invalidation)
        Boolean existsAfterUpdate = redisTemplate.hasKey("products::" + productId);
        System.out.println("===> [TEST CACHE EVICT] Key trong Redis sau update: " + existsAfterUpdate);
        assertThat(existsAfterUpdate).isFalse();

        // Lần 3: Truy vấn lại sau update -> Cache Miss lại để lấy dữ liệu mới
        ProductResponse response3 = productService.getProductById(productId);
        assertThat(response3.name()).isEqualTo("Bàn phím cơ RK84 V2 Pro");
    }
}
