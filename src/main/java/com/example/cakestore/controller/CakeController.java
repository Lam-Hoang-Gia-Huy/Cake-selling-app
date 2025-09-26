package com.example.cakestore.controller;

import com.example.cakestore.entity.Cake;
import com.example.cakestore.entity.Category;
import com.example.cakestore.entity.Discount;
import com.example.cakestore.repository.CakeRepository;
import com.example.cakestore.repository.CategoryRepository;
import com.example.cakestore.repository.DiscountRepository;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cakes")
public class CakeController {

    @Autowired
    private CakeRepository cakeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private Cloudinary cloudinary;

    @GetMapping
    public List<Cake> getAllCakes() {
        return cakeRepository.findAll();
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getCakesByCategory(@PathVariable Long categoryId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
            List<Cake> cakes = cakeRepository.findByCategory(category);
            return ResponseEntity.ok(cakes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCake(
            @RequestParam("name") String name,
            @RequestParam("price") double price,
            @RequestParam("description") String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "discountType", required = false) String discountType,
            @RequestParam(value = "discountValue", required = false) Double discountValue,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            Cake cake = new Cake();
            cake.setName(name);
            cake.setPrice(price);
            cake.setDescription(description);

            if (categoryId != null) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
                cake.setCategory(category);
            }

            if (discountType != null && discountValue != null) {
                Discount discount = new Discount();
                discount.setType(Discount.DiscountType.valueOf(discountType.toUpperCase()));
                discount.setValue(discountValue);
                discountRepository.save(discount);
                cake.setDiscount(discount);
            }

            List<String> imageUrls = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    Map uploadResult = cloudinary.uploader().upload(image.getBytes(), Map.of("folder", "cakes"));
                    imageUrls.add((String) uploadResult.get("secure_url"));
                }
            }
            cake.setImageUrls(imageUrls);

            Cake savedCake = cakeRepository.save(cake);
            return ResponseEntity.ok(savedCake);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCakeById(@PathVariable Long id) {
        return cakeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCake(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("price") double price,
            @RequestParam("description") String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "discountType", required = false) String discountType,
            @RequestParam(value = "discountValue", required = false) Double discountValue,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            return cakeRepository.findById(id).map(cake -> {
                cake.setName(name);
                cake.setPrice(price);
                cake.setDescription(description);

                if (categoryId != null) {
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
                    cake.setCategory(category);
                } else {
                    cake.setCategory(null);
                }

                if (discountType != null && discountValue != null) {
                    Discount discount = cake.getDiscount() != null ? cake.getDiscount() : new Discount();
                    discount.setType(Discount.DiscountType.valueOf(discountType.toUpperCase()));
                    discount.setValue(discountValue);
                    discountRepository.save(discount);
                    cake.setDiscount(discount);
                } else {
                    cake.setDiscount(null);
                }

                if (images != null && !images.isEmpty()) {
                    List<String> imageUrls = new ArrayList<>(cake.getImageUrls());
                    for (MultipartFile image : images) {
                        Map uploadResult;
                        try {
                            uploadResult = cloudinary.uploader().upload(image.getBytes(), Map.of("folder", "cakes"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        imageUrls.add((String) uploadResult.get("secure_url"));
                    }
                    cake.setImageUrls(imageUrls);
                }

                Cake updatedCake = cakeRepository.save(cake);
                return ResponseEntity.ok(updatedCake);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCake(@PathVariable Long id) {
        return cakeRepository.findById(id).map(cake -> {
            cakeRepository.delete(cake);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    // New search endpoint
    @GetMapping("/search")
    public ResponseEntity<List<Cake>> searchCakes(@RequestParam("name") String name) {
        try {
            List<Cake> cakes = cakeRepository.findByNameContainingIgnoreCase(name);
            return ResponseEntity.ok(cakes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/images/{imageIndex}")
    public ResponseEntity<?> deleteCakeImage(@PathVariable Long id, @PathVariable int imageIndex) {
        try {
            return cakeRepository.findById(id).map(cake -> {
                List<String> imageUrls = cake.getImageUrls();
                if (imageIndex >= 0 && imageIndex < imageUrls.size()) {
                    imageUrls.remove(imageIndex);
                    cake.setImageUrls(imageUrls);
                    cakeRepository.save(cake);
                    return ResponseEntity.ok(cake);
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid image index"));
                }
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}