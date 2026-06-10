package com.se.dhubbw.mensa.application.dto.mapper;

import com.se.dhubbw.mensa.application.dto.output.MensaDTO;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Unit tests for {@link MensaDTOMapper}.
 *
 * <p>Verifies the entity → DTO transformation, including the
 * {@code zusatz} field handling: when the source entity has a
 * {@code null} value the mapper must coerce it to an empty string so
 * the DTO contract (non-null {@code String}) holds.</p>
 */
@UnitTest
public class MensaDTOMapperTest {

    /**
     * Mapper under test.
     */
    private MensaDTOMapper mapper;

    /**
     * Build a fresh mapper for every test.
     */
    @BeforeEach
    public void setUp() {
        mapper = new MensaDTOMapper();
    }

    /**
     * Happy path: every persisted field is forwarded to the DTO.
     */
    @Test
    public void givenFullyPopulatedEntity_whenMapped_thenAllFieldsCopied() {
        Mensa entity = new Mensa();
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        entity.setGerichtID(id);
        entity.setDatum(LocalDate.of(2026, 6, 10));
        entity.setEssensbezeichnung("Portion Stangenspargel");
        entity.setAllergene("[Ei,ML,Se,Sf,We]");
        entity.setPreis(5.90);
        entity.setErnaehrung(Ernaehrung.VEGETARISCH);
        entity.setLinie("Wahlessen 1");
        entity.setZusatz(
            "mit Pfannkuchen oder Dampfkartoffeln und "
                + "Soße nach holländischer Art oder Butter");

        MensaDTO dto = mapper.convertMensaEntityToMensaDTO(entity);

        Assertions.assertEquals(id, dto.gerichtID());
        Assertions.assertEquals(LocalDate.of(2026, 6, 10), dto.datum());
        Assertions.assertEquals(
            "Portion Stangenspargel", dto.essensbezeichnung());
        Assertions.assertEquals("[Ei,ML,Se,Sf,We]", dto.allergene());
        Assertions.assertEquals(5.90, dto.preis(), 0.001);
        Assertions.assertEquals(Ernaehrung.VEGETARISCH, dto.ernaehrung());
        Assertions.assertEquals("Wahlessen 1", dto.linie());
        Assertions.assertEquals(
            "mit Pfannkuchen oder Dampfkartoffeln und "
                + "Soße nach holländischer Art oder Butter",
            dto.zusatz());
    }

    /**
     * Defensive: a {@code null} {@code zusatz} on the entity must be
     * coerced to an empty string in the DTO, so the JSON contract
     * stays a non-null {@code String}.
     */
    @Test
    public void givenEntityWithNullZusatz_whenMapped_thenZusatzIsEmptyString() {
        Mensa entity = new Mensa();
        entity.setGerichtID(UUID.randomUUID());
        entity.setDatum(LocalDate.of(2026, 6, 10));
        entity.setEssensbezeichnung("Reissalat");
        entity.setAllergene("");
        entity.setPreis(2.90);
        entity.setErnaehrung(Ernaehrung.VEGAN);
        entity.setLinie("Wahlessen 1");
        // Deliberately leave zusatz unset (null).

        MensaDTO dto = mapper.convertMensaEntityToMensaDTO(entity);

        Assertions.assertNotNull(dto.zusatz());
        Assertions.assertEquals("", dto.zusatz());
    }
}
