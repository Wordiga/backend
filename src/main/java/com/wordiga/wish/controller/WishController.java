package com.wordiga.wish.controller;

import com.wordiga.wish.api.WishApi;
import com.wordiga.wish.dto.WishDeleteRequest;
import com.wordiga.wish.dto.WishFolderResponse;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.wish.dto.WishResponse;
import com.wordiga.global.security.CurrentMemberId;
import com.wordiga.wish.service.WishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishes")
@RequiredArgsConstructor
public class WishController implements WishApi {

    private final WishService wishService;

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody WishRequest request) {

        WishResponse response = wishService.addWish(memberId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeWish(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody WishDeleteRequest request) {

        wishService.removeWish(memberId, request.getContentId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WishFolderResponse>> getWishFolders(
            @CurrentMemberId Long memberId) {

        List<WishFolderResponse> folders = wishService.getWishFolders(memberId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/{folderName}")
    public ResponseEntity<List<WishResponse>> getWishesByFolder(
            @CurrentMemberId Long memberId,
            @PathVariable String folderName) {

        List<WishResponse> wishes = wishService.getWishesByFolder(memberId, folderName);
        return ResponseEntity.ok(wishes);
    }
}
