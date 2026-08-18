package com.wordiga.global.exception;

import com.wordiga.global.exception.ApiExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerUnitTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void formatsBindingAndUnreadableErrors() {
        BindException binding = new BindException(new com.wordiga.plan.dto.PlanUpdateRequest(), "request");
        binding.rejectValue("title", "invalid", "잘못된 값");
        assertThat(handler.handleValidation(binding).getBody().fieldErrors()).hasSize(1);
        assertThat(handler.handleUnreadable(mock(HttpMessageNotReadableException.class)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void formatsConstraintViolations() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("size");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("50 이하여야 합니다");

        var response = handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getBody().fieldErrors().getFirst().field()).isEqualTo("size");
    }

    @Test
    void mapsDomainAndUpstreamStatusCodes() {
        assertThat(code(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")).isEqualTo("PLAN_NOT_FOUND");
        assertThat(code(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다.")).isEqualTo("CONTENT_NOT_FOUND");
        assertThat(code(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 API를 사용할 수 없습니다.")).isEqualTo("TOURISM_API_UNAVAILABLE");
        assertThat(code(HttpStatus.SERVICE_UNAVAILABLE, "제안서 저장소 오류")).isEqualTo("PROPOSAL_STORAGE_UNAVAILABLE");
        assertThat(code(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 오류")).isEqualTo("AI_SERVER_UNAVAILABLE");
        assertThat(code(HttpStatus.SERVICE_UNAVAILABLE, null)).isEqualTo("AI_SERVER_UNAVAILABLE");
        assertThat(code(HttpStatus.NOT_FOUND, null)).isEqualTo("CONTENT_NOT_FOUND");
        assertThat(code(HttpStatus.BAD_GATEWAY, "AI 응답 오류")).isEqualTo("AI_RESPONSE_INVALID");
        assertThat(code(HttpStatus.BAD_REQUEST, "요청 오류")).isEqualTo("INVALID_REQUEST");
    }

    private String code(HttpStatus status, String reason) {
        return handler.handleStatus(new ResponseStatusException(status, reason)).getBody().code();
    }
}
