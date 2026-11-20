package com.library;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The app starts, on the test database. */
class LibraryManagementSystemApplicationTests extends TestSupport {

    @Test
    void contextLoads() throws Exception {
        assertThat(adminToken()).isNotBlank();
    }
}
