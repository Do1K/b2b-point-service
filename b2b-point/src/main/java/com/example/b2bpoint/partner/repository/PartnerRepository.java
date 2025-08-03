package com.example.b2bpoint.partner.repository;

import com.example.b2bpoint.partner.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {


    Optional<Partner> findByBusinessNumber(String businessNumber);

    Optional<Partner> findByApiKey(String apiKey);
}
