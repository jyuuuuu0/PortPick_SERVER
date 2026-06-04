package com.example.PortPick_SERVER.repository;

import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.CareerRange;
import com.example.PortPick_SERVER.model.CareerType;
import com.example.PortPick_SERVER.model.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("""
            select p
            from Portfolio p
            join fetch p.user u
            where (:jobRole is null or u.jobRole = :jobRole)
              and (:careerType is null or u.careerType = :careerType)
              and (:careerRange is null or u.careerRange = :careerRange)
            order by p.createdAt desc
            """)
    List<Portfolio> findAllByFilters(
            @Param("jobRole") JobRole jobRole,
            @Param("careerType") CareerType careerType,
            @Param("careerRange") CareerRange careerRange
    );

    @Query("""
            select p
            from Portfolio p
            join fetch p.user
            where p.id = :portfolioId
            """)
    Optional<Portfolio> findWithUserById(@Param("portfolioId") Long portfolioId);
}
