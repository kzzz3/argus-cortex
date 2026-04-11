package com.kzzz3.argus.cortex;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kzzz3.argus.cortex.**.mapper")
public class ArgusCortexApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArgusCortexApplication.class, args);
	}

}
