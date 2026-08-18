package com.wordiga.wish.repository;

import com.wordiga.wish.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByMemberIdAndContent_ContentId(Long memberId, String contentId);

    boolean existsByMemberIdAndContent_ContentId(Long memberId, String contentId);

    List<Wish> findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(Long memberId, String folderName);

    List<Wish> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    void deleteByMemberIdAndContent_ContentId(Long memberId, String contentId);

    @Query("""
                SELECT new com.wordiga.wish.repository.WishRepository$SigunguCountDto(w.content.sigunguCode, COUNT(w))
                FROM Wish w
                WHERE w.memberId = :memberId AND w.content.sigunguCode IS NOT NULL
                GROUP BY w.content.sigunguCode
            """)
    List<SigunguCountDto> countWishesBySigunguGroup(@Param("memberId") Long memberId);

    record SigunguCountDto(String sigunguCode, long count) {
    }
}
