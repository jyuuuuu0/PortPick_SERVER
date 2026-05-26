package com.example.PortPick_SERVER.repository;

import com.example.PortPick_SERVER.model.PortfolioLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortfolioLikeRepository extends JpaRepository<PortfolioLike, Long> {

    boolean existsByPortfolioIdAndUserId(Long portfolioId, Long userId);

    Optional<PortfolioLike> findByPortfolioIdAndUserId(Long portfolioId, Long userId);

    long countByPortfolioId(Long portfolioId);

    @Query("""
            select pl.portfolio.id as portfolioId, count(pl) as likeCount
            from PortfolioLike pl
            where pl.portfolio.id in :portfolioIds
            group by pl.portfolio.id
            """)
    List<PortfolioLikeCountProjection> countByPortfolioIds(@Param("portfolioIds") Collection<Long> portfolioIds);

    @Query("""
            select pl.portfolio.id
            from PortfolioLike pl
            where pl.user.id = :userId
              and pl.portfolio.id in :portfolioIds
            """)
    List<Long> findLikedPortfolioIdsByUserIdAndPortfolioIds(
            @Param("userId") Long userId,
            @Param("portfolioIds") Collection<Long> portfolioIds
    );

    interface PortfolioLikeCountProjection {
        Long getPortfolioId();

        long getLikeCount();
    }
}
