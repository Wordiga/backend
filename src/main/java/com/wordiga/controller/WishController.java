package com.wordiga.controller;

import com.wordiga.api.WishApi;
import com.wordiga.dto.wish.WishDeleteRequest;
import com.wordiga.dto.wish.WishFolderResponse;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.dto.wish.WishResponse;
import com.wordiga.service.WishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishes")
@RequiredArgsConstructor
public class WishController implements WishApi {

    private final WishService wishService;

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            Authentication authentication,
            @Valid @RequestBody WishRequest request) {

        Long memberId = Long.valueOf(authentication.getName());
        WishResponse response = wishService.addWish(memberId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeWish(
            Authentication authentication,
            @Valid @RequestBody WishDeleteRequest request) {

        Long memberId = Long.valueOf(authentication.getName());
        wishService.removeWish(memberId, request.getContentId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/folders")
    public ResponseEntity<List<WishFolderResponse>> getWishFolders(
            Authentication authentication) {

        Long memberId = Long.valueOf(authentication.getName());
        List<WishFolderResponse> folders = wishService.getWishFolders(memberId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/folders/{folderName}")
    public ResponseEntity<List<WishResponse>> getWishesByFolder(
            Authentication authentication,
            @PathVariable String folderName) {

        Long memberId = Long.valueOf(authentication.getName());
        List<WishResponse> wishes = wishService.getWishesByFolder(memberId, folderName);
        return ResponseEntity.ok(wishes);
    }
}