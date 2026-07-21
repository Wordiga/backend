package com.wordiga.service;

import com.wordiga.domain.Wish;
import com.wordiga.dto.wish.WishFolderResponse;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.dto.wish.WishResponse;
import com.wordiga.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {

    /**
     * 충남 16개 시군구 코드 → 이름 매핑
     */
    private static final Map<String, String> CHUNGNAM_SIGUNGU_MAP = Map.ofEntries(
            Map.entry("110", "천안시"),
            Map.entry("130", "공주시"),
            Map.entry("140", "보령시"),
            Map.entry("150", "아산시"),
            Map.entry("160", "서산시"),
            Map.entry("170", "논산시"),
            Map.entry("180", "계룡시"),
            Map.entry("190", "당진시"),
            Map.entry("310", "금산군"),
            Map.entry("330", "부여군"),
            Map.entry("340", "서천군"),
            Map.entry("350", "청양군"),
            Map.entry("360", "홍성군"),
            Map.entry("370", "예산군"),
            Map.entry("380", "태안군"),
            Map.entry("400", "청양군")  // 필요 시 수정
    );
    private final WishRepository wishRepository;

    /**
     * 위시 등록
     * - 이미 등록된 contentId이면 무시 (중복 방지)
     * - 시군구 코드로 자동 폴더링
     */
    @Transactional
    public WishResponse addWish(Long memberId, WishRequest request) {
        // 중복 체크
        if (wishRepository.existsByMemberIdAndContentId(memberId, request.getContentId())) {
            // 이미 존재하면 기존 것 반환
            Wish existing = wishRepository.findByMemberIdAndContentId(memberId, request.getContentId())
                    .orElseThrow();
            return WishResponse.from(existing);
        }

        // 폴더명 결정: 시군구 코드로 자동 분류
        String sigunguName = resolveSigunguName(request.getSigunguCode());
        String folderName = sigunguName != null ? sigunguName : "기본 위시리스트";

        Wish wish = Wish.create(
                memberId,
                request.getContentId(),
                request.getContentTypeId(),
                request.getTitle(),
                request.getFirstimage(),
                request.getAddr1(),
                request.getSigunguCode(),
                sigunguName,
                folderName
        );

        Wish saved = wishRepository.save(wish);
        return WishResponse.from(saved);
    }

    /**
     * 위시 삭제
     */
    @Transactional
    public void removeWish(Long memberId, String contentId) {
        wishRepository.deleteByMemberIdAndContentId(memberId, contentId);
    }

    /**
     * 위시 폴더 목록 조회
     * - 충남 16개 시군구 자동 폴더 + 기본 위시리스트
     */
    public List<WishFolderResponse> getWishFolders(Long memberId) {
        List<Object[]> summaries = wishRepository.findFolderSummariesByMemberId(memberId);

        return summaries.stream()
                .map(row -> WishFolderResponse.builder()
                        .folderName((String) row[0])
                        .sigunguCode((String) row[1])
                        .count((Long) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 폴더(지역) 내 위시 목록 조회
     */
    public List<WishResponse> getWishesByFolder(Long memberId, String folderName) {
        List<Wish> wishes = wishRepository.findByMemberIdAndFolderNameOrderByCreatedAtDesc(memberId, folderName);
        return wishes.stream()
                .map(WishResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 시군구 코드 → 시군구명 변환
     */
    private String resolveSigunguName(String sigunguCode) {
        if (sigunguCode == null || sigunguCode.isBlank()) {
            return null;
        }
        return CHUNGNAM_SIGUNGU_MAP.getOrDefault(sigunguCode, null);
    }
}