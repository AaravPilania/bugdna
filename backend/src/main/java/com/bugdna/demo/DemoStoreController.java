package com.bugdna.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/demo-api")
public class DemoStoreController {

    public record Product(Long id, String name, String category, double price, String description, String image) {}
    public record CouponRequest(String code) {}
    public record CheckoutRequest(List<Map<String, Object>> items, String coupon, String customerEmail) {}

    private static final List<Product> PRODUCTS = List.of(
            new Product(1L, "Minimalist Canvas Sneaker", "Footwear", 85.00, "Breathable organic canvas with vulcanized rubber sole.", "/images/sneaker.png"),
            new Product(2L, "Organic Cotton Pullover", "Apparel", 64.00, "Heavyweight 400gsm brushed cotton fleece.", "/images/pullover.png"),
            new Product(3L, "Analog Field Watch", "Accessories", 120.00, "Matte stainless steel with sapphire crystal and NATO strap.", "/images/watch.png"),
            new Product(4L, "Waterproof Commuter Backpack", "Bags", 95.00, "Cordura ballistic nylon with magnetic roll-top closure.", "/images/backpack.png")
    );

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(PRODUCTS);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        if (id == 999L) {
            Map<String, Object> error = new HashMap<>();
            error.put("errorCode", "PRODUCT_NOT_FOUND");
            error.put("message", "Product with id 999 does not exist in inventory catalog");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return PRODUCTS.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("errorCode", "PRODUCT_NOT_FOUND");
                    error.put("message", "Product not found: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                });
    }

    @PostMapping("/cart/coupon")
    public ResponseEntity<?> applyCoupon(@RequestBody CouponRequest request) {
        String code = request.code() != null ? request.code().trim().toUpperCase() : "";
        if ("SAVE20".equals(code)) {
            // Accepts SAVE20 with 200, so the bug hides until checkout!
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "code", "SAVE20",
                    "discountPercent", 20,
                    "message", "20% discount coupon applied successfully"
            ));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", "INVALID_COUPON_FORMAT",
                "message", "Coupon code is invalid or expired"
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        String coupon = request.coupon() != null ? request.coupon().trim().toUpperCase() : "";

        // Main Demo Fault: SAVE20 triggers 500 DEMO_COUPON_RULE_ERROR
        if ("SAVE20".equals(coupon)) {
            Map<String, Object> error = new HashMap<>();
            error.put("errorCode", "DEMO_COUPON_RULE_ERROR");
            error.put("message", "Coupon rule evaluation failed: null pointer in discount calculation pipeline");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "CONFIRMED",
                "message", "Order confirmed successfully"
        ));
    }
}

