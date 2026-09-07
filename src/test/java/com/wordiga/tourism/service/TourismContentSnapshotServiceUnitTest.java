package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourismContentSnapshotServiceUnitTest {

    @Mock
    TourismContentDetailService detailService;
    @Mock
    TourismApiClient tourismApiClient;
    @Mock
    TourismContentSnapshotRepository snapshotRepository;

    TourismContentSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new TourismContentSnapshotService(detailService, tourismApiClient, snapshotRepository);
    }

    @Test
    void refreshesCommonAndSigunguDataAfterSevenThirtyOncePerDay() {
        TourismContentSnapshot snapshot = snapshot(null);
        ContentDetailDto content = content();
        SigunguItem sigungu = new SigunguItem();
        sigungu.setCode("200");
        sigungu.setName("아산시");
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content);
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu));
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 7, 30);

        service.synchronizeReferenced(List.of("126508", "126508"), now);
        service.synchronizeReferenced(List.of("126508"), now.plusHours(1));

        assertThat(snapshot.getTitle()).isEqualTo("현충사 최신");
        assertThat(snapshot.getFirstimage()).isEqualTo("https://example.com/new.jpg");
        assertThat(snapshot.getSigunguName()).isEqualTo("아산시");
        assertThat(snapshot.getSourceModifiedAt()).isEqualTo(LocalDateTime.of(2026, 9, 5, 12, 30));
        assertThat(snapshot.getLastSyncedAt()).isEqualTo(now);
        verify(detailService).getCommonDetail("126508");
        verify(tourismApiClient).fetchSigunguList();
    }

    @Test
    void skipsSynchronizationBeforeSevenThirty() {
        service.synchronizeReferenced(List.of("126508"), LocalDateTime.of(2026, 9, 6, 7, 29, 59));

        verifyNoInteractions(snapshotRepository, detailService, tourismApiClient);
    }

    @Test
    void keepsExistingSnapshotAndRetriesWhenEitherApiFails() {
        TourismContentSnapshot snapshot = snapshot(null);
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content());
        when(tourismApiClient.fetchSigunguList()).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 8, 0);

        service.synchronizeReferenced(List.of("126508"), now);
        service.synchronizeReferenced(List.of("126508"), now.plusMinutes(1));

        assertThat(snapshot.getTitle()).isEqualTo("현충사 기존");
        assertThat(snapshot.getLastSyncedAt()).isNull();
        verify(detailService, times(2)).getCommonDetail("126508");
        verify(tourismApiClient, times(2)).fetchSigunguList();
    }

    @Test
    void keepsNewerStoredDataWhenApiReturnsOlderSourceVersion() {
        TourismContentSnapshot snapshot = TourismContentSnapshot.builder()
                .contentId("126508")
                .contentTypeId("12")
                .title("현충사 저장 최신본")
                .sigunguCode("200")
                .sigunguName("아산시")
                .sourceModifiedAt(LocalDateTime.of(2026, 9, 6, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 9, 6, 12, 0))
                .lastSyncedAt(LocalDateTime.of(2026, 9, 5, 8, 0))
                .build();
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content());
        SigunguItem sigungu = new SigunguItem();
        sigungu.setCode("200");
        sigungu.setName("아산시");
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu));
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 13, 0);

        service.synchronizeReferenced(List.of("126508"), now);

        assertThat(snapshot.getTitle()).isEqualTo("현충사 저장 최신본");
        assertThat(snapshot.getLastSyncedAt()).isEqualTo(now);
    }

    @Test
    void callsLdongApiEvenWhenCommonResponseHasNoSigunguCode() {
        TourismContentSnapshot snapshot = snapshot(null);
        ContentDetailDto content = content();
        content.setLDongSignguCd(null);
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content);
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of());

        service.synchronizeReferenced(List.of("126508"), LocalDateTime.of(2026, 9, 6, 8, 0));

        assertThat(snapshot.getSigunguCode()).isNull();
        assertThat(snapshot.getSigunguName()).isNull();
        verify(tourismApiClient).fetchSigunguList();
    }

    @Test
    void rejectsIncompleteCommonResponseWithoutChangingSnapshot() {
        TourismContentSnapshot snapshot = snapshot(null);
        ContentDetailDto content = content();
        content.setTitle(null);
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content);
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu("200", "아산시")));

        service.synchronizeReferenced(List.of("126508"), LocalDateTime.of(2026, 9, 6, 8, 0));

        assertThat(snapshot.getTitle()).isEqualTo("현충사 기존");
        assertThat(snapshot.getLastSyncedAt()).isNull();
    }

    @Test
    void rejectsMissingSigunguMappingAndInvalidSourceDate() {
        TourismContentSnapshot snapshot = snapshot(null);
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content());
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of());
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 8, 0);

        service.synchronizeReferenced(List.of("126508"), now);

        ContentDetailDto invalidDate = content();
        invalidDate.setModifiedtime("invalid");
        when(detailService.getCommonDetail("126508")).thenReturn(invalidDate);
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu("200", "아산시")));
        service.synchronizeReferenced(List.of("126508"), now.plusMinutes(1));

        assertThat(snapshot.getTitle()).isEqualTo("현충사 기존");
        assertThat(snapshot.getLastSyncedAt()).isNull();
    }

    @Test
    void rejectsMismatchedContentIdBeforeCallingLdongApi() {
        TourismContentSnapshot snapshot = snapshot(null);
        ContentDetailDto content = content();
        content.setContentid("other");
        when(snapshotRepository.findByContentIdForUpdate("126508")).thenReturn(Optional.of(snapshot));
        when(detailService.getCommonDetail("126508")).thenReturn(content);

        service.synchronizeReferenced(List.of("126508"), LocalDateTime.of(2026, 9, 6, 8, 0));

        assertThat(snapshot.getTitle()).isEqualTo("현충사 기존");
        assertThat(snapshot.getLastSyncedAt()).isNull();
        verifyNoInteractions(tourismApiClient);
    }

    private TourismContentSnapshot snapshot(LocalDateTime lastSyncedAt) {
        return TourismContentSnapshot.builder()
                .contentId("126508")
                .contentTypeId("12")
                .title("현충사 기존")
                .sigunguCode("200")
                .sigunguName("아산시")
                .updatedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .lastSyncedAt(lastSyncedAt)
                .build();
    }

    private ContentDetailDto content() {
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("126508");
        content.setContenttypeid("12");
        content.setTitle("현충사 최신");
        content.setFirstimage("https://example.com/new.jpg");
        content.setAddr1("충청남도 아산시 염치읍 현충사길 126");
        content.setMapx("126.9891281");
        content.setMapy("36.8051452");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");
        content.setLclsSystm1("VE");
        content.setLclsSystm2("VE01");
        content.setLclsSystm3("VE010100");
        content.setModifiedtime("20260905123000");
        return content;
    }

    private SigunguItem sigungu(String code, String name) {
        SigunguItem item = new SigunguItem();
        item.setCode(code);
        item.setName(name);
        return item;
    }
}
