package com.kexin.mall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
 * 1、整合Mybatis-Plus
 *  1）导入依赖（mybatis-plus-boot-starter）
 *  2）配置
 *      1、配置数据源（连什么数据库）
 *          1、导入数据库驱动
 *          2、在application.yml中配置数据源相关信息
 *      2、配置Mybatis-Plus相关信息
 *          1、使用@MapperScan扫描包
 *          2、告诉MyBatis-Plus，sql映射文件（mapper的xml）的位置
 *
 * 2、逻辑删除
 */
@EnableFeignClients(basePackages = "com.kexin.mall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.kexin.mall.product.dao")
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }
}
