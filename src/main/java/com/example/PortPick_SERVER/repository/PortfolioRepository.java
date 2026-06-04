package com.example.PortPick_SERVER.repository;

import com.example.PortPick_SERVER.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
