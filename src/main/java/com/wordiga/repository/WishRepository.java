package com.wordiga.repository;

import com.wordiga.wish.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByMemberIdAndContentId(Long memberId, String contentId);

    boolean existsByMemberIdAndContentId(Long memberId, String contentId);

    List<Wish> findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(Long memberId, String folderName);

    List<Wish> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    void deleteByMemberIdAndContentId(Long memberId, String contentId);

    @Query("""
                SELECT new com.wordiga.repository.WishRepository$SigunguCountDto(w.content.sigunguCode, COUNT(w))
                FROM Wish w
                WHERE w.memberId = :memberId AND w.content.sigunguCode IS NOT NULL
                GROUP BY w.content.sigunguCode
            """)
    List<SigunguCountDto> countWishesBySigunguGroup(@Param("memberId") Long memberId);

    record SigunguCountDto(String sigunguCode, long count) {
    }
}
