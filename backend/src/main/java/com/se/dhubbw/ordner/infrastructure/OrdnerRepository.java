package com.se.dhubbw.ordner.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrdnerRepository extends JpaRepository<Ordner, UUID> { }
