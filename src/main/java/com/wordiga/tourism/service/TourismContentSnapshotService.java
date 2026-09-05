package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshot.ContentSnapshotValues;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;
import static com.wordiga.global.util.KtoUtils.parseKtoDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourismContentSnapshotService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final LocalTime SYNC_START = LocalTime.of(7, 30);
    private final TourismContentDetailService detailService;
    private final TourismApiClient tourismApiClient;
    private final TourismContentSnapshotRepository snapshotRepository;

    @Transactional
    public void synchronizeReferenced(Collection<String> contentIds) {
        synchronizeReferenced(contentIds, LocalDateTime.now(KOREA));
    }

    void synchronizeReferenced(Collection<String> contentIds, LocalDateTime now) {
        if (now.toLocalTime().isBefore(SYNC_START)) return;

        contentIds.stream().distinct().forEach(contentId -> snapshotRepository.findByContentIdForUpdate(contentId)
                .filter(snapshot -> snapshot.getLastSyncedAt() == null
                        || !snapshot.getLastSyncedAt().toLocalDate().equals(now.toLocalDate()))
                .ifPresent(snapshot -> synchronize(snapshot, now)));
    }

    private void synchronize(TourismContentSnapshot snapshot, LocalDateTime now) {
        try {
            ContentDetailDto content = detailService.getCommonDetail(snapshot.getContentId());
            if (!Objects.equals(snapshot.getContentId(), content.getContentid())) {
                throw new IllegalStateException("관광 콘텐츠 공통정보 응답의 식별자가 일치하지 않습니다.");
            }
            String sigunguName = fetchSigunguName(content.getLDongSignguCd());
            ContentSnapshotValues values = values(content, sigunguName);
            if (snapshot.getSourceModifiedAt() != null
                    && values.sourceModifiedAt().isBefore(snapshot.getSourceModifiedAt())) {
                snapshot.markSynchronized(now);
                log.warn("[TourismSnapshot] 원천 수정일이 저장값보다 과거여서 기존값 유지: contentId={}",
                        snapshot.getContentId());
                return;
            }
            snapshot.update(values, now);
        } catch (Exception exception) {
            log.warn("[TourismSnapshot] 동기화 실패, 기존값 유지: contentId={}", snapshot.getContentId(), exception);
        }
    }

    private String fetchSigunguName(String sigunguCode) {
        List<SigunguItem> sigunguItems = tourismApiClient.fetchSigunguList();
        if (!StringUtils.hasText(sigunguCode)) return null;
        return sigunguItems.stream()
                .filter(item -> sigunguCode.equals(item.getCode()) && StringUtils.hasText(item.getName()))
                .map(SigunguItem::getName)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("법정동 시군구 응답에서 코드를 찾을 수 없습니다."));
    }

    private ContentSnapshotValues values(ContentDetailDto content, String sigunguName) {
        if (!StringUtils.hasText(content.getContentid()) || !StringUtils.hasText(content.getTitle())) {
            throw new IllegalStateException("관광 콘텐츠 공통정보 응답이 불완전합니다.");
        }
        return new ContentSnapshotValues(
                content.getContenttypeid(), content.getTitle(), content.getFirstimage(), content.getAddr1(),
                parseBigDecimal(content.getMapx()), parseBigDecimal(content.getMapy()), content.getLDongSignguCd(),
                sigunguName, content.getLclsSystm1(), content.getLclsSystm2(), content.getLclsSystm3(),
                sourceModifiedAt(content.getModifiedtime())
        );
    }

    private LocalDateTime sourceModifiedAt(String value) {
        LocalDateTime parsed = parseKtoDateTime(value);
        if (parsed == null) {
            throw new IllegalStateException("관광 콘텐츠 원천 수정일 형식이 올바르지 않습니다.");
        }
        return parsed;
    }
}
