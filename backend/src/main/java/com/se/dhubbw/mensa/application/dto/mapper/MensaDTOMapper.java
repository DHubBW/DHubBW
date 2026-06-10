package com.se.dhubbw.mensa.application.dto.mapper;

import com.se.dhubbw.mensa.application.dto.output.MensaDTO;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import org.springframework.stereotype.Component;

/**
 * Mapper component for transforming {@link Mensa} entities to
 * {@link MensaDTO} objects.
 * Implements the representation transformation between persistence
 * and API layer.
 *
 * @architecture.component Mensaverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → DTO)
 */
@Component
public class MensaDTOMapper {
  /**
   * Create a DTO from a {@link Mensa} entity.
   *
   * @param mensa the entity to convert
   * @return the corresponding {@link MensaDTO}
   */
  public MensaDTO convertMensaEntityToMensaDTO(final Mensa mensa) {
    String zusatz = mensa.getZusatz();
    return new MensaDTO(
        mensa.getGerichtID(),
        mensa.getDatum(),
        mensa.getEssensbezeichnung(),
        mensa.getAllergene(),
        mensa.getPreis(),
        mensa.getErnaehrung(),
        mensa.getLinie(),
        zusatz != null ? zusatz : ""
    );
  }
}
