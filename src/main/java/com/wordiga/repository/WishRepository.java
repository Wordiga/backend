package com.wordiga.repository;

import com.wordiga.domain.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByMemberIdAndContentId(Long memberId, String contentId);

    boolean existsByMemberIdAndContentId(Long memberId, String contentId);

    List<Wish> findByMemberIdAndFolderNameOrderByCreatedAtDesc(Long memberId, String folderName);

    @Query("SELECT DISTINCT w.folderName, w.sigunguCode, w.sigunguName, COUNT(w) " +
            "FROM Wish w WHERE w.memberId = :memberId " +
            "GROUP BY w.folderName, w.sigunguCode, w.sigunguName " +
            "ORDER BY w.folderName")
    List<Object[]> findFolderSummariesByMemberId(@Param("memberId") Long memberId);

    void deleteByMemberIdAndContentId(Long memberId, String contentId);
}