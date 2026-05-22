package com.se.dhubbw.mensa.application.dto.mapper;

import com.se.dhubbw.mensa.application.dto.MensaDTO;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import org.springframework.stereotype.Component;

@Component
public class MensaDTOMapper {
  /**
   * Create a DTO from a {@link Mensa} entity.
   *
   * @param mensa the entity to convert
   * @return the corresponding {@link MensaDTO}
   */
  public MensaDTO convertMensaEntityToMensaDTO(final Mensa mensa) {
    return new MensaDTO(
        mensa.getGerichtID(),
        mensa.getDatum(),
        mensa.getEssensbezeichnung(),
        mensa.getAllergene(),
        mensa.getPreis(),
        mensa.getErnaehrung(),
        mensa.getLinie()
    );
  }
}
