package com.kexin.mall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class MallThirdPartyApplicationTests {
    @Autowired
    OSSClient ossClient;

    @Test
    void contextLoads() {
    }

    // 使用SpringCloud Alibaba，配置好了yaml,bean注入
    @Test
    public void testUpload2() throws FileNotFoundException {

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\kexin\\Pictures\\4d349f57947df6590a2dd1364c3b0b1e.jpg");
        // 上传
        ossClient.putObject("kexin-mall", "test-2.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功.");
    }

}
