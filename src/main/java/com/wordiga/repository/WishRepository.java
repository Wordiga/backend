package com.wordiga.repository;

import com.wordiga.domain.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByMemberIdAndContentId(Long memberId, String contentId);

    boolean existsByMemberIdAndContentId(Long memberId, String contentId);

    List<Wish> findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(Long memberId, String folderName);

    List<Wish> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    void deleteByMemberIdAndContentId(Long memberId, String contentId);
}
