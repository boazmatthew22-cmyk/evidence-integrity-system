package com.hackathon.evidence.controller;

import com.hackathon.evidence.model.Evidence;
import com.hackathon.evidence.repository.EvidenceRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceRepository evidenceRepository;

    public EvidenceController(EvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Evidence uploadEvidence(
            @RequestParam String caseId,
            @RequestParam String uploadedBy,
            @RequestParam MultipartFile file
    ) throws IOException, NoSuchAlgorithmException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file.");
        }

        String hash = calculateSha256(file.getBytes());

        Path uploadFolder = Paths.get("uploads");
        Files.createDirectories(uploadFolder);

        String safeFileName = UUID.randomUUID() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");

        Path savedFilePath = uploadFolder.resolve(safeFileName);
        Files.copy(file.getInputStream(), savedFilePath);

        Evidence evidence = new Evidence();
        evidence.setCaseId(caseId);
        evidence.setOriginalFileName(file.getOriginalFilename());
        evidence.setFileType(file.getContentType());
        evidence.setFileSize(file.getSize());
        evidence.setSha256Hash(hash);
        evidence.setStoragePath(savedFilePath.toString());
        evidence.setUploadedBy(uploadedBy);
        evidence.setStatus("HASHED_AND_STORED");
        evidence.setUploadedAt(LocalDateTime.now());

        return evidenceRepository.save(evidence);
    }

    @GetMapping
    public List<Evidence> getAllEvidence() {
        return evidenceRepository.findAll();
    }

    @PostMapping(value = "/{id}/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> verifyEvidence(
            @PathVariable Long id,
            @RequestParam MultipartFile file
    ) throws IOException, NoSuchAlgorithmException {

        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evidence record not found."));

        String newHash = calculateSha256(file.getBytes());

        boolean verified = newHash.equals(evidence.getSha256Hash());

        if (verified) {
            evidence.setStatus("VERIFIED");
        } else {
            evidence.setStatus("TAMPERING_DETECTED");
        }

        evidenceRepository.save(evidence);

        Map<String, Object> result = new HashMap<>();
        result.put("evidenceId", evidence.getId());
        result.put("originalHash", evidence.getSha256Hash());
        result.put("currentHash", newHash);
        result.put("verified", verified);

        return result;
    }

    private String calculateSha256(byte[] fileBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);

        StringBuilder hash = new StringBuilder();
        for (byte hashByte : hashBytes) {
            hash.append(String.format("%02x", hashByte));
        }

        return hash.toString();
    }
}