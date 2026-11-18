package com.rural.travel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.rural.travel.mapper", "com.rural.travel.guide.mapper"})
public class RuralTravelApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuralTravelApplication.class, args);
    }
}
