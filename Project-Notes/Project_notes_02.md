

# 17、品牌管理菜单

后台：系统管理/菜单管理/新增

![image-20200428164054517](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/image-20200428164054517.png)





（2）将逆向工程product得到的resources\src\views\modules\product文件拷贝到gulimall/renren-fast-vue/src/views/modules/product目录下，也就是下面的两个文件

brand.vue ： 显示的表单
brand-add-or-update.vue：添加和更改功能

但是显示的页面没有新增和删除功能，这是因为权限控制的原因

```
<el-button v-if="isAuth('product:brand:save')" type="primary" @click="addOrUpdateHandle()">新增</el-button>
<el-button v-if="isAuth('product:brand:delete')" type="danger" @click="deleteHandle()" :disabled="dataListSelections.length <= 0">批量删除</el-button>

```

查看“isAuth”的定义位置：它是在“index.js”中定义，暂时将它设置为返回值为true，即可显示添加和删除功能。

进行添加 测试成功， 进行修改 也会自动回显

> build/webpack.base.conf.js 中注释掉createLintingRule()函数体，不进行lint语法检查
>

#### 效果优化和快速“显示状态”开关

“显示状态”按钮

brand.vue

```vue
<template slot-scope="scope"> scope属性包含了一整行数据
  定义显示效果
  <el-switch
    v-model="scope.row.showStatus"
    active-color="#13ce66"
    inactive-color="#ff4949"
    @change="updateBrandStatus(scope.row)" 变化会调用函数
    :active-value = "1"
    :inactive-value	= "0"
  ></el-switch>
</template>

另外导入了
<script>
import AddOrUpdate from "./brand-add-or-update";
他作为弹窗被brand.vue使用
<!-- 弹窗, 新增 / 修改 -->
<add-or-update v-if="addOrUpdateVisible" ref="addOrUpdate" @refreshDataList="getDataList"></add-or-update>
    
AddOrUpdate具体是个会话窗
<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :visible.sync="visible"
  >

```

brand-add-or-update.vue

```
<el-form-item label="显示状态" prop="showStatus">
    <el-switch v-model="dataForm.showStatus"
               active-color="#13ce66"
               inactive-color="#ff4949"
               :active-value="1"
               :inactive-value="0"
               >
    </el-switch>
</el-form-item>

```

```
//更新开关的状态
    updateBrandStatus(data) { // 传入了改变行的数据
      console.log("最新状态", data);
      let {brandId,showStatus} = data;
      this.$http({
        url: this.$http.adornUrl("/product/brand/update"),
        method: "post",
        data: this.$http.adornData({brandId,showStatus}, false)
      }).then(({ data }) => {

        this.$message({
          message: "状态更新成功",
          type: "success"
        });

      });
    },

```

更新品牌对应的controller

```java
@RestController
@RequestMapping("product/brand")
public class BrandController {
    /** * 修改 */
    @RequestMapping("/update")
    public R update(@RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }


```

品牌实体

```java
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/* 品牌id */
	@TableId
	private Long brandId;
	/*** 品牌名 */
	private String name;
	/*** 品牌logo地址 */
	private String logo;
	/*** 介绍 */
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	private Integer showStatus;
	/** * 检索首字母 */
	private String firstLetter;
	/** * 排序 */
	private Integer sort;
}

```

测试：

![image-20210330231555596](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330231555596.png)

没有图片，接下来要找一个地方存放上传的图片



# 18、云储存开通与使用

![image-20210331100509472](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331100509472.png)

和传统的单体应用不同，这里我们选择将数据上传到分布式文件服务器上。

这里我们选择将图片放置到阿里云上，使用对象存储。



![image-20210331102211409](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331102211409.png)

我们可以在程序中设置自动上传图片到阿里云对象存储。

上传模型：

![image-20210331102513101](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331102513101.png)

![image-20210331102554445](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331102554445.png)

- 上传的账号信息存储在应用服务器
- 上传先找应用服务器要一个policy上传策略，生成防伪签名

### OSS 整合测试

使用代码上传
查看阿里云关于文件上传的帮助： https://help.aliyun.com/document_detail/32009.html?spm=a2c4g.11186623.6.768.549d59aaWuZMGJ

1.1）添加依赖包
在Maven项目中加入依赖项（推荐方式）

在 Maven 工程中使用 OSS Java SDK，只需在 pom.xml 中加入相应依赖即可。以 3.8.0 版本为例，在 <dependencies 内加入如下内容：

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.5.0</version>
</dependency>
```

1.2）上传文件流

以下Sample代码用于上传文件流：

```java
// Endpoint以杭州为例，其它Region请按实际情况填写。
String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
String accessKeyId = "<yourAccessKeyId>";
String accessKeySecret = "<yourAccessKeySecret>";

// 创建OSSClient实例。
OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

// 上传文件流。
InputStream inputStream = new FileInputStream("<yourlocalFile>");
ossClient.putObject("<yourBucketName>", "<yourObjectName>", inputStream);

// 关闭OSSClient。
ossClient.shutdown();

```

上面代码的信息可以通过如下查找：

endpoint的取值：点击概览就可以看到你的endpoint信息，endpoint在这里就是上海等地区，如 oss-cn-qingdao.aliyuncs.com
bucket域名：就是签名加上bucket

accessKey的获取
accessKeyId和accessKeySecret需要创建一个RAM账号：

![image-20210331103051227](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331103051227.png)

- 选上`编程访问`

创建用户完毕后，会得到一个“AccessKey ID”和“AccessKeySecret”，然后复制这两个值到代码的“AccessKey ID”和“AccessKeySecret”。

另外还需要添加访问控制权限：

![image-20210331103536754](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331103536754.png)

```
endpoint = "oss-us-east-1.aliyuncs.com";
```

![image-20210331103750270](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331103750270.png)

原生上传例子

```java
    @Test
    public void testUpload() throws FileNotFoundException {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-us-east-1.aliyuncs.com";
        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
        String accessKeyId = "";
        String accessKeySecret = "";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\kexin\\Pictures\\p2400854170.jpg");
        // 上传
        ossClient.putObject("kexin-mall", "RR.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功.");
    }
```

2）更为简单的使用方式，是使用SpringCloud Alibaba来管理oss

![image-20210331104258788](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331104258788.png)

详细使用方法，见： https://help.aliyun.com/knowledge_detail/108650.html

（1）添加依赖到common pom

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alicloud-oss</artifactId>
    <version>2.2.0.RELEASE</version>
</dependency>
```

（2）创建“AccessKey ID”和“AccessKeySecret”

（3）配置key，secret和endpoint相关信息

```properties
spring:
  datasource:...
  cloud:
    nacos:.....
    alicloud:
      access-key: 。。
      secret-key: 。。
      oss:
        endpoint: oss-us-east-1.aliyuncs.com
```

（4）注入OSSClient并进行文件上传下载等操作

![image-20210331105035603](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331105035603.png)



```java
// 使用SpringCloud Alibaba，配置好了yaml,bean注入
    @Test
    public void testUpload2() throws FileNotFoundException {

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\kexin\\Pictures\\4d349f57947df6590a2dd1364c3b0b1e.jpg");
        // 上传
        ossClient.putObject("kexin-mall", "test.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功.");
    }
```

但是这样来做还是比较麻烦，如果以后的上传任务都交给mall-product来完成，显然耦合度高。最好单独新建一个Module来完成文件上传任务。

### mall-third-party微服务

添加依赖，将原来mall-common中的“spring-cloud-starter-alicloud-oss”依赖移动到该项目中

```xml
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alicloud-oss</artifactId>
            <version>2.2.0.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>com.kexin.mall</groupId>
            <artifactId>mall-common</artifactId>
            <version>1.0-SNAPSHOT</version>
            <exclusions>
                <exclusion>
                    <groupId>com.baomidou</groupId>
                    <artifactId>mybatis-plus-boot-starter</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

```

另外也需要在“pom.xml”文件中，添加如下的依赖管理

```xml
<dependencyManagement>

        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>2.2.0.RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

```

主启动类@EnableDiscoveryClient开启服务的注册和发现



在nacos中注册

（1）在nacos创建命名空间“ mall-third-party ”

（2）在“ mall-third-party”命名空间中，创建“ mall-third-party.yml”文件

```properties
spring:
  cloud:
    alicloud:
      access-key: ..
      secret-key: ..
      oss:
        endpoint: ..

```

编写配置文件application.yml

```properties
spring:
  application: 
    name: mall-third-party
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
server:
  port: 30000
  
logging:
  level:
    com.kexin.mall.product: debug

```

注意去网关里配置转发，/api/thirdparty/…的路径改完后只有/…，但是其他服务是不去服务名的

bootstrap.properties

```
spring.cloud.nacos.config.name=mall-third-party
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.namespace=9054e55c-b667-428c-b71d-0f2b42a6acff
spring.cloud.nacos.config.extension-configs[0].data-id=oss.yml
spring.cloud.nacos.config.extension-configs[0].group=DEFAULT_GROUP
spring.cloud.nacos.config.extension-configs[0].refresh=true
```

nacos端新建oss.yml

```
spring:
    cloud:
        alicloud:
            access-key: ..
            secret-key: ..
            oss: 
                endpoint: ..
```

编写测试类

```java
@SpringBootTest
class MallThirdPartyApplicationTests {
    @Autowired
    OSSClient ossClient;

    @Test
    public void testUpload() throws FileNotFoundException {
        
        String endpoint = "";
        String accessKeyId = "";
        String accessKeySecret = "";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

         //上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\Kexin\\Downloads\\123.jpg");
        ossClient.putObject("mall-kexin", "333.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功.");
    }
}

```

```java
    @Test
    public void testUpload() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("C:\\Users\\Kexin\\Downloads\\123.jpg");
        // 参数1位bucket  参数2位最终名字
        ossClient.putObject("mall-kexin","321.jpg",inputStream);
        ossClient.shutdown();
    }

```

上面的逻辑中，我们的想法是先把字节流给服务器，服务器给阿里云，还是传到了服务器。我们需要一些前端代码完成这个功能，字节流就别来服务器了



### 改进：**OSS**获取服务端签名(服务端签名后直传)

背景

采用JavaScript客户端直接签名（参见JavaScript客户端签名直传）时，AccessKeyID和AcessKeySecret会暴露在前端页面，因此存在严重的安全隐患。因此，ali-OSS提供了服务端签名后直传的方案。

![image-20210331114014299](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331114014299.png)



服务端签名后直传的原理如下：

1. 用户发送上传Policy请求到应用服务器。
2. 应用服务器返回上传Policy和签名给用户。
3. 用户直接上传数据到OSS。

![image-20210331114028674](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331114028674.png)

编写“com.kexin.mall.thirdparty.controller.`OssController`”类：

```java
@RestController
public class OssController {

    @Autowired
    OSS ossClient;

    @Value ("${spring.cloud.alicloud.oss.endpoint}")
    String endpoint ;
    @Value("${spring.cloud.alicloud.oss.bucket}")
    String bucket ;
    @Value("${spring.cloud.alicloud.access-key}")
    String accessId ;
    @Value("${spring.cloud.alicloud.secret-key}")
    String accessKey ;

    @RequestMapping("/oss/policy")
    public Map<String, String> policy(){
        String host = "https://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint

        String format = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String dir = format; // 用户上传文件时指定的前缀。

        Map<String, String> respMap=null;
        try {
            // 签名有效事件
            long expireTime = 30;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);

            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            // 签名
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            respMap= new LinkedHashMap<String, String>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));

        } catch (Exception e) {
            // Assert.fail(e.getMessage());
            System.out.println(e.getMessage());
        } finally {
            ossClient.shutdown();
        }
        return respMap;
    }
}

```

上面的意思是说用户通过url请求得到一个policy，要拿这个东西直接传到阿里云，不要去服务器了

测试： http://localhost:30000/oss/policy 返回签名

![image-20210331115916204](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331115916204.png)

在该微服务中测试通过，但是我们不能对外暴露端口或者说为了统一管理，我们还是让用户请求网关然后转发过来

以后在上传文件时的访问路径为“ http://localhost:88/api/thirdparty/oss/policy”，通过网关转发

调整网关端口，以后我们访问88端口的88/api/thirdparty/oss/policy来获取上面信息

```properties
        - id: third_party_route
          uri: lb://mall-third-party
          predicates:
            - Path=/api/thirdparty/**
          filters:
            - RewritePath=/api/thirdparty/(?<segment>.*),/$\{segment}
```

![image-20210331120514104](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331120514104.png)



### OSS前后联调测试上传

放置项目提供的upload文件夹到components/目录下，一个是单文件上传，另外一个是多文件上传

* policy.js封装一个Promise，发送/thirdparty/oss/policy请求。vue项目会自动加上api前缀
* multiUpload.vue多文件上传。要改，改方式如下
* singleUpload.vue单文件上传。要替换里面的action中的内容。action=“http://gulimall-fermhan.oss-cn-qingdao.aliyuncs.com”

```
mall\renren-fast-vue\src\components\upload
```



brand-add-or-update.vue中

* 修改el-form-item label="品牌logo地址"内容。
* 要使用文件上传组件，先导入import SingleUpload from “@/components/upload/singleUpload”;
* 填入<single-upload v-model="dataForm.logo"></single-upload>
* 写明要使用的组件components: { SingleUpload },

点击一下文件上传，发现发送了两个请求

localhost:88/api/thirdparty/oss/policy?t=1613300654238

我们在后端准备好了签名controller，那么前端是在哪里获取的呢

policy.js

逻辑为先去访问我们的服务器获取policy，然后取阿里云，所以我们至少要发送2个请求

```javascript
import http from '@/utils/httpRequest.js'
export function policy() {
   return  new Promise((resolve,reject)=>{
        http({
            // 先去获取签名
            url: http.adornUrl("/third/party/oss/policy"),
            method: "get",
            params: http.adornParams({})
        }).then(({ data }) => {
            resolve(data);
        })
    });
}

```

而文件上传前调用的方法： :before-upload=“beforeUpload”

```javascript
发现该方法返回了一个new Promise，调用了policy()，该方法是policy.js中的
import { policy } from "./policy";

....
beforeUpload(file) {
      let _self = this;
      return new Promise((resolve, reject) => {
          
        policy() // 获取签名后得到相应
          .then(response => {
            // 意思是说policy获取到签名后，把签名信息保存起来
            // console.log("这是什么${filename}");
            _self.dataObj.policy = response.data.policy;
            _self.dataObj.signature = response.data.signature;
            _self.dataObj.ossaccessKeyId = response.data.accessid;
            _self.dataObj.key = response.data.dir +getUUID()+"_${filename}";
            _self.dataObj.dir = response.data.dir;
            _self.dataObj.host = response.data.host;
            resolve(true);
            // 总的来说什么意思呢？
            // 上传之前先请求签名，保存起来签名
            // 根据action="http://gulimall-fermhan.oss-cn-qingdao.aliyuncs.com"
            // 结合data信息，提交到云端
          })
          .catch(err => {
            console.log("出错了...",err)
            reject(false);
          });
      });
    },

```

在vue中看是response.data.policy，在控制台看response.policy。所以去java里面改返回值为R。

修改：OssController

```
public R policy(){
....
return R.ok().put("data", respMap);
```

![image-20210331121942042](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331121942042.png)

#### 阿里云开启跨域

开始执行上传，但是在上传过程中，出现了跨域请求问题：（服务去请求oss服务，前面说过了，跨域不是浏览器限制了你，而是新的服务器限制的问题，所以得去阿里云设置）

这又是一个跨域的问题，解决方法就是在阿里云上开启**跨域访问**：

在oss里面，设置我们bucket是可以跨域访问的

![image-20210331122332151](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331122332151.png)

![image-20210331122420331](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331122420331.png)

再次执行文件上传。

注意上传时他的key变成了response.data.dir +getUUID()+"_${filename}";

优化：上传后显示图片地址

显示图片：

```vue
<el-table-column prop="logo" header-align="center" align="center" label="品牌logo地址">
    <template slot-scope="scope">
        <!-- 自定义表格+自定义图片 -->
        <img :src="scope.row.logo" style="width: 100px; height: 80px" />
    </template>
</el-table-column>

```

![image-20210331123340834](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331123340834.png)





# 19、JSR303校验

问题引入：填写form时应该有前端校验，后端也应该有校验

前端的校验是element-ui表单验证https://element.eleme.cn/#/zh-CN/component/form



### 前端-表单校验&自定义校验器

Form 组件提供了表单验证的功能，只需要通过 rules 属性传入约定的验证规则，并将 Form-Item 的 prop 属性设置为需校验的字段名即可。校验规则参见 async-validator

使用自定义校验规则可以解决字母限制的问题

自定义校验规则

```javascript
dataRule: {
        name: [{ required: true, message: '品牌名不能为空', trigger: 'blur' }],
        logo: [
          { required: true, message: '品牌logo地址不能为空', trigger: 'blur' }
        ],
        descript: [
          { required: true, message: '介绍不能为空', trigger: 'blur' }
        ],
        showStatus: [
          {
            required: true,
            message: '显示状态[0-不显示；1-显示]不能为空',
            trigger: 'blur'
          }
        ],
        firstLetter: [
          {
            validator: (rule, value, callback) => {
              if (value === '') {
                callback(new Error('首字母必须填写'))
              } else if (!/^[a-zA-Z]$/.test(value)) {
                callback(new Error('首字母必须a-z或者A-Z之间'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ],
        sort: [
          {
            validator: (rule, value, callback) => {
              if (value === '') {
                callback(new Error('排序字段必须填写'))
              } else if (!Number.isInteger(value) || value < 0) {
                callback(new Error('排序必须是一个大于等于0的整数'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
```

我们只完成了前端的表单校验，还需要服务器端校验



### JSR303数据校验

![image-20210331134937734](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331134937734.png)



#### @NotNull等

步骤1：使用校验注解

在Java中提供了一系列的校验方式，它这些校验方式在“`javax.validation.constraints`”包中，提供了如@Email，@NotNull等注解

```xml
<dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>2.0.1.Final</version>
</dependency>

或者
<!--jsr3参数校验器-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
里面依赖了hibernate-validator

```

在非空处理方式上提供了@NotNull，@NotBlank和@NotEmpty

（1）@NotNull 该属性不能为null

（2）@NotEmpty 该字段不能为null或""

支持以下几种类型

* CharSequence (length of character sequence is evaluated)字符序列（字符序列长度的计算）
* Collection (collection size is evaluated) 集合长度的计算
* Map (map size is evaluated) map长度的计算
* Array (array length is evaluated) 数组长度的计算
* 上面什么意思呢？就是说如果标注的是map，它会帮你看长度

（3）@NotBlank：不能为空，不能仅为一个空格



在BrandEntity中

```java
/**
* 品牌名
*/
@NotBlank(message = "Brand name is required")
private String name;
```

#### @Valid内置异常

> 这里内置异常的意思是发生异常时返回的json不是我们的R对象，而是mvc的内置类

在添加了@NotBlank，还需要开启校验功能@Valid

BrandController中加校验注解@Valid，开启校验

```java
    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Valid @RequestBody BrandEntity brand){
		brandService.save(brand);

        return R.ok();
    }
```

测试： http://localhost:88/api/product/brand/save

在postman种发送上面的请求，可以看到返回的甚至不是R对象

```json
{
    "timestamp": "2021-03-29T09:20:46.383+0000",
    "status": 400,
    "error": "Bad Request",
    "errors": [
        {
            "codes": [
                "NotBlank.brandEntity.name",
                "NotBlank.name",
                "NotBlank.java.lang.String",
                "NotBlank"
            ],
            "arguments": [
                {
                    "codes": [
                        "brandEntity.name",
                        "name"
                    ],
                    "arguments": null,
                    "defaultMessage": "name",
                    "code": "name"
                }
            ],
            "defaultMessage": "不能为空",
            "objectName": "brandEntity",
            "field": "name",
            "rejectedValue": "",
            "bindingFailure": false,
            "code": "NotBlank"
        }
    ],
    "message": "Validation failed for object='brandEntity'. Error count: 1",
    "path": "/product/brand/save"
}

```

能够看到"defaultMessage": “不能为空”，这些错误消息定义在“hibernate-validator”的“\org\hibernate\validator\ValidationMessages_zh_CN.properties”文件中。在该文件中定义了很多的错误规则：

```
javax.validation.constraints.AssertFalse.message     = 只能为false
javax.validation.constraints.AssertTrue.message      = 只能为true
javax.validation.constraints.DecimalMax.message      = 必须小于或等于{value}
javax.validation.constraints.DecimalMin.message      = 必须大于或等于{value}
javax.validation.constraints.Digits.message          = 数字的值超出了允许范围(只允许在{integer}位整数和{fraction}位小数范围内)
javax.validation.constraints.Email.message           = 不是一个合法的电子邮件地址
javax.validation.constraints.Future.message          = 需要是一个将来的时间
javax.validation.constraints.FutureOrPresent.message = 需要是一个将来或现在的时间
javax.validation.constraints.Max.message             = 最大不能超过{value}
javax.validation.constraints.Min.message             = 最小不能小于{value}
javax.validation.constraints.Negative.message        = 必须是负数
javax.validation.constraints.NegativeOrZero.message  = 必须是负数或零
javax.validation.constraints.NotBlank.message        = 不能为空
javax.validation.constraints.NotEmpty.message        = 不能为空
javax.validation.constraints.NotNull.message         = 不能为null
javax.validation.constraints.Null.message            = 必须为null
javax.validation.constraints.Past.message            = 需要是一个过去的时间
javax.validation.constraints.PastOrPresent.message   = 需要是一个过去或现在的时间
javax.validation.constraints.Pattern.message         = 需要匹配正则表达式"{regexp}"
javax.validation.constraints.Positive.message        = 必须是正数
javax.validation.constraints.PositiveOrZero.message  = 必须是正数或零
javax.validation.constraints.Size.message            = 个数必须在{min}和{max}之间

org.hibernate.validator.constraints.CreditCardNumber.message        = 不合法的信用卡号码
org.hibernate.validator.constraints.Currency.message                = 不合法的货币 (必须是{value}其中之一)
org.hibernate.validator.constraints.EAN.message                     = 不合法的{type}条形码
org.hibernate.validator.constraints.Email.message                   = 不是一个合法的电子邮件地址
org.hibernate.validator.constraints.Length.message                  = 长度需要在{min}和{max}之间
org.hibernate.validator.constraints.CodePointLength.message         = 长度需要在{min}和{max}之间
org.hibernate.validator.constraints.LuhnCheck.message               = ${validatedValue}的校验码不合法, Luhn模10校验和不匹配
org.hibernate.validator.constraints.Mod10Check.message              = ${validatedValue}的校验码不合法, 模10校验和不匹配
org.hibernate.validator.constraints.Mod11Check.message              = ${validatedValue}的校验码不合法, 模11校验和不匹配
org.hibernate.validator.constraints.ModCheck.message                = ${validatedValue}的校验码不合法, ${modType}校验和不匹配
org.hibernate.validator.constraints.NotBlank.message                = 不能为空
org.hibernate.validator.constraints.NotEmpty.message                = 不能为空
org.hibernate.validator.constraints.ParametersScriptAssert.message  = 执行脚本表达式"{script}"没有返回期望结果
org.hibernate.validator.constraints.Range.message                   = 需要在{min}和{max}之间
org.hibernate.validator.constraints.SafeHtml.message                = 可能有不安全的HTML内容
org.hibernate.validator.constraints.ScriptAssert.message            = 执行脚本表达式"{script}"没有返回期望结果
org.hibernate.validator.constraints.URL.message                     = 需要是一个合法的URL

org.hibernate.validator.constraints.time.DurationMax.message        = 必须小于${inclusive == true ? '或等于' : ''}${days == 0 ? '' : days += '天'}${hours == 0 ? '' : hours += '小时'}${minutes == 0 ? '' : minutes += '分钟'}${seconds == 0 ? '' : seconds += '秒'}${millis == 0 ? '' : millis += '毫秒'}${nanos == 0 ? '' : nanos += '纳秒'}
org.hibernate.validator.constraints.time.DurationMin.message        = 必须大于${inclusive == true ? '或等于' : ''}${days == 0 ? '' : days += '天'}${hours == 0 ? '' : hours += '小时'}${minutes == 0 ? '' : minutes += '分钟'}${seconds == 0 ? '' : seconds += '秒'}${millis == 0 ? '' : millis += '毫秒'}${nanos == 0 ? '' : nanos += '纳秒'}


```

想要自定义错误消息，可以覆盖默认的错误提示信息，如@NotBlank的默认message是

```java
public @interface NotBlank {

	String message() default "{javax.validation.constraints.NotBlank.message}";

```

可以在添加注解的时候，修改message：

```java
	@NotBlank(message = "品牌名必须非空")
	private String name;

```

当再次发送请求时，得到的错误提示信息：

```json
{
    "timestamp": "2020-03-29T09:36:04.125+0000",
    "status": 400,
    "error": "Bad Request",
    "errors": [
        {
            "codes": [
                "NotBlank.brandEntity.name",
                "NotBlank.name",
                "NotBlank.java.lang.String",
                "NotBlank"
            ],
            "arguments": [
                {
                    "codes": [
                        "brandEntity.name",
                        "name"
                    ],
                    "arguments": null,
                    "defaultMessage": "name",
                    "code": "name"
                }
            ],
            "defaultMessage": "品牌名必须非空",
            "objectName": "brandEntity",
            "field": "name",
            "rejectedValue": "",
            "bindingFailure": false,
            "code": "NotBlank"
        }
    ],
    "message": "Validation failed for object='brandEntity'. Error count: 1",
    "path": "/product/brand/save"
}

```

但是返回的错误不是R对象，影响接收端的接收，我们可以通过局部异常处理或者统一一次处理解决



#### 局部异常处理BindResult

步骤3：给校验的Bean后，紧跟一个BindResult，就可以获取到校验的结果。拿到校验的结果，就可以自定义的封装。

如下两个方法是一体的

```java
@RequestMapping("/save")
public R save(@Valid @RequestBody BrandEntity brand){
    brandService.save(brand);

    return R.ok();
}

@RequestMapping("/save")
public R save(@Valid @RequestBody BrandEntity brand,
              BindingResult result){ // 手动处理异常

    if( result.hasErrors()){
        Map<String,String> map=new HashMap<>();
        //1.获取错误的校验结果
        result.getFieldErrors().forEach((item)->{
            //获取发生错误时的message
            String message = item.getDefaultMessage();
            //获取发生错误的字段
            String field = item.getField();
            map.put(field,message);
        });
        return R.error(400,"提交的数据不合法").put("data",map);
    }else {

    }
    brandService.save(brand);

    return R.ok();
}

```

```java
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
   private static final long serialVersionUID = 1L;

   /**
    * 品牌id
    */
   @TableId
   private Long brandId;
   /**
    * 品牌名
     这里用@NotBlank，表示该字符串必须要有实际内容且不能是空字符串“  ”
    */
   @NotBlank(message = "品牌名必须提交")
   private String name;
   /**
    * 品牌logo地址
    */
   @NotEmpty
   @URL(message = "logo必须是一个合法的url地址")
   private String logo;
   /**
    * 介绍
    */
   private String descript;
   /**
    * 显示状态[0-不显示；1-显示]
    */
   private Integer showStatus;
   /**
    * 检索首字母
    * 正则表达式在这里不需要加"/^[a-zA-Z]$/"
     * 这里用@NotEmpty表示，不能为null也不能空，就算为空字符串，“  ”，因为下面的 @Pattern会进一步检验字符串内容为一个字母
    */
   @NotEmpty
   @Pattern(regexp="^[a-zA-Z]$",message = "检索首字母必须是一个字母")
   private String firstLetter;
   /**
    * 排序
    */
   @NotNull
   @Min(value = 0,message = "排序必须大于等于0")
   private Integer sort;

}

```

这种是针对于该请求设置了一个内容校验，如果针对于每个请求都单独进行配置，显然不是太合适，实际上可以统一的对于异常进行处理。



### 统一异常处理`@ExceptionHandler`



上文说到 @ ExceptionHandler 需要进行异常处理的方法必须与出错的方法在同一个Controller里面。那么当代码加入了 @ControllerAdvice，则不需要必须在同一个 controller 中了。这也是 Spring 3.2 带来的新特性。从名字上可以看出大体意思是控制器增强。 也就是说，@controlleradvice + @ ExceptionHandler 也可以实现全局的异常捕捉。


（1）抽取一个异常处理类

- `@ControllerAdvice`标注在类上，通过“basePackages”能够说明处理哪些路径下的异常。
- `@ExceptionHandler(value = 异常类型.class)`标注在方法上

```java
/**
 * 集中处理所有异常
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.kexin.mall.product.controller")//管理的controller
public class MallExceptionControllerAdvice {

    @ExceptionHandler(value = Exception.class) // 也可以返回ModelAndView
    public R handleValidException(MethodArgumentNotValidException exception){

        Map<String,String> map=new HashMap<>();
        // 获取数据校验的错误结果
        BindingResult bindingResult = exception.getBindingResult();
        // 处理错误
        bindingResult.getFieldErrors().forEach(fieldError -> {
            String message = fieldError.getDefaultMessage();
            String field = fieldError.getField();
            map.put(field,message);
        });

        log.error("数据校验出现问题{},异常类型{}", exception.getMessage(), exception.getClass());

        return R.error(400,"数据校验出现问题").put("data",map);
    }
}

```

（2）测试： http://localhost:88/api/product/brand/save

（3）默认异常处理

```
@ExceptionHandler(value = Throwable.class)
public R handleException(Throwable throwable){
    log.error("未知异常{},异常类型{}",
              throwable.getMessage(),
              throwable.getClass());
    return R.error(BizCodeEnum.UNKNOW_EXEPTION.getCode(),
                   BizCodeEnum.UNKNOW_EXEPTION.getMsg());
}

```

（4）**错误状态码**

上面代码中，针对于错误状态码，是我们进行随意定义的，然而正规开发过程中，错误状态码有着严格的定义规则，如该在项目中我们的错误状态码定义

上面的用法主要是通过@Controller+@ExceptionHandler来进行异常拦截处理

BizCodeEnum

为了定义这些错误状态码，我们可以单独定义一个常量类，用来存储这些错误状态码



```java
package com.kexin.common.exception;

/***
 * 错误码和错误信息定义类
 * 1. 错误码定义规则为5为数字
 * 2. 前两位表示业务场景，最后三位表示错误码。例如：100001。10:通用 001:系统未知异常
 * 3. 维护错误码后需要维护错误描述，将他们定义为枚举形式
 * 错误码列表：
 *  10: 通用
 *      001：参数格式校验
 *  11: 商品
 *  12: 订单
 *  13: 购物车
 *  14: 物流
 */

public enum BizCodeEnum {
    UNKNOW_EXEPTION(10000,"系统未知异常"),

    VALID_EXCEPTION( 10001,"参数格式校验失败");

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

```

```java

/**
 * 集中处理所有异常
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.kexin.mall.product.controller")//管理的controller
public class MallExceptionControllerAdvice {

    @ExceptionHandler(value = Exception.class) // 也可以返回ModelAndView
    public R handleValidException(MethodArgumentNotValidException exception){

        Map<String,String> map=new HashMap<>();
        // 获取数据校验的错误结果
        BindingResult bindingResult = exception.getBindingResult();
        // 处理错误
        bindingResult.getFieldErrors().forEach(fieldError -> {
            String message = fieldError.getDefaultMessage();
            String field = fieldError.getField();
            map.put(field,message);
        });

        log.error("数据校验出现问题{},异常类型{}", exception.getMessage(), exception.getClass());

        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(),
                BizCodeEnum.VALID_EXCEPTION.getMsg());
    }

    // 默认异常处理，能处理任意类型的异常
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){
        log.error("未知异常{},异常类型{}",
                throwable.getMessage(),
                throwable.getClass());
        return R.error(BizCodeEnum.UNKNOW_EXEPTION.getCode(),
                BizCodeEnum.UNKNOW_EXEPTION.getMsg());
    }

}

```



### JSR303分组校验功能（多场景校验）

前面解决了统一异常处理，但是现状有新的需求是对同一实体类参数也要区分场景

如果新增和修改两个接口需要验证的字段不同，比如id字段，新增可以不传递，但是修改必须传递id，我们又不可能写两个vo来满足不同的校验规则。所以就需要用到分组校验来实现。

步骤：

在common里面抽取一个校验的包valid，里面添加两个接口：AddGroup和UpdateGroup

* 创建分组接口Insert.class Update.class
* 在VO的属性中标注@NotBlank等注解，如@NotNull(message = “用户姓名不能为空”,groups = {Insert.class,Update.class})
* controller的方法上或者方法参数上写分组的接口信息，如@Validated(AddGroup.class)

##### 1、@NotNull(groups={A.class})

1、给校验注解，标注上groups，指定什么情况下才需要进行校验

如：指定在更新和添加的时候，都需要进行校验。新增时不需要带id，修改时必须带id

在实体类的统一属性上添加多个不同的校验注解

```java
/**
	 * 品牌id
	 */
	@NotNull(message = "修改必须指定品牌id", groups = {UpdateGroup.class})
	@Null(message = "新增不能指定id", groups = {AddGroup.class})
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 */
	@NotBlank(message = "Brand name is required", groups = {AddGroup.class, UpdateGroup.class})
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank(message="logo必须是一个合法的URL地址", groups = {AddGroup.class})
	//@URL(message = "logo必须是一个合法的URL地址", groups={AddGroup.class, UpdateGroup.class})
	private String logo;
	//注意上面因为@NotBlank没有指定UpdateGroup分组，所以不生效。此时update时可以不携带，但带了一定得是url地址
```

> 在这种情况下，没有指定分组的校验注解，默认是不起作用的。想要起作用就必须要加groups。



![image-20210331142121880](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210331142121880.png)

##### 2、@Validated

业务方法参数上使用@Validated注解

@Validated的value值指定要使用的一个或多个分组

JSR-303 defines validation groups as custom annotations which an application declares for the sole purpose of using
them as type-safe group arguments, as implemented in SpringValidatorAdapter.

JSR-303 将验证组定义为自定义注释，应用程序声明的唯一目的是将它们用作类型安全组参数，如 SpringValidatorAdapter 中实现的那样。

Other SmartValidator implementations may support class arguments in other ways as well.

其他SmartValidator 实现也可以以其他方式支持类参数。

```java
// 新增场景添加 新增分组注解 
@RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand){
        brandService.save(brand);

        return R.ok();
    }
// 删除场景添加 删除分组注解
@RequestMapping("/delete")
public R delete(@RequestBody Long[] brandIds) {
    brandService.removeByIds(Arrays.asList(brandIds));

    return R.ok();
}
```

总结：controller接收到之后，根据@Validated表明的分组信息，品牌对应的校验注解。



### 分组校验的默认校验

这里要是指定了分组，实体类上的注解就是指定了分组的注解才生效，

没有指定分组的默认不生效，要是没有指定分组，就是对没有指定分组的注解生效，指定分组的注解就不生效了

可以在自定义的异常分组接口中继承Default类。所有没有写明group的都属于Default分组。

> 此外还可以在实体类上标注@GroupSequece({A.class,B.class})指定校验顺序
>
> 通过@GroupSequence指定验证顺序：先验证A分组，如果有错误立即返回而不会验证B分组，接着如果A分组验证通过了，那么才去验证B分组，最后指定User.class表示那些没有分组的在最后。这样我们就可以实现按顺序验证分组了。
>
> 关于Default，此处我springvalidation默认生成的验证接口，验证的范围是所有带有验证信息的属性，
>
> 若是属性上方写了验证组，则是验证该组内的属性
>
> 若是验证实体类类上写了GroupSequence({}) 则说明重写了Default验证接口，Default就按照GroupSequence里所写的组信息进行验证
> 



### 自定义校验注解

Hibernate Validator提供了一系列内置的校验注解，可以满足大部分的校验需求。但是，仍然有一部分校验需要特殊定制，例如某个字段的校验，我们提供两种校验强度，当为normal强度时我们除了<>号之外，都允许出现。当为strong强度时，我们只允许出现常用汉字，数字，字母。内置的注解对此则无能为力，我们试着通过自定义校验来解决这个问题。

场景：要校验showStatus的0/1状态，可以用正则，但我们可以利用其他方式解决复杂场景。比如我们想要下面的场景

```java
/**
	 * 显示状态[0-不显示；1-显示]
	 */
@NotNull(groups = {AddGroup.class, UpdateStatusGroup.class})
@ListValue(vals = {0,1}, groups = {AddGroup.class, UpdateGroup.class, UpdateStatusGroup.class})
private Integer showStatus;
```

添加依赖

```java
<!--校验-->
<dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>2.1.0.Final</version>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>5.4.1.Final</version>
</dependency>
<!--高版本需要javax.el-->
<dependency>
    <groupId>org.glassfish</groupId>
    <artifactId>javax.el</artifactId>
    <version>3.0.1-b08</version>
</dependency>

```

在common valid目录添加一个ListValue注解 file

##### 1、自定义校验注解

必须有3个属性

- message()错误信息

- groups()分组校验

- payload()自定义负载信息

- ```java
  // 自定义注解
  @Documented
  @Constraint(validatedBy = { ListValueConstraintValidator.class}) // 校验器
  @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE }) // 哪都可以标注
  @Retention(RUNTIME)
  public @interface ListValue {
      // 使用该属性去Validation.properties中取
      String message() default "{com.atguigu.common.valid.ListValue.message}";
  
      Class<?>[] groups() default { };
  
      Class<? extends Payload>[] payload() default { };
  
      // 数组，需要用户自己指定
      int[] value() default {};
  }
  
  ```


因为上面的message值对应的最终字符串需要去ValidationMessages.properties中获得，所以我们在common resources中新建文件`ValidationMessages.properties`

文件内容

```
com.kexin.common.valid.ListValue.message=必须提交指定的值 [0,1]
```



##### 2、自定义校验器ConstraintValidator

上面只是定义了异常消息，但是怎么验证是否异常还没说，下面的ConstraintValidator就是说的，比如我们要限定某个属性值必须在一个给定的集合里，那么就通过重写initialize()方法，指定可以有哪些元素。而controller接收到的数据用isValid(验证

编写一个自定义的检验器[自定义校验器ConstraintValidator]

ListValueConstraintValidator

```java
public class ListValueConstraintValidator
        implements ConstraintValidator<ListValue,Integer> { //<注解,校验值类型>

    // 存储所有可能的值
    private Set<Integer> set=new HashSet<>();

    @Override // 初始化方法，你可以获取注解上的内容并进行处理
    public void initialize(ListValue constraintAnnotation) {
        // 获取后端写好的限制 // 这个value就是ListValue里的value，我们写的注解是@ListValue(value={0,1})
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }
    }

    /**
     * 判断是否校验成功
     * @param value  需要校验的值
     * @param context
     * @return
     */
    @Override // 覆写验证逻辑
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // 看是否在限制的值里
        return  set.contains(value);
    }
}

```

具体的校验类需要实现ConstraintValidator接口，第一个泛型参数是所对应的校验注解类型，第二个是校验对象类型。在初始化方法initialize中，我们可以先做一些别的初始化工作，例如这里我们获取到注解上的value并保存下来，然后生成set对象。

真正的验证逻辑由isValid完成，如果传入形参的属性值在这个set里就返回true，否则返回false


##### 3、关联校验器和校验注解

```java
@Constraint(validatedBy = { ListValueConstraintValidator.class})
```

一个校验注解可以匹配多个校验器



##### 4、使用实例

```java
	/**
	 * 显示状态[0-不显示；1-显示]
	  用value[]指定可以写的值
	 */
	@ListValue(value = {0,1},groups ={AddGroup.class})
	private Integer showStatus;

```



把BrandController中的update修改，拆分成两个，新增一个专门用来update status

新增一个UpdateStatusGroup接口

```java
    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }
```



# 20、商品SPU和SKU管理

### 属性分组-概念

从sql文件中重新添加admin库的sys_menu表，刷新后左边菜单全部有了

* **SPU**：standard product unit(标准化产品单元)：是商品信息聚合的最小单位，是一组可复用、易检索的标准化信息的集合，该集合描述了一个产品的特性。 
  * 如iphoneX是SPU
* **SKU**：stock keeping unit(库存量单位)：库存进出计量的基本单元，可以是件/盒/托盘等单位。SKU是对于大型连锁超市DC配送中心物流管理的一个必要的方法。现在已经被引申为产品统一编号的简称，每种产品对应有唯一的SKU号。
  * 如iphoneX 64G 黑色 是SKU
* 基础属性：同一个SPU拥有的特性叫基本属性。如机身长度，这个是手机共用的属性。而每款手机的属性值不同
  * 也可以叫规格参数
* 销售属性：能决定库存量的叫销售属性。如颜色



**基本属性〖规格参数〗与销售属性**
每个分类下的商品共享规格参数，与销售属性。只是有些商品不一定要用这个分类下全部的属性；

* 属性是以三级分类组织起来的
* 规格参数中有些是可以提供检索的
* 规格参数也是基本属性，他们具有自己的分组
* 属性的分组也是以三级分类组织起来的
* 属性名确定的，但是值是每一个商品不同来决定的



**pms数据库表**
pms数据库下的attr属性表，attr-group表

* attr-group-id：几号分组
* catelog-id：什么类别下的，比如手机

根据商品找到spu-id，attr-id

属性关系-规格参数-销售属性-三级分类 关联关系

> 每个分类有特点的属性

先通过分类找到对应的属性分组，然后根据属性分组查到拥有的属性

![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/fe2f951a6554c1105732e62ea00e99a3.png)

SPU-SKU属性表

![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/9f66b3989f3fa31091786551aad7da23.png)

荣耀V20有两个属性，网络和像素，但是这两个属性的spu是同一个，代表是同款手机。

sku表里保存spu是同一手机，sku可能相同可能不同，相同代表是同一款，不同代表是不同款。

属性表说明每个属性的 枚举值

分类表有所有的分类，但有父子关系





### 前端组件抽取&父子组件交互

点击子组件，父组件触发事件

后台：商品系统/平台属性/属性分组



抽取一个三级分类的组件，创建一个common目录，添加catogary.vue组件

为了属性分组目的，在product目录下添加attrgroup.vue组件（<!--左右布局，左边一个小的菜单，右边为较大的表格-->）

父子组件通讯：左侧菜单（common的category组件）被点击时候，要通知父组件（attrgroup这个页面）去更新右边对应table的内容

![image-20200430215649355](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/b54bb02ae813a74fed74f6236ac40fdf.png)

根据其他的请求地址http://localhost:8001/#/product-attrgroup

所以应该有product/attrgroup.vue。我们之前写过product/cateory.vue，现在我们要抽象到common/cateory.vue（也就是左侧的tree单独成一个vue组件）

1）左侧内容：

要在左面显示菜单，右面显示表格。复制`<el-row :gutter="20">。。。`，放到attrgroup.vue的`<template>`。20表示列间距

去element-ui文档里找到布局

```
<el-row :gutter="20">
    <el-col :span="6"> <div class="grid-content bg-purple"></div></el-col>
    <el-col :span="18"><div class="grid-content bg-purple"></div></el-col>
</el-row>
```

分为2个模块，分别占6列和18列（分别是tree和当前spu等信息）

有了布局之后，要在里面放内容。接下来要抽象一个分类vue。新建common/category，生成vue模板。把之前写的el-tree放到`<template>`

```
<el-tree :data="menus" 
         :props="defaultProps" node-key="catId" ref="menuTree" @node-click="nodeClick"	></el-tree>
所以他把menus绑定到了菜单上，
所以我们应该在export default {中有menus的信息
该具体信息会随着点击等事件的发生会改变值（或比如created生命周期时），
tree也就同步变化了

```

common/category写好后，就可以在attrgroup.vue中导入使用了

```
<script>
import Category from "../common/category";
export default {
  //import引入的组件需要注入到对象中才能使用。组件名:自定义的名字，一致可以省略
  components: { Category},

```

导入了之后，就可以在`attrgroup.vue`中找合适位置放好

```
<template>
<el-row :gutter="20">
    <el-col :span="6">
        <category @tree-node-click="treenodeclick"></category>
    </el-col>

```

2）右侧表格内容：

开始填写属性分组页面右侧的表格

复制mall-product\src\main\resources\src\views\modules\product\attrgroup.vue中的部分内容div到attrgroup.vue

批量删除是弹窗add-or-update

导入data、结合components



##### **父子组件**

要实现功能：点击左侧，右侧表格对应内容显示。

父子组件传递数据：category.vue点击时，引用它的attgroup.vue能感知到， 然后通知到add-or-update

比如嵌套div，里层div有事件后冒泡到外层div（是指一次点击调用了两个div的点击函数）

1）子组件（category）给父组件（attrgroup）传递数据，事件机制；

去element-ui的tree部分找event事件，看node-click()

在category中绑定node-click事件

```
<el-tree :data="menus" :props="defaultProps" node-key="catId" ref="menuTree" 
         @node-click="nodeClick"	></el-tree>

```

**this.$emit()**

2）子组件给父组件发送一个事件，携带上数据；

```javascript
nodeClick(data,Node,component){
    console.log("子组件被点击",data,Node,component);
    this.$emit("tree-node-click",data,Node,component);
}, 
    第一个参数事件名字随便写，
    后面可以写任意多的东西，事件发生时都会传出去

```

this.$emit(事件名,“携带的数据”);

3）父组件中的获取发送的事件]

```
在attr-group中写
<category @tree-node-click="treeNodeClick"></category>
表明他的子组件可能会传递过来点击事件，用自定义的函数接收传递过来的参数

```

```javascript
 父组件中进行处理
//获取发送的事件数据
    treeNodeClick(data,Node,component){
     console.log("attgroup感知到的category的节点被点击",data,Node,component);
     console.log("刚才被点击的菜单ID",data.catId);
    },

```



### 获取分类属性分组



**查询功能：**

GET /product/attrgroup/list/{catelogId}

后端修改AttrGroupController里面获取list的函数，要改成根据catId来获取

```java
 /**
     * 列表
     * @param  catelogId 0的话查所有
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params,@PathVariable Long catelogId){
        //        PageUtils page = attrGroupService.queryPage(params);

        PageUtils page = attrGroupService.queryPage(params,catelogId);

        return R.ok().put("page", page);
    }
```

增加接口与实现

Query里面就有个方法getPage()，传入map，将map解析为mybatis-plus的IPage<T>对象
自定义PageUtils类用于传入IPage对象，得到其中的分页信息
AttrGroupServiceImpl extends ServiceImpl，其中ServiceImpl的父类中有方法page(IPage, Wrapper)。对于wrapper而言，没有条件的话就是查询所有
queryPage()返回前还会return new PageUtils(page);，把page对象解析好页码信息，就封装为了响应数据

```java
@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override   // 按关键字或者按id查,根据分类返回属性分组 
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        // 检索条件（常用与模糊查询关键字）
        String key = (String) params.get("key");

        // 构造检索条件
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();

        // select * from AttrGroup where attr_group_id=key or attr_group_name=key
        // key不为空
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) ->
                    obj.eq("attr_group_id", key)
                            .or()
                            .like("attr_group_name", key)
            );
        }

        // 0表示查询所有
        if (catelogId == 0) {
            // Query可以把map封装为IPage // this.page(IPage,QueryWrapper)
            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        } else {
            // 非0，要按照三级分类查询
            // 增加id信息
            wrapper.eq("catelog_id", catelogId);

            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        }
    }

}
```

测试

查1分类的属性分组：localhost:88/api/product/attrgroup/list/1

查1分类的属性分组并且分页、关键字为aa：localhost:88/api/product/attrgroup/list/1?page=1&key=aa。结果当然查不到

```json
{
    "msg": "success",
    "code": 0,
    "page": {
        "totalCount": 0,
        "pageSize": 10,
        "totalPage": 0,
        "currPage": 1,
        "list": []
    }
}

```

然后调整前端

发送请求时url携带id信息，${this.catId}，get请求携带page信息

点击第3级分类时才查，修改attr-group.vue中的函数即可

```javascript
//感知树节点被点击
treenodeclick(data, node, component) {
    if (node.level == 3) {
        this.catId = data.catId;
        this.getDataList(); //重新查询
    }
},
    
// 获取数据列表
getDataList() {
    this.dataListLoading = true;
    this.$http({
        url: this.$http.adornUrl(`/product/attrgroup/list/${this.catId}`),
        method: "get",
        params: this.$http.adornParams({
            page: this.pageIndex,
            limit: this.pageSize,
            key: this.dataForm.key
        })
    }).then(({ data }) => {
        if (data && data.code === 0) {
            this.dataList = data.page.list;
            this.totalPage = data.page.totalCount;
        } else {
            this.dataList = [];
            this.totalPage = 0;
        }
        this.dataListLoading = false;
    });
},

```



### 分组新增&级联选择器

上面演示了查询功能，下面写insert分类

但是想要下面这个效果：因为分类可以对应多个属性分组，所以我们新增的属性分组时要指定分类

![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/20210216172146.png)

下拉菜单应该是手机一级分类的，这个功能是级联选择器

级联选择器<el-cascader
级联选择：https://element.eleme.cn/#/zh-CN/component/cascader

级联选择的下拉同样是个options数组，多级的话用children属性即可

> 只需为 Cascader 的`options`属性指定选项数组即可渲染出一个级联选择器。通过`props.expandTrigger`可以定义展开子级菜单的触发方式。

去vue里找src\views\modules\product\attrgroup-add-or-update.vue，修改对应的位置为`<el-cascader ....>`

把data()里的数组categorys绑定到options上即可，更详细的设置可以用props绑定



优化：想让第三级得children返回null而不是[]，解决级联选择器问题，在java端把children属性空值去掉，空集合时去掉children字段

在CategoryEmtity里面`@JsonInclude`

```java
/**
	 * 包含其所有子分类, （不是数据表里面的相关属性，需要注解）
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)// 不为空时返回的json才带该字段
	@TableField(exist = false)
	private List<CategoryEntity> children;
```

提交完后返回页面也刷新了，是用到了父子组件。在`$message`弹窗结束回调`$this.emit`

接下来要解决的问题是，修改了该vue后，新增是可以用，修改回显就有问题了，应该回显3级



### 分组修改&级联选择器回显

回显是需要完整的路径，前端在attrgroup-add-or-update组件的init里面，还要添加一个// 查出catelogId的完整路径

前端：

```vue
<el-button
           type="text"
           size="small"
           @click="addOrUpdateHandle(scope.row.attrGroupId)"
           >修改</el-button>

<script>
    // 新增 / 修改
    addOrUpdateHandle(id) {
        // 先显示弹窗
        this.addOrUpdateVisible = true;
        // .$nextTick(代表渲染结束后再接着执行
        this.$nextTick(() => {
            // this是attrgroup.vue
            // $refs是它里面的所有组件。在本vue里使用的时候，标签里会些ref=""
            // addOrUpdate这个组件
            // 组件的init(id);方法
            this.$refs.addOrUpdate.init(id);
        });
    },
</script>
在init方法里进行回显
但是分类的id还是不对，应该是用数组封装的路径

```

根据属性分组id查到属性分组后填充到页面

```javascript
init(id) {
    this.dataForm.attrGroupId = id || 0;
    this.visible = true;
    this.$nextTick(() => {
        this.$refs["dataForm"].resetFields();
        if (this.dataForm.attrGroupId) {
            this.$http({
                url: this.$http.adornUrl(
                    `/product/attrgroup/info/${this.dataForm.attrGroupId}`
                ),
                method: "get",
                params: this.$http.adornParams()
            }).then(({ data }) => {
                if (data && data.code === 0) {
                    this.dataForm.attrGroupName = data.attrGroup.attrGroupName;
                    this.dataForm.sort = data.attrGroup.sort;
                    this.dataForm.descript = data.attrGroup.descript;
                    this.dataForm.icon = data.attrGroup.icon;
                    this.dataForm.catelogId = data.attrGroup.catelogId;
                    //查出catelogId的完整路径
                    this.catelogPath =  data.attrGroup.catelogPath;
                }
            });
        }
    });

```



后端

修改AttrGroupEntity

```java
/**
	 * 三级分类修改的时候回显路径
	 */
@TableField(exist = false)// 数据库中不存在
private Long[] catelogPath;
```

修改controller，找到属性分组id对应的分类，然后把该分类下的所有属性分组都填充好

```java
    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        // 用当前当前分类id查询完整路径并写入 attrGroup
        Long[] path = categoryService.findCateLogPath(attrGroup.getCatelogId());
        attrGroup.setCatelogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }
```

添加service

```java
@Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        paths = findParentPath(catelogId, paths);
        // 收集的时候是顺序 前端是逆序显示的 所以用集合工具类给它逆序一下
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }
    /**
     * 递归收集所有父节点
     */
    private List<Long> findParentPath(Long catlogId, List<Long> paths) {
        // 1、收集当前节点id
        paths.add(catlogId);
        CategoryEntity byId = this.getById(catlogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }
```

优化：会话关闭时清空内容，防止下次开启还遗留数据



### 品牌分类关联&级联更新（难）mybatis-plus

要做一个分页插件

mybatis-plus用法

官网：https://mp.baomidou.com/guide/page.html

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version></version>
</dependency>

```

mp常用注解
比如@TableName，标注在实体类上，使用的时候定义mapper接口指定实体类泛型即可

也可以使用@TableField映射属性和数据库字段

@TableLogic用于逻辑删除

wrapper
查询条件用QueryWrapper包装

wrapper.allEq(map);用于指定字段值

wrapper.gt(“age”,2);// 大于 // 用于指定字段与常数关系


##### mp分页使用

需要先添加个mybatis的拦截器

```java
@EnableTransactionManagement  // 开启使用
@MapperScan("com.kexin.mall.product.dao")
@Configuration
public class MyBatisConfig {

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        paginationInterceptor.setOverflow(true);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }
}
```

- 接口`IPage<User> selectPageVo(Page<?> page, Integer state);`
- xml：不变
- 接收的返回值`IPage<T>`

```java
Page page = new Page<>(2,2);
Page result = mapper.selectPage(page,null);
result.getRecords()
```

> 如果要自定义SQL，在接口里单独写@Select注解或者在xml里写好即可

##### Query

在Service实现层 this.page(Page,QueryWrapper)

项目中用的分页方式，不是自己创建page对象，而是根据url的参数自动封装

```java
package com.kexin.common.utils;

public class Query<T> {

    public IPage<T> getPage(Map<String, Object> params) {
        return this.getPage(params, null, false);
    }

    public IPage<T> getPage(Map<String, Object> params,  // 参数有curPage limit order  sidx  asc
                            String defaultOrderField,// 默认排序字段
                            boolean isAsc) { // 默认降序
        //分页参数
        long curPage = 1;
        long limit = 10;
        // new Page<>(curPage, limit);   .
        // page.addOrder(OrderItem.asc(orderField));
        // page.addOrder(OrderItem.desc(orderField));
        // page.addOrder(OrderItem.asc(defaultOrderField));
        // page.addOrder(OrderItem.desc(defaultOrderField));

        // 页码
        if(params.get(Constant.PAGE) != null){
            curPage = Long.parseLong((String)params.get(Constant.PAGE));
        }
        // 偏移
        if(params.get(Constant.LIMIT) != null){
            limit = Long.parseLong((String)params.get(Constant.LIMIT));
        }

        // 分页对象  mybatis-plus内容，实现Ipage
        Page<T> page = new Page<>(curPage, limit);

        // 分页参数
        params.put(Constant.PAGE, page);

        // 排序字段
        // 防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderField = SQLFilter.sqlInject((String)params.get(Constant.ORDER_FIELD));
        String order = (String)params.get(Constant.ORDER);


        // 前端字段排序
        if(StringUtils.isNotEmpty(orderField) && StringUtils.isNotEmpty(order)){
            if(Constant.ASC.equalsIgnoreCase(order)) {
                return  page.addOrder(OrderItem.asc(orderField));
            }else {
                return page.addOrder(OrderItem.desc(orderField));
            }
        }
        // 如果已经传来了排序字段，已经返回了

        // 没有排序字段，则不排序
        if(StringUtils.isBlank(defaultOrderField)){
            return page;
        }

        // 默认排序
        if(isAsc) {
            page.addOrder(OrderItem.asc(defaultOrderField));
        }else {
            page.addOrder(OrderItem.desc(defaultOrderField));
        }

        return page;
    }
}


```

**常规用法：XML 自定义分页**

> 这种用法其实是mybatis的内容

- UserMapper.java 方法内容

  ```java
  public interface UserMapper {//可以继承或者不继承BaseMapper
      /**
       * @param page 分页对象,xml中可以从里面进行取值,传递参数 Page 即自动分页,必须放在第一位(你可以继承Page实现自己的分页对象)
       * @param state 状态
       * @return 分页对象
       */
      IPage<User> selectPageVo(Page<?> page, Integer state);
  }
  ```

- UserMapper.xml 等同于编写一个普通 list 查询，mybatis-plus 自动替你分页

  ```java
  <select id="selectPageVo" resultType="com.baomidou.cloud.entity.UserVo">
      SELECT id,name FROM user WHERE state=#{state}
  </select>
  ```

- UserServiceImpl.java 调用分页方法

  ```java
  public IPage<User> selectUserPage(Page<User> page, Integer state) {
      // 不进行 count sql 优化，解决 MP 无法自动优化 SQL 问题，这时候你需要自己查询 count 部分
      // page.setOptimizeCountSql(false);
      // 当 total 为小于 0 或者设置 setSearchCount(false) 分页插件不会进行 count 查询
      // 要点!! 分页返回的对象与传入的对象是同一个
      return userMapper.selectPageVo(page, state);
  }
  ```



**模糊查询**

去到BrandController里面的查询列表，进入queryPage服务函数

```java
@Override // BrandServiceImpl
public PageUtils queryPage(Map<String, Object> params) {
    QueryWrapper<BrandEntity> wrapper = new QueryWrapper<>();
    String key = (String) params.get("key");
    if(!StringUtils.isEmpty(key)){
        // 字段等于  or  模糊查询
        wrapper.eq("brand_id", key).or().like("name", key);
    }
    // 按照分页信息和查询条件  进行查询
    IPage<BrandEntity> page = this.page(
        // 传入一个IPage对象，他是接口，实现类是Page
        new Query<BrandEntity>().getPage(params),
        wrapper
    );
    return new PageUtils(page);
}

```

**Ipage**

```java
// Page对象指定页码和条数，其中的泛型是数据类型

// this.page()是Iservice里的方法
default <E extends IPage<T>> E page(E page,
                                    Wrapper<T> queryWrapper) {
    return this.getBaseMapper().selectPage(page, queryWrapper);
}

```





##### 关联分类/商品

新增的华为、小米、oppo都应该是手机下的品牌，但是品牌对分类可能是一对多的，比如小米对应手机和电视

多对多的关系应该有relation表

修改CategoryBrandRelationController的逻辑

API：https://easydoc.xyz/doc/75716633/ZUqEdvA4/SxysgcEF

CategoryBrandRelationController

```java
/**
     * 获取当前品牌的所有分类列表
     */
    @GetMapping("/catelog/list")
    public R list(@RequestParam("brandId") Long brandId){
        // 根据品牌id获取其分类信息（返回关联关系这个实体类）
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId)
        );
        return R.ok().put("data", data);
    }
    // 获得分类列表后再继续进行后面的工作
    
 // 重写save

```

##### 关联表的优化

分类名本可以在brand表中，但因为**关联查询对数据库性能有影响**，在电商中大表数据从不做关联，哪怕**分步查**也不用关联

所以像name这种冗余字段可以保存，优化save，**保存时用关联表存好，但select时不用关联**

```java
    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:categorybrandrelation:save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }
```



```java
 /**
     * 根据获取品牌id 、三级分类id查询对应的名字保存到数据库
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        // 获取品牌id 、三级分类id
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();

        // 根据id去查 品牌名字、分类名字，统一放到一个表里，就不关联分类表查了
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

        // 把查到的设置到要保存的哪条数据里
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        
        this.save(categoryBrandRelation);
    }
```

最终效果：

![image-20210401095335687](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401095335687.png)



##### 保持冗余字段的数据一致

但是如果分类表里的name发送变化，那么品牌表里的分类name字段应该同步变化。

**第一种方法：用updateWrapper**

所以应该修改brand-controller，使之update时检测分类表里的name进行同步

brandServiceImpl

```java
@Override
    public void updateDetail(BrandEntity brand) {
        // 保证冗余字段的数据一致
        this.updateById(brand);
        if(!StringUtils.isEmpty(brand.getName())){
            // 同步更新其他关联表中的数据
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());
            // TODO 更新其他关联
        }
    }
```

CategoryBrandRelationServiceImpl

```java
 @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        this.update(relationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));

    }
```



**第二种方式 生成自己的dao**（我觉得比上一种要麻烦）

categotyController里面的update也要修改

```java
@RequestMapping("/update")
    //@RequiresPermissions("product:category:update")
    public R update(@RequestBody CategoryEntity category){
		categoryService.updateCascade(category);
        return R.ok();
    }
```

CategoryServiceImpl

```java
    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional  //因为涉及到多次修改，因此要开启事务
    @Override
    //@CacheEvict(value = {"category"},allEntries = true)
    public void updateCascade(CategoryEntity category) {
        this.updateById(category); // 更新自己
        if (!StringUtils.isEmpty(category.getName())) { // 更新关联表里面的
            categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
        }
    }
```

dao.xml文件

```xml
<update id="updateCategory">
    update pms_category_brand_relation set catelog_name=#{name} WHERE catelog_id=#{catId}
</update>
```

```java
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {

    void updateCategory(@Param("catId")Long catId, @Param("name")String name);
}

```



# 21、平台属性

### 规格参数新增与vo

![image-20210401103916911](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401103916911.png)

为查询全部功能添加模糊查询功能

在AttrGroupController中的@RequestMapping("/list/{catelogId}")，去到queryPage

```java
@Override   // 按关键字或者按id查
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        // 检索条件（常用与模糊查询关键字）
        String key = (String) params.get("key");

        // 构造检索条件
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();

        // select * from AttrGroup where attr_group_id=key or attr_group_name=key
        // key不为空
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> obj
                    .eq("attr_group_id", key)
                    .or()
                    .like("attr_group_name", key)
            );
        }

        // 0表示查询所有
        if (catelogId == 0) {
            // Query可以把map封装为IPage // this.page(IPage,QueryWrapper)
            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        } else {
            // 非0，要按照三级分类查询
            // 增加id信息
            wrapper.eq("catelog_id", catelogId);

            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        }
    }
```

要做关联，来到规格参数里面



格参数新增时，请求的URL：Request URL:

http://localhost:88/api/product/attr/base/list/0?t=1588731762158&page=1&limit=10&key=

当有新增字段时，我们往往会在entity实体类中新建一个字段，并标注数据库中不存在该字段

```java
如在一些Entity中，为了让mybatis-plus与知道某个字段不与数据库匹配，
    那么就加个
    @TableField(exist=false)
    private Long attrGroupId;
```

然而这种方式并不规范，比较规范的做法是，新建一个`vo`文件夹，将每种不同的对象，按照它的功能进行了划分。在java中，涉及到了这几种类型

PO、DO、TO、DTO

**1．PO持久对象**

> PO就是对应数据库中某个表中的一条记录，多个记录可以用PO的集合。PO中应该不包含任何对数据的操作。
>

**2、DO（Domain 0bject)领域对象**

> 就是从现实世界中推象出来的有形或无形的业务实体。
>

**3.TO(Transfer 0bject)，数据传输对象传输的对象**

> 不同的应用程序之间传输的对象。微服务
>

**4.DTO(Data Transfer Obiect)数据传输对象**

> 这个概念来源于J2EE的设汁模式，原来的目的是为了EJB的分布式应用握供粗粒度的数据实体，以减少分布式调用的次数，从而握分布式调用的性能和降低网络负载，但在这里，泛指用于示层与服务层之间的数据传输对象。
>

**5.VO(value object)值对象**

> 通常用于业务层之间的数据传递，和PO一样也是仅仅包含数据而已。但应是抽象出的业务对象，可以和表对应，也可以不，这根据业务的需要。用new关韃字创建，由GC回收的
>

View object：视图对象

接受页面传递来的对象，封装对象

将业务处理完成的对象，封装成页面要用的数据

**6.BO(business object)业务对象**

> 从业务模型的度看．见IJML元#领嵫模型的领嵫对象。封装业务逻辑的java对象，通过用DAO方法，结合PO,VO进行业务操作。businessobject:业务对象主要作用是把业务逻辑封装为一个对苤。这个对象可以包括一个或多个其它的对彖。比如一个简历，有教育经历、工怍经历、社会关系等等。我们可以把教育经历对应一个PO工作经历
>

**7、POJO简单无规则java对象**

**8、DAO**



**新建VO对象**
Request URL: http://localhost:88/api/product/attr/save，现在的情况是，它在保存的时候，只是保存了attr，并没有保存attrgroup，为了解决这个问题，我们新建了一个vo/AttrVo.java，在原Attr基础上增加了attrGroupId字段，使得保存新增数据的时候，也保存了它们之间的关系。

```java
/**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

```

```java
@Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();// 这是PO，用来对应数据库
        // 重要的工具
        BeanUtils.copyProperties(attrVo, attrEntity);// 把页面VO的值，封装到PO中
        //1、保存基本数据
        this.save(attrEntity);
        //2、保存关联关系
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
        relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
        relationEntity.setAttrId(attrEntity.getAttrId());
		relationDao.insert(relationEntity);
        relationEntity.setAttrSort(0);
    }
```

通过" `BeanUtils.copyProperties(attr,attrEntity);`"能够实现在两个Bean之间属性对拷



问题：现在有两个查询，一个是查询部分，另外一个是查询全部，但是又必须这样来做吗？还是有必要的，但是可以在后台进行设计，两种查询是根据catId是否为零进行区分的。



![image-20200901092947494](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/1e6fe100ff0d0dcfbca25956205f60f6.png)



### 查询参数规格列表功能

![image-20210401112927778](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401112927778.png)



用这种方法，避免笛卡尔乘积（分页数据本来就没多少，循环遍历去查数据库也没有多大压力）

```java
// product/attr/base/list/{catelogId}
    @GetMapping("/base/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
        @PathVariable("catelogId") Long catelogId){

        PageUtils page = attrService.queryBaseAttrPage(params, catelogId);

        return R.ok().put("page", page);
    }
```

**BeanUtils.copyProperties(attr,attrEntity);**
这个是spring的工具类，用于拷贝同名属性

**属性分页**
先用mp的正常分页查出来数据，得到Page对象

然后用PageUtils把分页信息得到，但里面的数据需要替换一下

替换数据是为了解决“不使用联表查询”

查询的key是分类，

```java
    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;


	/**
     *
     * 分页模糊查询  ，比如按分类查属性、按属性类别查属性
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId) {

        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>();
        
        // 如果参数带有分类id，则按分类查询
        if (catelogId != 0L ) {
            wrapper.eq("catelog_id", catelogId);
        }
        // 支持模糊查询，用id或者name查
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        // 正式查询满足条件的属性
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        List<AttrEntity> records = page.getRecords();
        PageUtils pageUtils = new PageUtils(page);

        // 查到属性后还要结合分类名字、分组名字(分类->属性->分组) 封装为AttrRespVo对象
        List<AttrRespVo> attrRespVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);// 先把基本属性拷贝过来

            // 1.设置分类和分组的名字  先获取中间表对象  给attrRespVo 封装分组名字
            // 根据属性id查询关联表，得到其属性分组
            AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if (attrId != null && attrId.getAttrGroupId() != null) {
            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId);
            // 设置属性分组的名字
            attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
            }
            
            // 2.查询分类id 给attrRespVo 封装三级分类名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(attrRespVos);
        return pageUtils;
    }
```



### 规格修改

![image-20210401115739632](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401115739632.png)

```java
public class AttrRespVo extends AttrVo{
    private String catelogName;  // 所属分类的名字 "手机/数码/手机"
    private String groupName;   // 所属分组名字  “主体”

    private Long[] catelogPath;

}
```

AttrController

```java
 /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		//AttrEntity attr = attrService.getById(attrId);
        AttrRespVo respVo = attrService.getAttrInfo(attrId);
        return R.ok().put("attr", respVo);
    }
```



```java
@Autowired
CategoryService categoryService;

。。。。

@Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
       // 先查到attr的详细信息
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);// 先把基本属性拷贝过来

        // 1、设置分组信息
        // 分组关联信息
        AttrAttrgroupRelationEntity attrgroupRelation =
                relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
        if(attrgroupRelation != null){
            respVo.setAttrGroupId((attrgroupRelation.getAttrGroupId()));  // 根据分组关联信息，去得到分组ID
            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
            if (attrGroupEntity != null){
                respVo.setGroupName(attrGroupEntity.getAttrGroupName());
            }

        }
        // 2、设置分类信息（用分类id找出完整的分类路径）
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCateLogPath(catelogId);
        respVo.setCatelogPath(catelogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null){
            respVo.setCatelogName(categoryEntity.getName());
        }
        return respVo;
    }
    
```



![image-20210401122844102](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401122844102.png)

目前回显都正确了，现在去修改后台的update方法



```java
/**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }
```



```java
@Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity entity = new AttrEntity();
        BeanUtils.copyProperties(attr, entity);
        this.baseMapper.updateById(entity);   // 到这里都是基本的修改

        //只有当属性分组不为空时，说明更新的是规则参数，则需要更新关联表
        // 1、修改分组关联
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
        relationEntity.setAttrId(attr.getAttrId());
        relationEntity.setAttrGroupId(attr.getAttrGroupId());
        Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", relationEntity.getAttrId()));
        if (count > 0) {
            relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
        }else{
            relationDao.insert(relationEntity);
        }
    }
```

完成规格参数的查询和修改

![image-20210401130117580](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401130117580.png)

### 销售属性维护

销售属性和上面规格参数很像（attr_type=0销售属性，1=规格参数）

![image-20210401130432471](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401130432471.png)

base是基本属性，sale是销售属性

![image-20210401130219366](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401130219366.png)



```java
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(
            @RequestParam Map<String, Object> params,
            @PathVariable("catelogId") Long catelogId,
            @PathVariable("attrType") String type){

        PageUtils page = attrService.queryBaseAttrPage(params, catelogId, type);

        return R.ok().put("page", page);
    }
```

与规格参数公用了一个接口，根据type不同来区分，区别是规格参数的相关操作需要维护属性与属性分组关系表，而销售属性不需要维护



```java
@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();// 这是PO，用来对应数据库
        // 重要的工具
        BeanUtils.copyProperties(attrVo, attrEntity);// 把页面VO的值，封装到PO中
        //1、保存基本数据
        this.save(attrEntity);
        //2、保存关联关系
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attrVo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            //relationEntity.setAttrSort(0);
            relationDao.insert(relationEntity);
        }
    }

    /**
     *
     * 分页模糊查询  ，比如按分类查属性、按属性类别查属性
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
    // 传入的attrType是"base"或其他，但是数据库存的是 "0"销售 / "1"基本

        // 属性都在pms_attr表中混合着
        QueryWrapper<AttrEntity> wrapper =
                new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type)
                        ?ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                        :ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

//        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type) ? 1 : 0 );

        // 如果参数带有分类id，则按分类查询
        if (catelogId != 0L ) {
            wrapper.eq("catelog_id", catelogId);
        }
        // 支持模糊查询，用id或者name查
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        // 正式查询满足条件的属性
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        List<AttrEntity> records = page.getRecords();
        PageUtils pageUtils = new PageUtils(page);

        // 查到属性后还要结合分类名字、分组名字(分类->属性->分组) 封装为AttrRespVo对象
        List<AttrRespVo> attrRespVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);// 先把基本属性拷贝过来

            // 1.设置分类和分组的名字  先获取中间表对象  给attrRespVo 封装分组名字
            if("base".equalsIgnoreCase(type)){ // 如果是规格参数才查询，或者说销售属性没有属性分组，只有分类
                // 根据属性id查询关联表，得到其属性分组
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId);
                    // 设置属性分组的名字
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
            // 2.查询分类id 给attrRespVo 封装三级分类名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(attrRespVos);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
       // 先查到attr的详细信息
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);// 先把基本属性拷贝过来


        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 1、设置分组信息
            // 分组关联信息
            AttrAttrgroupRelationEntity attrgroupRelation =
                    relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if(attrgroupRelation != null){
                respVo.setAttrGroupId((attrgroupRelation.getAttrGroupId()));  // 根据分组关联信息，去得到分组ID
                //设置分组名
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
                if (attrGroupEntity != null){
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 2、设置分类信息（用分类id找出完整的分类路径）
        Long catelogId = attrEntity.getCatelogId();
        //查询并设置分类路径
        Long[] catelogPath = categoryService.findCateLogPath(catelogId);
        respVo.setCatelogPath(catelogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null){
            respVo.setCatelogName(categoryEntity.getName());
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.baseMapper.updateById(attrEntity);   // 到这里都是基本的修改

        //只有当属性分组不为空时，说明更新的是规则参数，则需要更新关联表
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 1、修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", relationEntity.getAttrId()));
            if (count > 0) {
                relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            }else{
                relationDao.insert(relationEntity);
            }
        }
    }
}
```



### 查询分组关联属性&删除关联

![image-20210401133854032](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401133854032.png)

![image-20210401134014877](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401134014877.png)

```java
@GetMapping ("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        // 获取到当前分组 关联的所有属性
        List<AttrEntity> attrEntities= attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", attrEntities);
    }
```



```java
/**
     * 查询分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId)
        );
        List<AttrEntity> attrEntities = relationEntities.stream().map((entity) -> {
            AttrEntity attrEntity = baseMapper.selectById(entity.getAttrId());
            return attrEntity;
        }).collect(Collectors.toList());
        return attrEntities;
    }
```





![image-20210401135725492](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401135725492.png)

成功显示。去做移除功能

![image-20210401135805198](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401135805198.png)

​	AttrGroupController

```java
///product/attrgroup/attr/relation/delete
    @PostMapping("attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrService.deleteRelation(vos);
        return R.ok();
    }
```

AttrServiceImpl

```java
// 批量删除
    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.stream(vos).map(item -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        //delete from pms_attr_attrgroup_relation where (attr_id=? and attr_group_id=?) or (attr_id=? and attr_group_id=?)
        relationDao.deleteBatchRelation(relationEntities);

    }
```



AttrAttrgroupRelationDao.xml

```java
    <delete id="deleteBatchRelation">
        delete from pms_attr_attrgroup_relation where
        <foreach collection="entities" item="item" separator=" or ">
            (attr_id=#{item.attrId} and attr_group_id=#{item.attrGroupId})
        </foreach>
    </delete>
```

测试，删除成功



### 查询分组未关联属性

```
AttrServiceImpl中的getRelationAttr写的不同，有错去排查
```

![image-20210401143118849](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401143118849.png)



难点：查出那些分组是能关联进来的（本分类下，没有被其他分组关联的属性）

```java
    @GetMapping ("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params) {
        // 返回分页数据
        PageUtils page = attrService.getNoRelationAttr(params, attrgroupId);
        return R.ok().put("page", page);
    }
```



```java
/**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 1、当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        // 2、当前分组只能关联别的分组没有引用的属性
        // 2.1）、当前分类下的其他分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId);
        List<Long> collect = group.stream().map(item -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        // 2.2）、这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        List<Long> attrIds = groupId.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        // 2.3）、从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());

        if(attrIds != null && attrIds.size() > 0){
            wrapper.notIn("attr_id",attrIds);
        }
        //模糊搜索条件
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) ->{
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        return new PageUtils(page);

    }
```





![image-20210401151908041](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401151908041.png)



### 新增分组与属性关联

上图点击确认新增后，后台的操作：

![image-20210401152233004](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401152233004.png)

AttrGroupController

```java
 // 新增关联关系
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){
        relationService.saveBatch(vos);
        return R.ok();
    }
```

AttrAttrgroupRelationServiceImpl

```java
 @Override
    public void saveBatch(List<AttrGroupRelationVo> vos) {
        List<AttrAttrgroupRelationEntity> collect =  vos.stream().map(item->{
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }
```



# 22、新增商品 

### 调试会员等级相关接口



![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/20210218072348.png)

- 基本信息
- 规则参数
  - 前两步都是spu
- 销售属性
- SKU信息
  - 根据上一步选择的录入价格、标题
- 保存完成

获取所有会员等级：/member/memberlevel/list
API：https://easydoc.xyz/doc/75716633/ZUqEdvA4/jCFganpf

开启编写member项目

在“mall-gateway”中修改“”文件，添加对于member的路由

```properties
        - id: member_route
          uri: lb://mall-member
          predicates:
            - Path=/api/member/**
          filters:
            - RewritePath=/api/(?<segment>.*),/$\{segment}
```

在“mall-member”中，创建“bootstrap.properties”文件，内容如下：

```properties
spring.cloud.nacos.config.name=gulimall-member
spring.cloud.nacos.config.server-addr=192.168.137.14:8848
spring.cloud.nacos.config.namespace=795521fa-77ef-411e-a8d8-0889fdfe6964
spring.cloud.nacos.config.extension-configs[0].data-id=mall-member.yml
spring.cloud.nacos.config.extension-configs[0].group=DEFAULT_GROUP
spring.cloud.nacos.config.extension-configs[0].refresh=true
```



![image-20210401163615991](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401163615991.png)



### 获取分类关联的品牌

获取分类关联的品牌：/product/categorybrandrelation/brands/list

API：https://easydoc.xyz/doc/75716633/ZUqEdvA4/HgVjlzWV

- 查询所有会员等级
- 查询选中分类 关联的 品牌
- 查询分类下的所有属性分组list（从attr-group表中用分类id查到符合的属性分组），还有属性分组中的所有属性list

![image-20210401165405051](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401165405051.png)



CategoryBrandRelationController

```java
/**
     * product/categorybrandrelation/brands/list
     *
     * 1、Controller：处理请求，接收和校验数据
     * 2、Service接收controller传来的数据，进行业务处理
     * 3、Controller接收Service处理完的数据，封装页面指定的vo
     *
     * @param catId
     * @return
     */
    @GetMapping("/brands/list")
    public R relationBrandList(@RequestParam(value = "catId", required = true) Long catId) {
        List<BrandEntity> vos = categoryBrandRelationService.getBrandsByCatId(catId);

        // 封装数据
        List<BrandVo> collect = vos.stream().map(item -> {
            BrandVo brandVo = new BrandVo();
            brandVo.setBrandId(item.getBrandId());
            brandVo.setBrandName(item.getName());

            return brandVo;
        }).collect(Collectors.toList());

        return R.ok().put("data", collect);
    }
```



![image-20210401172118537](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401172118537.png)

CategoryBrandRelationServiceImpl (注入service比注入dao有更丰富的业务逻辑)

```java
    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        // 这么长是为了复用代码

        // 只需要查关联表就可以了
        List<CategoryBrandRelationEntity> catelogId = relationDao.selectList(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
        // 把品牌详细信息都获取出来
        List<BrandEntity> collect = catelogId.stream().map(item -> {
            Long brandId = item.getBrandId();
            BrandEntity byId = brandService.getById(brandId);
            return byId;
        }).collect(Collectors.toList());

        return collect;
    }
```

![image-20210401172855269](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401172855269.png)







### 获取分类下所有分组以及属性

![image-20210401173056329](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401173056329.png)

![image-20210401175231323](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401175231323.png)

AttrGroupController

```java
/**
     * 获取当前分类下的所有属性分组，而且还要带上它们所有的属性
     * /product/attrgroup/{catelogId}/withattr
     */
    @GetMapping("/{catelogId}/withattr")
    public R getAttrgroupWithAttrs(@PathVariable("catelogId") Long catelogId) {
        // 1、查出当前分类下的所有属性分组(用三级id来查出）
        // 2、查出每个属性分组的所有属性
        List<AttrGroupWithAttrsVo> vos = attrGroupService.getAttrgroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data", vos);
    }
```

AttrGroupServiceImpl

```java
/**
     * 根据分类id查出所有的分组以及这些组里面的属性（用作规格参数）
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrgroupWithAttrsByCatelogId(Long catelogId) {

        // 1. 查询分组信息（要用关联关系来查询）
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        // 2、查询所有属性（基于分组）
        List<AttrGroupWithAttrsVo> collect = groupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrsVO = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, attrsVO);

            // 查询这个分组下的所有属性(按照分组的id）
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVO.getAttrGroupId());
            attrsVO.setAttrs(attrs);

            return attrsVO;
        }).collect(Collectors.toList());
        return collect;
    }
```

![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/20210218140943.png)

![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/20210218140953.png)



![img](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/20210218141001.png)



### 商品新增vo抽取

![image-20210401182906340](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210401182906340.png)

逆向生成VO，放入vo目录。把里面get/setting用lombok替换，同时修改一些type(比如从string改为BigDecimal)



### 商品新增业务流程分析

要完善保存功能

去SpuInfoController

```java
/**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:spuinfo:save")
    public R save(@RequestBody SpuSaveVo vo){
		spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }
```

SpuInfoServiceImpl大致框架

```java
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    /**
     * 保存所有数据 [33kb左右]
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1.保存spu基本信息 pms_sku_info
            // 插入后id自动返回注入
            // 此处有分布式id的问题，所以要加事务

        // 2.保存spu的表述图片  pms_spu_info_desc
        // 3.保存spu的图片集  pms_sku_images
            // 先获取所有图片
            // 保存图片的时候 并且保存这个是那个spu的图片

        // 4.保存spu的规格参数  pms_product_attr_value

        // 5.保存spu的积分信息 mall_sms->sms_spu_bounds
        // 6.保存当前spu对应所有sku信息
        //      6.1) sku的基本信息：pms_sku_info
        //      6.2）sku的图片信息：pms_sku_images
        //      6.3) sku的销售属性: pms_sku_sale_attr_value
        //      6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)


        skus.forEach(item -> {
            // 2).基本信息的保存 pms_sku_info
            // skuName 、price、skuTitle、skuSubtitle 这些属性需要手动保存

            // 设置spu的品牌id

            // 3).保存sku的图片信息  pms_sku_images
            // sku保存完毕 自增主键就出来了 收集所有图片

            // 4).sku的销售属性  pms_sku_sale_attr_value
            // 5.) sku的优惠、满减、会员价格等信息  [跨库]
        });

    }

}
```

**商品优惠db表**

* SkuLadderEntity买几件打几折
  * 买几件
  * 打几折
  * 是否参与其他优惠
  * skuId
* SkuFullReductionEntity满多少减多少
  * 满多少
  * 减多少
  * 是否参与其他优惠
  * skuId
* MemberPriceEntity会员价格
  * 会员等级id：memberLevelId
  * MemberPriceEntity
  * 会员价格memberPrice
  * 是否参与其他优惠
  * skuId
    

### 保存SPU基本信息

先创建到接口上，再添加实现



SpuImagesServiceImpl

```java
@Override
    public void saveImages(Long id, List<String> images) {
        if (images == null || images.size() == 0){

        }else{
            List<SpuImagesEntity> collect = images.stream().map(img->{
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(id);
                spuImagesEntity.setImgUrl(img);

                return spuImagesEntity;
            }).collect(Collectors.toList());

            this.saveBatch(collect);    // 最后批量保存
        }
    }
```

step1-4

```java
/**
     * 保存所有数据 [33kb左右]
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1.保存spu基本信息 pms_sku_info
        // 插入后id自动返回注入
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);
        // 此处有分布式id的问题，所以要加事务

        // 2.保存spu的表述图片  pms_spu_info_desc
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);


        // 3.保存spu的图片集  pms_sku_images
        // 先获取所有图片
        // 保存图片的时候 并且保存这个是那个spu的图片
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);

        // 4.保存spu的规格参数  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
            // 遍历这些属性
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity entity = new ProductAttrValueEntity();
            entity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            entity.setAttrName(byId.getAttrName());
            entity.setAttrValue(attr.getAttrValues());
            entity.setQuickShow(attr.getShowDesc());
            entity.setSpuId(infoEntity.getId());
            return entity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);
        
        // 5.保存spu的积分信息 mall_sms->sms_spu_bounds
        // 6.保存当前spu对应所有sku信息
        //      6.1) sku的基本信息：pms_sku_info
        //      6.2）sku的图片信息：pms_sku_images
        //      6.3) sku的销售属性: pms_sku_sale_attr_value
        //      6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)


        skus.forEach(item -> {
            // 2).基本信息的保存 pms_sku_info
            // skuName 、price、skuTitle、skuSubtitle 这些属性需要手动保存

            // 设置spu的品牌id

            // 3).保存sku的图片信息  pms_sku_images
            // sku保存完毕 自增主键就出来了 收集所有图片

            // 4).sku的销售属性  pms_sku_sale_attr_value
            // 5.) sku的优惠、满减、会员价格等信息  [跨库]
        });

    }
```



### 保存SKU基本信息

```java
@Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1.保存spu基本信息 pms_sku_info
        // 插入后id自动返回注入
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);
        // 此处有分布式id的问题，所以要加事务

        // 2.保存spu的表述图片  pms_spu_info_desc
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);


        // 3.保存spu的图片集  pms_sku_images
        // 先获取所有图片
        // 保存图片的时候 并且保存这个是那个spu的图片
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);

        // 4.保存spu的规格参数  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
            // 遍历这些属性
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity entity = new ProductAttrValueEntity();
            entity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            entity.setAttrName(byId.getAttrName());
            entity.setAttrValue(attr.getAttrValues());
            entity.setQuickShow(attr.getShowDesc());
            entity.setSpuId(infoEntity.getId());
            return entity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        // 5.保存spu的积分信息 mall_sms->sms_spu_bounds
        // TODO 积分信息

        // 6.保存当前spu对应所有sku信息
        //      6.1) sku的基本信息：pms_sku_info
        //      6.2）sku的图片信息：pms_sku_images
        //      6.3) sku的销售属性: pms_sku_sale_attr_value
        //      6.4) sku的优惠、满减、会员价格等信息  [跨库]
        //              (mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(item -> {
                // 6.1) sku的基本信息：pms_sku_info
                String defaultImg="";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                // skuName 、price、skuTitle、skuSubtitle 这些属性需要手动保存
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSkuDefaultImg(defaultImg);

                skuInfoService.saveSkuInfo(skuInfoEntity);


                // 6.2）sku的图片信息：pms_sku_images
                // sku保存完毕 自增主键就出来了 收集所有图片
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();

                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return  skuImagesEntity;
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(imagesEntities);


                // 6.3) sku的销售属性: pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(at -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(at, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuInfoEntity.getSkuId());
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                // 6.4) sku的优惠、满减、会员价格等信息  [跨库]

            }
        }

    }
```



### 远程调用服务保存优惠等信息

 远程调用服务前提：1.本服务首先必须在注册中心中 2.一定要开启远程调用功能@EnableFeignClients

product要调用，所以先建立feign目录和文件

**第一个远程调用：**

第一个功能：保存商品的spu信息（soupon有个controller专门负责处理储存bouond信息）所以product的feign想要调用

CouponFeignService接口(这个需要根据coupon里面想要调用的controller来写，这里是SpuBoundsController的save) 要跟远程的接口保持完整一致的签名

```java
@FeignClient("mall-coupon")
public interface CouponFeignService {
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("/coupon/skufullreduction/saveInfo")
    R saveSkuReductionTo(@RequestBody SkuReductionTo skuReductionTo);

}
```



TO模型：在微服务中，A给B传，会先变成JSON（两个服务需要有相同的json转换配置，不然会转换失败）

在common新增一个To的包，一个叫SpuBoundTo ，一个叫SkuReductionTo

```java
@Data
public class SpuBoundTo {
    private Long spuId;

    private BigDecimal buyBounds;

    private BigDecimal growBounds;
}

```

```java
@Data
public class SkuReductionTo {

    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}

```



coupon里面的SpuBoundsController

```java
/**
     * 保存
     */
    @PostMapping("/save")
    //@RequiresPermissions("coupon:spubounds:save")
    public R save(@RequestBody SpuBoundsEntity spuBounds){
		spuBoundsService.save(spuBounds);

        return R.ok();
    }
 
 @RequestBody 就是将传过来的json转成 数据
```



在CouponFeignService中注意：

1. CouponFeignService,saveSpuBounds(spuBoundTo);

   1. @RequestBody将这个对象转为json
   2. 找到mall-coupon服务，给/coupon/spubounds/save发送请求。
      * 将上一步转的json放在请求体位置，发送请求；
   3. 对方服务收到请求，请求体里有json数据。
      * (@RequestBody SpuBoundEntity spuBounds)：将请求体的json转为SpuBoundsEntity；

   只要json数据模型是兼容的，双方服务无需使用同一个to

   



**第二个远程调用：product调用coupon/skufullreduction/saveInfo**

现成写一个：

```java
@PostMapping("/saveInfo")
    public R saveInfo(@RequestBody SkuReductionTo skuReductionTo) {
        skuFullReductionService.saveSkuReductionTo(skuReductionTo);
        return R.ok();
    }
```

SkuFullReductionServiceImpl

```java
@Override
    public void saveSkuReductionTo(SkuReductionTo skuReductionTo) {
        // 6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)

        // 1.sms_sku_ladder 阶梯式打折（满几件打几折）
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
//        BeanUtils.copyProperties(skuReductionTo,skuLadderEntity);
        skuLadderService.save(skuLadderEntity);

        // 2. sms-sku_full_reduction 满多少减多少
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo,reductionEntity);
        this.save(reductionEntity);

        // 3.sms_member_price 会员价格
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();

        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity priceEntity = new MemberPriceEntity();
            priceEntity.setSkuId(skuReductionTo.getSkuId());
            priceEntity.setMemberLevelId(item.getId());
            priceEntity.setMemberLevelName(item.getName());
            priceEntity.setMemberPrice(item.getPrice());
            priceEntity.setAddOther(1);
            return  priceEntity;
        }).collect(Collectors.toList());
    }
```



```java
/**
     * 保存所有数据 [33kb左右]
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1.保存spu基本信息 pms_sku_info
        // 插入后id自动返回注入
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);
        // 此处有分布式id的问题，所以要加事务

        // 2.保存spu的表述图片  pms_spu_info_desc
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);


        // 3.保存spu的图片集  pms_sku_images
        // 先获取所有图片
        // 保存图片的时候 并且保存这个是那个spu的图片
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);

        // 4.保存spu的规格参数  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
            // 遍历这些属性
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity entity = new ProductAttrValueEntity();
            entity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            entity.setAttrName(byId.getAttrName());
            entity.setAttrValue(attr.getAttrValues());
            entity.setQuickShow(attr.getShowDesc());
            entity.setSpuId(infoEntity.getId());
            return entity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        // 5.保存spu的积分信息 mall_sms->sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }


        // 6.保存当前spu对应所有sku信息
        //      6.1) sku的基本信息：pms_sku_info
        //      6.2）sku的图片信息：pms_sku_images
        //      6.3) sku的销售属性: pms_sku_sale_attr_value
        //      6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(item -> {
                // 6.1) sku的基本信息：pms_sku_info
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                // skuName 、price、skuTitle、skuSubtitle 这些属性需要手动保存
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSkuDefaultImg(defaultImg);

                skuInfoService.saveSkuInfo(skuInfoEntity);


                // 6.2）sku的图片信息：pms_sku_images
                // sku保存完毕 自增主键就出来了 收集所有图片
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();

                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(imagesEntities);


                // 6.3) sku的销售属性: pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(at -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(at, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuInfoEntity.getSkuId());
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                // 6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                //if(skuReductionTo.getFullCount() >0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                R r2 = couponFeignService.saveSkuReductionTo(skuReductionTo);
                //}
                if (r2.getCode() != 0) {
                    log.error("远程保存sku优惠信息失败");
                }

            
            });
        }

    }
```



#### 商品保存debug完成

问题：SpuInfoDescEntity的spuId不是自增的，添加@TableId(type = IdType.INPUT)

open-feign问题，我的版本应该还是用的rebbon，member远程调用没错，但是product报错







# TODO

Product Management: 

1. SPU检索
2. SKU检索
3. SPU规格维护

Warehouse Management & Order Management

1. 整合ware服务&获取仓库列表
2. 查询库存&创建采购需求
3. 合并采购需求
4. 领取采购单
5. 完成采购








