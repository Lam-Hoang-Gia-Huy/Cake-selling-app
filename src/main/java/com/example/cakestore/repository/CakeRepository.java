package com.example.cakestore.repository;

import com.example.cakestore.entity.Cake;
import com.example.cakestore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CakeRepository extends JpaRepository<Cake, Long> {
    List<Cake> findByCategory(Category category);
    List<Cake> findByNameContainingIgnoreCase(String name);
}