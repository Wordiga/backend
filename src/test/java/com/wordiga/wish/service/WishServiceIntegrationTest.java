package com.wordiga.wish.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.wish.repository.WishRepository;
import com.wordiga.support.PostgresIntegrationTest;
import com.wordiga.wish.Wish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

class WishServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private WishService wishService;

    @Autowired
    private WishRepository wishRepository;

    @MockitoBean
    private TourismApiClient tourismApiClient;

    @AfterEach
    void cleanUp() {
        wishRepository.deleteAll();
    }

    @Test
    void savesFoldersQueriesAndDeletesWishInPostgres() {
        when(tourismApiClient.fetchCommonDetail("126508"))
                .thenReturn(content("126508", "현충사", "200"));
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu("200", "아산시")));

        wishService.addWish(1L, request("126508"));

        List<Wish> saved = wishRepository.findAll();
        assertThat(saved).singleElement().satisfies(wish -> {
            assertThat(wish.getFolderName()).isEqualTo("아산시");
            assertThat(wish.getCreatedAt()).isNotNull();
        });
        assertThat(wishService.getWishFolders(1L)).singleElement()
                .satisfies(folder -> assertThat(folder.getCount()).isEqualTo(1));
        assertThat(wishService.getWishesByFolder(1L, "아산시")).hasSize(1);

        wishService.removeWish(1L, "126508");

        assertThat(wishRepository.count()).isZero();
    }

    @Test
    void concurrentDifferentWishesCompleteWithoutDeadlock() {
        when(tourismApiClient.fetchCommonDetail("A"))
                .thenReturn(content("A", "현충사", "200"));
        when(tourismApiClient.fetchCommonDetail("B"))
                .thenReturn(content("B", "공산성", "150"));
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(
                sigungu("200", "아산시"), sigungu("150", "공주시")));

        assertThatCode(() -> {
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> wishService.addWish(1L, request("A")));
                var second = executor.submit(() -> wishService.addWish(1L, request("B")));
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            }
        }).doesNotThrowAnyException();
        assertThat(wishRepository.count()).isEqualTo(2);
    }

    private WishRequest request(String contentId) {
        WishRequest request = new WishRequest();
        request.setContentId(contentId);
        return request;
    }

    private ContentDetailDto content(String contentId, String title, String signguCode) {
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid(contentId);
        content.setContenttypeid("12");
        content.setTitle(title);
        content.setAddr1("충청남도");
        content.setMapx("126.9891281");
        content.setMapy("36.8051452");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd(signguCode);
        return content;
    }

    private SigunguItem sigungu(String code, String name) {
        SigunguItem item = new SigunguItem();
        item.setCode(code);
        item.setName(name);
        return item;
    }
}
