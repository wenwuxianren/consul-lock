package com.lyb.consullock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConsulLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsulLockApplication.class, args);
    }

}
