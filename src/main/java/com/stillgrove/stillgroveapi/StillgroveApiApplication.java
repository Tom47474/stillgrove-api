package com.stillgrove.stillgroveapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StillgroveApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StillgroveApiApplication.class, args);
	}

}
