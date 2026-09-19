package com.retail.rec.repository;

import com.retail.rec.model.Recommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RecRepo extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdOrderByScoreDesc(UUID userId, Pageable pageable);

    @Transactional
    void deleteByUserId(UUID userId);
}
