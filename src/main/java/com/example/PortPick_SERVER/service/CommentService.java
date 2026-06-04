package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.dto.CommentCreateRequest;
import com.example.PortPick_SERVER.dto.CommentResponse;
import com.example.PortPick_SERVER.dto.ReplyCreateRequest;
import com.example.PortPick_SERVER.dto.ReplyResponse;
import com.example.PortPick_SERVER.model.Comment;
import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.Reply;
import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.CommentRepository;
import com.example.PortPick_SERVER.repository.PortfolioRepository;
import com.example.PortPick_SERVER.repository.ReplyRepository;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public CommentService(
            CommentRepository commentRepository,
            ReplyRepository replyRepository,
            PortfolioRepository portfolioRepository,
            UserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.replyRepository = replyRepository;
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String email, Long portfolioId) {
        getPortfolio(portfolioId);
        return commentRepository.findAllByPortfolioIdWithDetails(portfolioId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(String email, Long portfolioId, CommentCreateRequest request) {
        User user = getSignedUpUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);

        if (request == null) {
            throw new IllegalArgumentException("코멘트 정보가 필요합니다.");
        }

        String content = requireContent(request.getContent());
        validateCoordinates(request.getXPercent(), request.getYPercent());

        Comment comment = new Comment(
                portfolio,
                user,
                content,
                request.getXPercent(),
                request.getYPercent()
        );

        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(String email, Long commentId) {
        User user = getUser(email);
        Comment comment = getComment(commentId);
        validateCommentDeletable(user, comment);

        commentRepository.delete(comment);
    }

    @Transactional
    public ReplyResponse createReply(String email, Long commentId, ReplyCreateRequest request) {
        User user = getSignedUpUser(email);
        Comment comment = getComment(commentId);

        if (request == null) {
            throw new IllegalArgumentException("답글 정보가 필요합니다.");
        }

        String content = requireContent(request.getContent());

        Reply reply = new Reply(comment, user, content);
        return ReplyResponse.from(replyRepository.save(reply));
    }

    @Transactional
    public void deleteReply(String email, Long replyId) {
        User user = getUser(email);
        Reply reply = getReply(replyId);
        validateReplyDeletable(user, reply);

        replyRepository.delete(reply);
    }

    @Transactional
    public CommentResponse resolveComment(String email, Long commentId) {
        User user = getUser(email);
        Comment comment = getComment(commentId);
        validatePortfolioOwner(user, comment.getPortfolio());

        if (!comment.isResolved()) {
            comment.resolve();
        }

        return CommentResponse.from(comment);
    }

    private void validateCommentDeletable(User user, Comment comment) {
        if (isCommentAuthor(user, comment) || isPortfolioOwner(user, comment.getPortfolio())) {
            return;
        }
        throw new AccessDeniedException("코멘트 작성자 또는 포트폴리오 소유자만 삭제할 수 있습니다.");
    }

    private void validateReplyDeletable(User user, Reply reply) {
        if (isReplyAuthor(user, reply) || isPortfolioOwner(user, reply.getComment().getPortfolio())) {
            return;
        }
        throw new AccessDeniedException("답글 작성자 또는 포트폴리오 소유자만 삭제할 수 있습니다.");
    }

    private void validatePortfolioOwner(User user, Portfolio portfolio) {
        if (!isPortfolioOwner(user, portfolio)) {
            throw new AccessDeniedException("포트폴리오 소유자만 코멘트를 완료 처리할 수 있습니다.");
        }
    }

    private boolean isCommentAuthor(User user, Comment comment) {
        return comment.getUser().getId().equals(user.getId());
    }

    private boolean isReplyAuthor(User user, Reply reply) {
        return reply.getUser().getId().equals(user.getId());
    }

    private boolean isPortfolioOwner(User user, Portfolio portfolio) {
        return portfolio.getUser().getId().equals(user.getId());
    }

    private void validateCoordinates(Double xPercent, Double yPercent) {
        if (xPercent == null || yPercent == null) {
            throw new IllegalArgumentException("코멘트 좌표(xPercent, yPercent)가 필요합니다.");
        }
        if (xPercent < 0.0 || xPercent > 100.0) {
            throw new IllegalArgumentException("xPercent는 0.0 이상 100.0 이하여야 합니다.");
        }
        if (yPercent < 0.0 || yPercent > 100.0) {
            throw new IllegalArgumentException("yPercent는 0.0 이상 100.0 이하여야 합니다.");
        }
    }

    private String requireContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("내용을 입력해 주세요.");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("내용은 1000자 이하로 입력해 주세요.");
        }
        return trimmed;
    }

    private User getSignedUpUser(String email) {
        User user = getUser(email);
        if (!user.isSignupCompleted()) {
            throw new AccessDeniedException("회원가입을 먼저 완료해 주세요.");
        }
        return user;
    }

    private User getUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Authenticated email is missing.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findWithUserById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findWithDetailsById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("코멘트를 찾을 수 없습니다."));
    }

    private Reply getReply(Long replyId) {
        return replyRepository.findWithDetailsById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("답글을 찾을 수 없습니다."));
    }
}
