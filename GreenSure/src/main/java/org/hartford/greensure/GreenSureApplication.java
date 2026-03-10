package org.hartford.greensure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GreenSureApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenSureApplication.class, args);
    }

}
