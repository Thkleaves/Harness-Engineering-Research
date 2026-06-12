package com.kleaves.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 *
 * @SpringBootApplication 是一个组合注解，包含：
 *   1. @Configuration     — 标记此类为配置类
 *   2. @EnableAutoConfiguration — 自动配置（扫描 classpath 中的 jar 包，
 *      发现有 spring-web 就配 Tomcat，有 JPA 就配数据源...）
 *   3. @ComponentScan     — 扫描当前包及其子包，
 *      把所有 @Component / @Service / @RestController 自动注册为 Bean
 *
 * 启动方式：
 *   IDEA 中右键 → Run 'DemoApplication'
 *   或命令行: mvn spring-boot:run
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
