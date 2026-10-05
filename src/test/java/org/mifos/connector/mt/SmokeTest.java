package org.mifos.connector.mt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "zeebe.enabled=false",
        "mt.gateway.base-url=http://localhost:8090"
})
class SmokeTest {
    @Test
    void contextLoads() {}
}
