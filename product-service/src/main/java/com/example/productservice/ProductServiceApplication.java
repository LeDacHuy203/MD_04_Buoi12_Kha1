package com.example.productservice;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initSampleData(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                Product sampleProduct = Product.builder()
                        .name("Bàn phím cơ không dây RK84")
                        .description("Bàn phím cơ Bluetooth 3 chế độ kết nối, switch Gateron Pro Yellow")
                        .price(new BigDecimal("1250000"))
                        .imageUrl("https://example.com/rk84.jpg")
                        .build();

                productRepository.save(sampleProduct);
                System.out.println("===> [INIT DATA] Đã tạo sản phẩm mẫu ID = 1 thành công!");
            }
        };
    }
}
