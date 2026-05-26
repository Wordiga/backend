package com.wordiga.repository;

import com.wordiga.domain.CartContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartContentRepository extends JpaRepository<CartContent, Long> {

}