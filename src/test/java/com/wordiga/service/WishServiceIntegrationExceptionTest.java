package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.repository.WishRepository;
import com.wordiga.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class WishServiceIntegrationExceptionTest extends PostgresIntegrationTest {

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
    void concurrentDuplicateWishKeepsUniqueConstraintWithoutDeadlock() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        when(tourismApiClient.fetchCommonDetail("same")).thenAnswer(ignored -> {
            barrier.await();
            return content();
        });

        List<Throwable> failures;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> wishService.addWish(1L, request()));
            var second = executor.submit(() -> wishService.addWish(1L, request()));
            failures = List.of(first, second).stream()
                    .map(future -> {
                        try {
                            future.get(10, TimeUnit.SECONDS);
                            return (Throwable) null;
                        } catch (ExecutionException exception) {
                            return exception.getCause();
                        } catch (Exception exception) {
                            return exception;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        assertThat(wishRepository.count()).isEqualTo(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private WishRequest request() {
        WishRequest request = new WishRequest();
        request.setContentId("same");
        return request;
    }

    private ContentDetailDto content() {
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("same");
        content.setContenttypeid("12");
        content.setTitle("현충사");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");
        return content;
    }
}
