package com.se.dhubbw.shared.application.dto.util;

/**
 * Enumeration of persistence operations (INSERT, UPDATE, DELETE).
 * Used in API responses to indicate the type of database operation performed.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Application Layer
 * @architecture.category A-Software (domain value object)
 */
public enum PersistenceOperation {
  /**
   * Chosen if the main db operation after handling request is INSERT.
   */
  INSERT,
  /**
   * Chosen if the main db operation after handling request is UPDATE.
   */
  UPDATE,
  /**
   * Chosen if the main db operation after handling request is DELETE.
   */
  DELETE
}
