package org.hartford.greensure;

import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class GreenSureApplicationTests {

    @MockBean
    private Gmail gmail;

    @Test
    void contextLoads() {
    }

}
