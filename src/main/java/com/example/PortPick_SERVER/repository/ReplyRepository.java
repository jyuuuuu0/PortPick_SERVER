package com.example.PortPick_SERVER.repository;

import com.example.PortPick_SERVER.model.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @Query("""
            select r
            from Reply r
            join fetch r.user
            join fetch r.comment c
            join fetch c.portfolio p
            join fetch p.user
            where r.id = :replyId
            """)
    Optional<Reply> findWithDetailsById(@Param("replyId") Long replyId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("delete from Reply r where r.comment.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@Param("portfolioId") Long portfolioId);
}
