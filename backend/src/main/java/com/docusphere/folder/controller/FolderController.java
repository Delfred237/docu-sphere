package com.docusphere.folder.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.folder.dto.CreateFolderRequest;
import com.docusphere.folder.dto.FolderResponse;
import com.docusphere.folder.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFolderRequest request) {
        FolderResponse response = folderService.createFolder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getRootFolders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderService.getRootFolders(user));
    }

    @GetMapping("/{publicId}/children")
    public ResponseEntity<List<FolderResponse>> getSubFolders(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {
        return ResponseEntity.ok(folderService.getSubFolders(user, publicId));
    }
}
