package com.hackathon.evidence.repository;

import com.hackathon.evidence.model.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
}
