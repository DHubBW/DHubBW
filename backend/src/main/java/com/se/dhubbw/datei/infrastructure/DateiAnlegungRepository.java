package com.se.dhubbw.datei.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DateiAnlegungRepository extends
    JpaRepository<DateiAnlegung, UUID> {
}
