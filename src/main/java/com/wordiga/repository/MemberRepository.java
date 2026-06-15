package com.wordiga.repository;

import com.wordiga.domain.Member;
import com.wordiga.domain.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}