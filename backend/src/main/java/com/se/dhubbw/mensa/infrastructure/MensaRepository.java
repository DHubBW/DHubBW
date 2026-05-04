package com.se.dhubbw.mensa.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MensaRepository extends JpaRepository<Mensa, UUID> {
}
