package com.se.dhubbw.shared.application.dto.util;

/**
 * Enum for several persistence operations
 * performed as response to http requests.
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
