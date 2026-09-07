package com.wordiga.tourism.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TourismContentSnapshotRepository extends JpaRepository<TourismContentSnapshot, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TourismContentSnapshot s WHERE s.contentId = :contentId")
    Optional<TourismContentSnapshot> findByContentIdForUpdate(@Param("contentId") String contentId);

    /**
     * 유저 선호 시군구 및 선호 테마 조건에 맞는 후보 관광지 스냅샷 목록 조회 (페이징 지원)
     *
     * 1. sigunguCode 조건: 선호 시군구 코드 매칭 (sigunguCode가 전달되지 않으면 전체 조회)
     * 2. themeCode 조건: 분류체계 1, 2, 3단계 중 하나라도 매칭되거나 contentTypeId와 일치하면 가중치 제공 (themeCode가 없으면 생략)
     */
    @Query("""
                SELECT s FROM TourismContentSnapshot s
                WHERE (:sigunguCode IS NULL OR s.sigunguCode = :sigunguCode)
                  AND (:themeCode IS NULL OR (
                        s.lclsSystem1Code = :themeCode 
                        OR s.lclsSystem2Code = :themeCode 
                        OR s.lclsSystem3Code = :themeCode 
                        OR s.contentTypeId = :themeCode
                      ))
                ORDER BY s.updatedAt DESC
            """)
    Page<TourismContentSnapshot> findCandidates(
            @Param("sigunguCode") String sigunguCode,
            @Param("themeCode") String themeCode,
            Pageable pageable
    );
}
