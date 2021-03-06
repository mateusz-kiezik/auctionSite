package com.auction.data.repositories;

import com.auction.data.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findAllById(Long id);
    List<Category> findAllByParentId(Long id);
}
