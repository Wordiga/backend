package com.wordiga.service;

import com.wordiga.domain.Wish;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.wish.WishFolderResponse;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.dto.wish.WishResponse;
import com.wordiga.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {

    private final WishRepository wishRepository;
    private final TourismContentDetailService tourismContentDetailService;

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

        ContentDetailDto content = tourismContentDetailService.getCommonDetail(request.getContentId());

        String sigunguName = resolveSigunguName(content.getLDongSignguCd());
        String folderName = sigunguName != null ? sigunguName : "기본 위시리스트";

        Wish wish = Wish.create(
                memberId,
                content.getContentid(),
                content.getContenttypeid(),
                content.getTitle(),
                content.getFirstimage(),
                content.getAddr1(),
                parseCoordinate(content.getMapx()),
                parseCoordinate(content.getMapy()),
                content.getLDongSignguCd(),
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
        Map<String, List<Wish>> folders = wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                .collect(Collectors.groupingBy(Wish::getFolderName, LinkedHashMap::new, Collectors.toList()));
        return folders.values().stream().sorted(java.util.Comparator.comparing(wishes -> wishes.getFirst().getFolderName()))
                .map(wishes -> WishFolderResponse.builder()
                .folderName(wishes.getFirst().getFolderName()).lDongSignguCd(wishes.getFirst().getSigunguCode())
                .count((long) wishes.size()).thumbnailUrl(wishes.getFirst().getFirstimage()).build()).toList();
    }

    /**
     * 특정 폴더(지역) 내 위시 목록 조회
     */
    public List<WishResponse> getWishesByFolder(Long memberId, String folderName) {
        List<Wish> wishes = wishRepository.findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(memberId, folderName);
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
        return ChungnamSigungu.NAMES.get(sigunguCode);
    }

    private BigDecimal parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
