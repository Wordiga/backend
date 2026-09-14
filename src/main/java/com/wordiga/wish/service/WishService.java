package com.wordiga.wish.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;
import com.wordiga.wish.Wish;
import com.wordiga.wish.dto.WishFolderResponse;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.wish.dto.WishResponse;
import com.wordiga.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;
import static com.wordiga.global.util.KtoUtils.parseKtoDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {

    private final WishRepository wishRepository;
    private final WishPreferenceCacheService preferenceCacheService;
    private final TourismContentDetailService tourismContentDetailService;
    private final TourismApiClient tourismApiClient;
    private final TourismContentSnapshotRepository snapshotRepository;
    private final TourismContentSnapshotService snapshotService;

    /**
     * 위시 등록
     * - 이미 등록된 contentId이면 무시 (중복 방지)
     * - 시군구 코드로 자동 폴더링
     */
    @Transactional
    public WishResponse addWish(Long memberId, WishRequest request) {
        // 중복 체크
        if (wishRepository.existsByMemberIdAndContent_ContentId(memberId, request.getContentId())) {
            // 이미 존재하면 기존 것 반환
            Wish existing = wishRepository.findByMemberIdAndContent_ContentId(memberId, request.getContentId())
                    .orElseThrow();
            snapshotService.synchronizeReferenced(List.of(request.getContentId()));
            return WishResponse.from(existing);
        }

        ContentDetailDto content = tourismContentDetailService.getCommonDetail(request.getContentId());
        String sigunguName = resolveSigunguName(content.getLDongSignguCd());
        String folderName = sigunguName != null ? sigunguName : "기본 위시리스트";

        TourismContentSnapshot contentSnapshot = snapshotRepository.findById(content.getContentid())
                .orElseGet(() -> snapshotRepository.save(TourismContentSnapshot.builder()
                        .contentId(content.getContentid())
                        .contentTypeId(content.getContenttypeid())
                        .title(content.getTitle())
                        .firstimage(content.getFirstimage())
                        .addr1(content.getAddr1())
                        .mapx(parseBigDecimal(content.getMapx()))
                        .mapy(parseBigDecimal(content.getMapy()))
                        .sigunguCode(content.getLDongSignguCd())
                        .sigunguName(sigunguName)
                        .lclsSystem1Code(content.getLclsSystm1())
                        .lclsSystem2Code(content.getLclsSystm2())
                        .lclsSystem3Code(content.getLclsSystm3())
                        .sourceModifiedAt(parseKtoDateTime(content.getModifiedtime()))
                        .updatedAt(java.time.LocalDateTime.now())
                        .build()));

        Wish wish = Wish.create(
                memberId,
                contentSnapshot,
                folderName
        );

        Wish saved = wishRepository.save(wish);
        preferenceCacheService.evictUserPreferenceCache(memberId);
        return WishResponse.from(saved);
    }

    /**
     * 위시 삭제
     */
    @Transactional
    public void removeWish(Long memberId, String contentId) {
        wishRepository.deleteByMemberIdAndContent_ContentId(memberId, contentId);
        preferenceCacheService.evictUserPreferenceCache(memberId);
    }

    /**
     * 위시 폴더 목록 조회
     * - 충남 16개 시군구 자동 폴더 + 기본 위시리스트
     */
    @Transactional
    public List<WishFolderResponse> getWishFolders(Long memberId) {
        List<Wish> wishes = wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId);
        snapshotService.synchronizeReferenced(wishes.stream().map(wish -> wish.getContent().getContentId()).toList());
        Map<String, List<Wish>> folders = wishes.stream()
                .collect(Collectors.groupingBy(Wish::getFolderName, LinkedHashMap::new, Collectors.toList()));
        return folders.values().stream().sorted(java.util.Comparator.comparing(folder -> folder.getFirst().getFolderName()))
                .map(folder -> WishFolderResponse.builder()
                        .folderName(folder.getFirst().getFolderName()).lDongSignguCd(folder.getFirst().getSigunguCode())
                        .count((long) folder.size())
                        .thumbnailUrl(folder.getFirst().getContent().getFirstimage()).build()).toList();
    }

    /**
     * 특정 폴더(지역) 내 위시 목록 조회
     */
    @Transactional
    public List<WishResponse> getWishesByFolder(Long memberId, String folderName) {
        List<Wish> wishes = wishRepository.findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(memberId, folderName);
        snapshotService.synchronizeReferenced(wishes.stream().map(wish -> wish.getContent().getContentId()).toList());
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
        return tourismApiClient.fetchSigunguList().stream()
                .filter(item -> sigunguCode.equals(item.getCode()))
                .map(com.wordiga.global.client.dto.SigunguItem::getName)
                .findFirst()
                .orElse(null);
    }

}
