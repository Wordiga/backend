package com.wordiga.wish.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.support.PostgresIntegrationTest;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.wish.repository.WishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private TourismApiClient tourismApiClient;

    private Long testMemberId;

    @BeforeEach
    void setUpFixture() {
        Member member = Member.create(
                "tester@wordiga.com",
                "테스터",
                OAuthProvider.KAKAO,
                "kakao_123456",
                null
        );
        testMemberId = memberRepository.save(member).getId();
    }

    @AfterEach
    void cleanUp() {
        wishRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void concurrentDuplicateWishKeepsUniqueConstraintWithoutDeadlock() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        SigunguItem sigungu = new SigunguItem();
        sigungu.setCode("200");
        sigungu.setName("천안시");
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu));
        when(tourismApiClient.fetchCommonDetail("same")).thenAnswer(ignored -> {
            barrier.await();
            return content();
        });

        List<Throwable> failures;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> wishService.addWish(testMemberId, request()));
            var second = executor.submit(() -> wishService.addWish(testMemberId, request()));
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
        content.setTitle("천안 삼거리");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");
        return content;
    }
}