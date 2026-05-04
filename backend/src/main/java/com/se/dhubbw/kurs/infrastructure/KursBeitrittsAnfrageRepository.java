package com.se.dhubbw.kurs.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KursBeitrittsAnfrageRepository extends
    JpaRepository<KursBeitrittsAnfrage, UUID> {
}
