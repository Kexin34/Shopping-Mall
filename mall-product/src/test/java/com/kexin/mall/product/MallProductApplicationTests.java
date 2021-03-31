package com.kexin.mall.product;

import com.kexin.mall.product.entity.BrandEntity;
import com.kexin.mall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

//@RunWith(SpringRunner.class)
@SpringBootTest
class MallProductApplicationTests {
    @Autowired
    BrandService brandService;

//    @Autowired
//    OSSClient ossClient;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(6L);
        brandEntity.setDescript("修1改");
        brandService.updateById(brandEntity);
    }

    // 原声例子
//    @Test
//    public void testUpload1() throws FileNotFoundException {
//        // Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-us-east-1.aliyuncs.com";
//        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "LTAI5tGhayiaY4GdRAjJtBfG";
//        String accessKeySecret = "SJ4jfvxVNSZ6ryXDWHE21IWalMWIde";
//
//        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        // 上传文件流。
//        InputStream inputStream = new FileInputStream("C:\\Users\\kexin\\Pictures\\p2400854170.jpg");
//        // 上传
//        ossClient.putObject("kexin-mall", "RR.jpg", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
//        System.out.println("上传成功.");
//    }

    // 使用SpringCloud Alibaba，配置好了yaml,bean注入
//    @Test
//    public void testUpload2() throws FileNotFoundException {
//
//        // 上传文件流。
//        InputStream inputStream = new FileInputStream("C:\\Users\\kexin\\Pictures\\4d349f57947df6590a2dd1364c3b0b1e.jpg");
//        // 上传
//        ossClient.putObject("kexin-mall", "test.jpg", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
//        System.out.println("上传成功.");
//    }

}
