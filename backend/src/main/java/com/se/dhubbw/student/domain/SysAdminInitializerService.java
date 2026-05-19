package com.se.dhubbw.student.domain;

/**
 * Service interface for system administrator initialization.
 *
 * <p>Defines the operation required to initialize a system administrator
 * account on application startup.</p>
 */
public interface SysAdminInitializerService {

    /**
     * Initialize the system administrator account.
     *
     * <p>Creates a new SYSTEMADMIN user with a randomly generated password
     * if no system administrator exists yet. Logs the credentials for
     * initial access.</p>
     *
     * <p>This method is idempotent - calling it multiple times will not
     * create duplicate accounts.</p>
     *
     * @param args command line arguments passed to the application
     */
    void run(String... args);
}
