package com.example.PortPick_SERVER.repository;

import com.example.PortPick_SERVER.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select distinct c
            from Comment c
            join fetch c.user
            left join fetch c.replies r
            left join fetch r.user
            where c.portfolio.id = :portfolioId
            order by c.createdAt asc
            """)
    List<Comment> findAllByPortfolioIdWithDetails(@Param("portfolioId") Long portfolioId);

    @Query("""
            select distinct c
            from Comment c
            join fetch c.user
            join fetch c.portfolio p
            join fetch p.user
            left join fetch c.replies r
            left join fetch r.user
            where c.id = :commentId
            """)
    Optional<Comment> findWithDetailsById(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("delete from Comment c where c.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@Param("portfolioId") Long portfolioId);
}
