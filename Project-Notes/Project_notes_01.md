

# 1、项目相关基础

mybatis-plus
springcloud

# 2、知识介绍-微服务架构图和项目描述



集群是个物理形态，分布式是个工作方式。

远程调用：在分布式系统中，各个服务可能处于不同主机，但是服务之间不可避免的相互调用，我们成为远程调用。

Springcloud中使用HTTP+JSON的方式完成远程调用。

**服务注册/发现&注册中心**

A服务调用B服务，A服务并不知道B服务当前在哪几台服务器有，那些是正常的，那些服务已经下线。解决这个问题可以引入注册中心。

配置中心用来几种管理微服务的配置信息。

服务熔断&服务降级

在微服务架构中，微服务之间通过网络进行通信，存在相互依赖，当其中一个服务不可用时，有可能会造成雪崩效应。要防止这样的情况，必须要有容错机制来保护服务。

**rpc远程调用情景：订单服务 --> 商品服务 --> 库存服务**

库存服务出现故障导致响应慢，导致商品服务需要等待，可能等到10s后库存服务才能响应。库存服务的不可用导致商品服务阻塞，商品服务等的期间，订单服务也处于阻塞。一个服务不可用导致整个服务链都阻塞。如果是高并发，第一个请求调用后阻塞10s得不到结果，第二个请求直接阻塞10s。更多的请求进来导致请求积压，全部阻塞，最终服务器的资源耗尽。导致雪崩

解决方案：

1. 服务熔断

   指定超时时间，库存服务3s没有响应就超时，如果经常失败，比如10s内100个请求都失败了。开启断路保护机制，下一次请求进来不调用库存服务了，因为上一次100%错误都出现了，我们直接在此中断，商品服务直接返回，返回一些默认数据或者null，而不调用库存服务了，这样就不会导致请求积压。

   * 设置服务的超时，当被调用的服务经常失败到达某个阈值，我们可以开启断路保护机制，后来的请求不再去调用这个服务。本地直接返回默认的数据

2. 服务降级
   
   * 在运维期间，当系统处于高峰期，系统资源紧张，我们可以让非核心业务降级运行。降级：某些服务不处理，或者处理简单【抛异常、返回NULL、调用Mock数据、调用Fallback处理逻辑】

**API网关**

客户端发送请求到服务器路途中，设置一个网关，请求都先到达网关，网关对请求进行统一认证（合法非法）和处理等操作。他是安检。

在微服务架构中，API gateway作为整体架构的重要组件，它抽象了微服务中都需要的公共功能，同时提供了客户端负载均衡，服务自动熔断，灰度发布，统一认证，限流流控，日志统计等丰富的功能，帮助我们解决很多API管理难题。



前后分离开发，分为内网部署和外网部署
**外网**是面向公众访问的，部署前端项目，可以有手机APP，电脑网页；
**内网**部署的是后端集群，前端在页面上操作发送请求到后端，在这途中会经过Nginx集群，**Nginx把请求转交给API网关**(springcloud gateway)（网关可以根据当前请求动态地路由到指定的服务，看当前请求是想调用商品服务还是购物车服务还是检索），从路由过来如果请求很多，可以负载均衡地调用商品服务器中一台（商品服务复制了多份），当商品服务器出现问题也可以在网关层面对服务进行熔断或降级（使用阿里的sentinel组件），网关还有其他的功能如认证授权、限流（只放行部分到服务器）等。

到达服务器后进行处理（springboot为微服务），服务与服务可能会相互调用（使用feign组件），有些请求可能经过登录才能进行（基于OAuth2.0的认证中心。安全和权限使用springSecurity控制）

服务可能保存了一些数据或者需要使用缓存，这里将使用redis集群（分片+哨兵集群）。持久化使用mysql，读写分离和分库分表。

服务和服务之间会使用消息队列（RabbitMQ），来完成异步解耦，分布式事务的一致性。有些服务可能需要全文检索，检索商品信息，使用ElaticSearch。

服务可能需要存取数据，使用阿里云的对象存储服务OSS。

项目上线后为了快速定位问题，使用ELK对日志进行处理，使用LogStash收集业务里的各种日志，把日志存储到ES中，用Kibana可视化页面从ES中检索出相关信息，帮助我们快速定位问题所在。

在分布式系统中，由于我们每个服务都可能部署在很多台机器，服务和服务可能相互调用，就得知道彼此都在哪里，所以需要将所有服务都注册到注册中心。服务从注册中心发现其他服务所在位置（使用阿里Nacos作为注册中心）。

每个服务的配置众多，为了实现改一处配置相同配置就同步更改，就需要配置中心，也使用阿里的Nacos，服务从配置中心中动态取配置。

服务追踪，追踪服务调用链哪里出现问题，使用springcloud提供的Sleuth、Zipkin、Metrics，把每个服务的信息交给开源的Prometheus进行聚合分析，再由Grafana进行可视化展示，提供Prometheus提供的AlterManager实时得到服务的告警信息，以短信/邮件的方式告知服务开发人员。

还提供了持续集成和持续部署。项目发布起来后，因为微服务众多，每一个都打包部署到服务器太麻烦，有了持续集成后开发人员可以将修改后的代码提交到github，运维人员可以通过自动化工具Jenkins Pipeline将github中获取的代码打包成docker镜像，最终是由k8s集成docker服务，将服务以docker容器的方式运行。

**微服务划分图**
![image-20210407123302097](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210407123302097.png)



反映了需要创建的微服务以及相关技术。

前后分离开发。前端项目分为admin-vue（工作人员使用的后台管理系统）、shop-vue（面向公众访问的web网站）、app（公众）、小程序（公众）

* 商品服务Product：商品的增删改查、商品的上下架、商品详情
* 支付服务Payment
* 优惠服务
* 用户服务Member：用户的个人中心、收货地址
* 仓储服务Ware：商品的库存
* 秒杀服务Coupon：
* 订单服务Order：订单增删改查
* 检索服务：商品的检索ES
* 中央认证服务：登录、注册、单点登录、社交登录
* 购物车服务：
* 后台管理系统：添加优惠信息等
  

# 3、linux环境搭建

#### 安装vagrant

目的：用vagrant给virtualbox快速创建虚拟机。
普通安装linux虚拟机太麻烦，可以利用vagrant可以帮助我们快速地创建一个虚拟机。主要装了vitualbox，vagrant可以帮助我们快速创建出一个虚拟机。他有一个镜像仓库。
去https://www.vagrantup.com/ 下载vagrant安装，安装后重启系统。cmd中输入vagrant有版本代表成功了。

安装Centos7

```
vagrant init centos/7 初始化一个centos7系统。
		（注意这个命令在哪个目录下执行的，他的Vagrantfile就生成在哪里）
vagrant up 启动虚拟机环境
vagrant ssh 连接虚拟机
vagrant reload 重启
```

启动完成标志![image-20210328063850153](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328063850153.png)



#### 网络地址转换

不过他使用的网络方式是网络地址转换NAT（端口转发），如果其他主机要访问虚拟机，必须由windows端口如3333转发给虚拟机端口如3306。这样每在linux里安一个软件都要进行端口映射，不方便，（也可以在virualBox里挨个设置）。我们想要给虚拟机一个固定的ip地址，windows和虚拟机可以互相ping通。

![image-20210328064909213](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328064909213.png)

方式1是在虚拟机中配置静态ip。

方式2，也可以更改Vagrantfile更改虚拟机ip，修改其中的config.vm.network "private_network",ip:"192.168.56.10"，这个ip需要在windows的ipconfig中查到vitualbox的虚拟网卡ip，然后更改下最后一个数字就行（不能是1，1是我们的主机）。配置完后vagrant reload重启虚拟机。在虚拟机中ip addr就可以查看到地址了。互相ping也能ping通。


配置网络信息，打开"Vagrantfile"文件：

```
config.vm.network "private_network", ip: "192.168.56.10"
```

修改完成后，重启启动vagrant

```
vagrant reload
```

vagrant ssh

[vagrant@localhost ~]$ ip addr

```
link/ether 08:00:27:fc:57:2a brd ff:ff:ff:ff:ff:ff
    inet 192.168.56.10/24 brd 192.168.56.255 scope global noprefixroute eth1
       valid_lft forever preferred_lft forever
    inet6 fe80::a00:27ff:fefc:572a/64 scope link
       valid_lft forever preferred_lft forever
```

虚拟机地址：192.168.56.10
本机地址 IPv4 Address. . . . . . . . . . . : 192.168.0.200
从本地ping虚拟机

```
Ping statistics for 192.168.56.10:
    Packets: Sent = 4, Received = 4, Lost = 0 (0% loss),
Approximate round trip times in milli-seconds:
    Minimum = 0ms, Maximum = 0ms, Average = 0ms
```

反过来也能ping得通

#### 虚拟机安装docker

```
#卸载系统之前的docker 
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
                  
                  
sudo yum install -y yum-utils

# 配置镜像
sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
    
sudo yum install docker-ce docker-ce-cli containerd.io

#启动docker
sudo systemctl start docker

# 设置开机自启动
sudo systemctl enable docker

#检查版本
docker -v  			
        #Docker version 20.10.5, build 55c4c88
sudo docker images					
        #REPOSITORY   TAG       IMAGE ID   CREATED   SIZE

```

##### systemctl命令



#### docker中安装mysql

用docker安装上mysql，去docker仓库里搜索mysql

```
#下载镜像
sudo docker pull mysql:5.7

# --name指定容器名字 -v目录挂载 -p指定端口映射  -e设置mysql参数 -d后台运行
sudo docker run -p 3306:3306 --name mysql \
-v /mydata/mysql/log:/var/log/mysql \
-v /mydata/mysql/data:/var/lib/mysql \
-v /mydata/mysql/conf:/etc/mysql \
-e MYSQL_ROOT_PASSWORD=root \
-d mysql:5.7

# 密码为vagrant，这样就可以不写sudo了
su root 

#查看docker目前运行中的容器
[root@localhost vagrant]# docker ps
CONTAINER ID   IMAGE       COMMAND                  CREATED          STATUS          PORTS                               NAMES
ae0fd58cf949   mysql:5.7   "docker-entrypoint.s…"   56 seconds ago   Up 54 seconds   0.0.0.0:3306->3306/tcp, 33060/tcp   mysql
```

![image-20210328092021469](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328092021469.png)

文件挂载：把容器文件（mysql的日志log和配置信息lib和数据文件etc）挂载到linux主机文件目录上（类似于快捷方式）

```
#进入指定容器
docker exec -it mysql bin/bash

#可以看一下目录
root@ae0fd58cf949:/# ls /
bin   docker-entrypoint-initdb.d  home   media  proc  sbin  tmp
boot  entrypoint.sh               lib    mnt    root  srv   usr
dev   etc                         lib64  opt    run   sys   var


root@ae0fd58cf949:/# whereis mysql
mysql: /usr/bin/mysql /usr/lib/mysql /etc/mysql /usr/share/mysql

#退出容器
exit;

#尝试在容器外，linux中进入容器内部的mysql文件
[root@localhost vagrant]# cd /mydata/mysql/
[root@localhost mysql]# ls
conf  data  log
```

因为有目录映射，所以我们可以直接在镜像外执行，添加mysql配置文件

```
vi /mydata/mysql/conf/my.conf 

[client]
default-character-set=utf8
[mysql]
default-character-set=utf8
[mysqld]
init_connect='SET collation_connection = utf8_unicode_ci'
init_connect='SET NAMES utf8'
character-set-server=utf8
collation-server=utf8_unicode_ci
skip-character-set-client-handshake
skip-name-resolve

#保存
docker restart mysql
```



#### docker中安装redis

如果直接挂载的话docker会以为挂载的是一个目录，所以我们先创建一个文件然后再挂载，在虚拟机中。

```
docker pull redis

# 在虚拟机中
mkdir -p /mydata/redis/conf
touch /mydata/redis/conf/redis.conf

# 端口映射和文件挂载
docker run -p 6379:6379 --name redis \
-v /mydata/redis/data:/data \
-v /mydata/redis/conf/redis.conf:/etc/redis/redis.conf \
-d redis redis-server /etc/redis/redis.conf

# 直接进去redis客户端
docker exec -it redis redis-cli
```

默认是不持久化的，要在redis.conf配置文件中输入appendonly yes，就可以aof持久化了。

```
vim /mydata/redis/conf/redis.conf
# 插入下面内容
appendonly yes
保存

#修改完后
docker restart redis
docker exec -it redis redis-cli
```



##### Docker自启动命令

设置redis容器在docker启动的时候启动

```
docker update redis --restart=always
```



# 4、开发环境和准备

**maven**

在settings中配置阿里云镜像，配置jdk1.8

IDEA安装插件lombok，mybatis等



**vsCode设置**
下载vsCode用于前端管理系统。在vsCode里安装插件。

Auto Close Tag
Auto Rename Tag
Chinese
ESlint
HTML CSS Support
HTML Snippets
JavaScript ES6
Live Server
open in brower
Vetur



依次创建出以下服务（都导入web和openFeign -> rpc调用）

* 商品服务product
* 存储服务ware
* 订单服务order
* 优惠券服务coupon
* 用户服务member

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.kexin.mall</groupId>
    <artifactId>mall</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mall</name>
    <description>Integration service</description>

    <packaging>pom</packaging>

    <modules>
        <module>mall-coupon</module>
        <module>mall-member</module>
        <module>mall-order</module>
        <module>mall-product</module>
        <module>mall-ware</module>
    </modules>

</project>

```



修改总项目的`.gitignore`，把小项目里的垃圾文件在提交的时候忽略掉

```
**/mvnw
**/mvnw.cmd
**/.mvn
**/target/
.idea
**/.gitignore
```





# 5、数据库准备

我自己修改过的sql上传到了：https://github.com/FermHan/gulimall

打开sqlyog软件，链接（linux里的mysql docker镜像）192.168.56.10，账号密码root。

> 注意重启虚拟机和docker后里面的容器就关了。

```
sudo docker ps
sudo docker ps -a
# 这两个命令的差别就是后者会显示  【已创建但没有启动的容器】

# 我们接下来设置我们要用的容器每次都是自动启动
sudo docker update redis --restart=always
sudo docker update mysql --restart=always

# 如果不配置上面的内容的话，我们也可以选择手动启动
sudo docker start mysql
sudo docker start redis

# 如果要进入已启动的容器
sudo docker exec -it mysql /bin/bash
# /bin/bash就是进入一般的命令行，如果改成redis就是进入了redis

```



然后接着去sqlyog执行我们的操作，在左侧root上右键建立数据库：字符集选utf8mb4，他能兼容utf8且能解决一些乱码的问题。分别建立了下面数据库

```
gulimall-oms
gulimall-pms
gulimall-sms
gulimall-ums
gulimall-wms
```

然后打开对应的sql在对应的数据库中执行。依次执行。(注意sql文件里没有建库语句)



# 6、人人项目npm

在Github上搜索renren-fast（后端）、renren-fast-vue（前端）项目。

```
git clone https://gitee.com/renrenio/renren-fast.git
git clone https://gitee.com/renrenio/renren-fast-vue.git
```

下载到了桌面，我们把renren-fast移动到我们的项目文件夹（删掉.git文件），而renren-vue是用VSCode打开的（后面再弄）。

#### renren-fast后端

在IDEA项目里的pom.xml添加一个renrnen-fast

```xml
    <modules>
        <module>mall-coupon</module>
        <module>mall-member</module>
        <module>mall-order</module>
        <module>mall-product</module>
        <module>mall-ware</module>
        
        <module>renren-fast</module>
    </modules>
```

然后打开`renren-fast/db/mysql.sql`，复制全部，在sqlyog中创建库`guli-admin`，粘贴刚才的内容执行。

然后修改项目里renren-fast中的application.yml，修改`application-dev.yml`中的数据库的url，通常把localhost修改为192.168.56.10即可。然后修改后面对应的数据库名称

```properties
url: jdbc:mysql://192.168.56.10:3306/gulimall_admin?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
username: root
password: root
```

然后运行该java项目下的RenrenApplication

浏览器输入http://localhost:8080/renren-fast/ 得到{“msg”:“invalid token”,“code”:401}就代表无误



#### renren-vue前端

用VSCode打开renren-fast-vue

`NPM`是随同`NodeJS`一起安装的包管理工具。JavaScript-NPM类似于java-Maven。
命令行输入`node -v` 检查配置好了，然后去VScode的项目终端中输入 `npm install`，是要去拉取依赖（package.json类似于pom.xml的dependency）



**Tips**（如果node版本太新，无法兼容）：

把node降级到10.16.3：先安装nvm-windows

```
nvm install v10.16.3
nvm use 10.16.3
```

### 

# 7、人人项目-逆向工程

逆向工程搭建

```
git clone https://gitee.com/renrenio/renren-generator.git
```

下载到桌面后，同样把里面的.git文件删除，然后移动到我们IDEA项目目录中，同样配置好pom.xml

```
<module>renren-generator</module>
```



#### 操作商品product模块

修改`application.yml`

```properties
url: jdbc:mysql://192.168.56.10:3306/gulimall_pms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
username: root
password: root
```

然后修改`generator.properties`

```properties
# 主目录
mainPath=com.kexin
#包名
package=com.kexin.mall
#模块名
moduleName=product
#作者
author=kexinwen
#email
email=kexinwen.ca@gmail.com
#表前缀(类名不会包含表前缀) 
#pms数据库中的表的前缀都pms
#如果写了表前缀，每一张表对于的javaBean就不会添加前缀了
tablePrefix=pms_
```

运行RenrenApplication。如果启动不成功，修改application中是port为801。访问http://localhost:801/

在网页上下方点击每页显示50个（pms库中的表），以让全部都显示，然后点击全部，点击生成代码。下载压缩包，解压压缩包，把main放到gulimall-product的同级目录下。
![image-20210328163054801](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328163054801.png)

#### 添加common模块

在项目上右击new modules— maven—然后在name上输入mall-common。

在pom.xml中也自动添加了<module>mall-common</module>

在common项目的pom.xml中添加依赖

```xml
<!-- mybatisPLUS-->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.3.2</version>
</dependency>
<!--简化实体类，用@Data代替getset方法-->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.8</version>
</dependency>
<!-- httpcomponent包。发送http请求 -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpcore</artifactId>
    <version>4.4.13</version>
</dependency>
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
    <version>2.6</version>
</dependency>
```

每个微服务里公共的类和依赖放到common里。

然后在product项目中的pom.xml中加入下面内容，作为common的子项目

```xml
<dependency>
    <groupId>com.kexin.mall</groupId>
    <artifactId>mall-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

> maven依赖的问题：
>
> - `<dependency>`代表本项目依赖，子项目也依赖
> - 如果有个`<optional>`标签，代表本项目依赖，但是子项目不依赖



* 复制renren-fast中的xss包粘贴到common的com.kexin.common目录下。
* 注释掉product项目下类中的`//import org.apache.shiro.authz.annotation.RequiresPermissions;`，他是shiro的东西
* 注释renren-generator\src\main\resources\template/Controller中所有的`@RequiresPermissions`，因为是shiro的。`## import org.apache.shiro.authz.annotation.RequiresPermissions;`
  

总之什么报错就去fast里面找。重启逆向工程。重新在页面上得到压缩包。重新解压出来，不过只把里面的controller复制粘贴到product项目对应的目录就行。



#### 测试

测试与整合商品服务里的mybatisplus

https://mp.baomidou.com/guide/quick-start.html#配置

在common的pom.xml中导入

```xml
<!-- 数据库驱动 https://mvnrepository.com/artifact/mysql/mysql-connector-java -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.17</version>
</dependency>
<!--tomcat里一般都带-->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>2.5</version>
    <scope>provided</scope>
</dependency>
```

删掉common里xss/xssfiler和XssHttpServletRequestWrapper

在product项目的resources目录下新建`application.yml`

```yaml
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall_pms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.jdbc.Driver

# MapperScan
# sql映射文件位置
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
```

> classpath 和 classpath* 区别：
> classpath：只会到你的class路径中查找找文件;
> classpath*：不仅包含class路径，还包括jar文件中(class路径)进行查找
>
> `classpath*` 的使用：当项目中有多个classpath路径，并同时加载多个classpath路径下（此种情况多数不会遇到）的文件，`*`就发挥了作用，如果不加`*`，则表示仅仅加载第一个classpath路径。

测试：在主启动类上加上注解`@MapperScan()`扫描包

```java
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
 */
@MapperScan("com.kexin.mall.product.dao")
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }
}

```

```java
// 先通过下面方法给数据库添加内容
@SpringBootTest
class MallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setDescript("测试描述");
        brandEntity.setName("中文乱码测试12");
        brandService.save(brandEntity);
        System.out.println("保存成功");
    }
}
```

```java
@SpringBootTest
class MallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(6L);
        brandEntity.setDescript("修改");
        brandService.updateById(brandEntity);
    }
}
```

在数据库中就能看到新增数据和修改数据了



#### 优惠券coupon模块

优惠券服务。重新打开generator逆向工程，修改`generator.properties`

启动生成`RenrenApplication.java`，运行后去浏览器80端口查看，同样让他一页全显示后选择全部后生成。生成后解压复制到coupon项目对应目录下。

让coupon也依赖于common，修改`pom.xml`

resources下src包先删除

添加`application.yml`

修改yml数据库信息（端口号后面会设置）

运行`mallCouponApplication.java`

![image-20210328185656798](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328185656798.png)

#### 会员member模块

重新使用代码生成器生成ums，模仿上面修改下面配置

修改代码生成器

新建application.yml

```properties
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall_sms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.jdbc.Driver

# MapperScan
# sql映射文件位置
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto

server:
  port: 7000
```

重启RenrenApplication.java，然后同样去浏览器获取压缩包解压到对应member项目目录

member也导入依赖

order端口是9000，product是10000，ware是11000。

以后比如order系统要复制多份，他的端口计算9001、9002

![image-20210328191110735](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328191110735.png)

#### 订单Order模块

order端口是9000

同样操作，测试结果：

http://localhost:9000/order/order/list

![image-20210328191906412](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328191906412.png)

#### 仓库Ware模块

ware端口是11000

同样操作，测试结果：

http://localhost:11000/ware/wareinfo/list

![image-20210328192506895](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328192506895.png)



# 8、SpringCloud Alibaba

#### 分布式组件-SpringCloud Alibaba

![image-20210328203628016](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210328203628016.png)

![image-20210329134819702](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329134819702.png)

阿里18年开发的微服务一站式解决方案。

https://github.com/alibaba/spring-cloud-alibaba/blob/master/README-zh.md

* 注册中心

* 配置中心

* 网关

  

Netflix把feign闭源了，spring cloud开了个open feign

Nacos等同于以前的Eureka， Sentinel等同于以前的Hystrix

在common的pom.xml中加入

```xml
<dependencyManagement>
    <dependencies>
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

上面是依赖管理，相当于以后再dependencies里引spring cloud alibaba就不用写版本号， 全用dependencyManagement进行管理。注意他和普通依赖的区别，他只是备注一下，并没有加入依赖



# 9、Nacos作为注册中心

一个更易于构建云原生应用的动态服务发现、配置管理和服务管理平台。

用nacos作为本项目的注册中心和配置中心。

注册中心文档：https://github.com/alibaba/spring-cloud-alibaba/tree/master/spring-cloud-alibaba-examples/nacos-example/nacos-discovery-example



使用注册中心前，需要先有一个注册中心。下载Nacos Server中间件地址：**https://github.com/alibaba/spring-cloud-alibaba**

我这里下了1.1.3b版本



安装启动nacos：

下载–解压–双击bin/startup.cmd。http://127.0.0.1:8848/nacos/ 账号密码nacos

> Linux/Unix/Mac 操作系统，执行命令 `sh startup.sh -m standalone`



**使用nacos：**

在某个项目里properties里写spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848（yaml同理，指定nacos的地址）。再指定applicatin.name告诉注册到nacos中以什么命名

* 第一步，添加依赖：放到common的pom里，不写版本是因为里面有了版本管理

```xml
 <dependency>
 	<groupId>com.alibaba.cloud</groupId>
 	<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
 </dependency>
```

下载完nacos要yml里面配置，这里以coupon模块举例

* 第二步，coupon的`application.yml`内容，配置了服务中心名和当前模块名字

```properties
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall-sms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848  #这个是nacos本机注册地址
  application:
    name: mall-coupon


mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto

server:
  port: 7000

```

> `server-addr: 127.0.0.1:8848`  #这个是nacos本机注册地址

* 第三步，使用 `@EnableDiscoveryClient` 注解开启服务注册与发现功能

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RestController
    class EchoController {
        @GetMapping(value = "/echo/{string}")
        public String echo(@PathVariable String string) {
            return string;
        }
    }
}

```

nacos测试：

http://192.168.160.1:8848/nacos  账号密码都是nacos

![](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329151341261.png)



然后依次给member模块配置上面的yaml，改下name就行。再给每个项目配置类上加上注解@EnableDiscoveryClient



# 10、Fegin(声明式远程调用)与注册中心

feign是一个声明式的HTTP客户端，他的目的就是让远程调用更加简单。给远程服务发的是HTTP请求。



### 测试member和coupon的远程调用

想要获取当前会员领取到的所有优惠券。先去注册中心找优惠券服务，注册中心调一台优惠券服务器给会员，会员服务器发送请求给这台优惠券服务器，然后对方响应。

- 服务请求方发送了2次请求，先问nacos要地址，然后再请求

会员服务想要远程调用优惠券服务，只需要给会员服务里引入openfeign依赖，他就有了远程调用其他服务的能力。

添加openfeign依赖，pom.xml

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

之前在member的pom.xml已经引用过了（微服务）。



##### coupon部分

在coupon中修改如下的内容

```java
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @RequestMapping("/member/list")
    public R membercoupons(){    //全系统的所有返回都返回R
        // 测试用：应该去数据库查用户对于的优惠券，但这个我简化了，不去数据库查了，构造了一个优惠券给他返回
        CouponEntity couponEntity = new CouponEntity();
        couponEntity.setCouponName("满100-10");//优惠券的名字
        return R.ok().put("coupons",Arrays.asList(couponEntity));
    }

```

这样准备好了优惠券的调用内容



##### member部分

在member的配置类上加注解`@EnableDiscoveryClient，`告诉member是一个远程调用客户端，member要调用东西的

```java
/*
 * 想要远程调用的步骤：
 * 	1、引入open-feign
 *  2、编写一个接口，告诉springcloud这个接口需要调用远程服务
 *      2.1 在接口里声明@FeignClient("mall-coupon")他是一个远程调用客户端且要调用coupon服务
 * 		2.2 要调用coupon服务的/coupon/coupon/member/list方法
 *  3、开启远程调用功能 @EnableFeignClients，要指定远程调用功能放的基础包
 */
@EnableFeignClients(basePackages="com.kexin.mall.member.feign")
@SpringBootApplication
@EnableDiscoveryClient  // nacos注解开启服务注册与发现功能
public class MallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberApplication.class, args);
    }
}
```

那么要调用什么东西呢？就是我们刚才写的优惠券的功能，复制函数部分.

在member中添加一个fiegn目录，用来放所有远程调用的接口

在member的com.kexin.mall.member.feign包下新建类：

```java
@FeignClient("mall-coupon") //告诉spring cloud这个接口是一个远程客户端，要调用coupon服务(nacos中找到)，具体是调用coupon服务的/coupon/coupon/member/list对应的方法
public interface CouponFeignService {
    
    // 远程服务的url
    //注意写全优惠券类上还有映射
    //注意这个地方不是控制层，所以这个请求映射请求的不是服务器上的东西，而是nacos注册中心的
    @RequestMapping("/coupon/coupon/member/list")
    public R membercoupons();//得到一个R对象
}
```

> @FeignClient+@RequestMapping构成远程调用的坐标。其他类中看似只是调用了CouponFeignService.membercoupons()，而实际上该方法跑去nacos里和rpc里调用了才拿到东西返回
>



然后在member的控制层MemberController写一个测试请求

```java
@Autowired
    CouponFeignService couponFeignService; // 远程接口注入

    // 测试用
    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("会员昵称张三");
        // 调用远程服务查询
        R membercoupons = couponFeignService.membercoupons();//假设张三去数据库查了后返回了张三的优惠券信息

        //打印会员和优惠券信息
        return R.ok().put("member",memberEntity).put("coupons",membercoupons.get("coupons"));
    }
```

测试：

重新启动服务, http://localhost:8000/member/member/coupons

![image-20210329201254342](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329201254342.png)

> coupon里的R.ok()是什么，就是设置了个msg
>

```java
public class R extends HashMap<String, Object> {//R继承了HashMap
    // ok是个静态方法，new了一个R对象，并且
    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);//调用了super.put(key, value);，即hashmap的put
        return r;
    }
}
```

### nacos作为配置中心

我们还可以用nacos作为配置中心。配置中心的意思是不在application.properties等文件中配置了，而是放到nacos配置中心公用，这样无需每台机器都改。

官方教程：https://github.com/alibaba/spring-cloud-alibaba/blob/master/spring-cloud-alibaba-examples/nacos-example/nacos-config-example/readme-zh.md

common中添加依赖 nacos配置中心

```java
<dependency>
     <groupId>com.alibaba.cloud</groupId>
     <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
 </dependency>
```

在coupons项目中创建/src/main/resources/`bootstrap.properties` ，这个文件是springboot里规定的，他优先级别application.properties高

```properties
# 改名字，对应nacos里的配置文件名
spring.application.name=mall-coupon
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
```

还是原来我们使用配置的方式，只不过优先级变了，所以匹配到了nacos的配置

还是配合@Value注解使用

```java
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    //从application.properties中获取
    //不要写user.name，他是环境里的变量
    @Value("${coupon.user.name}")
    private String name;
    @Value("${coupon.user.age}")
    private Integer age;
    
    @RequestMapping("/test")
    public R test(){
        return R.ok().put("name",name).put("age",age);
    }
```

浏览器去nacos里的配置列表，点击＋号，data ID：`mall-coupon.properties`，配置

```properties
# gulimall-coupon.properties
coupon.user.name="配置中心"      
coupon.user.age=12
```

![image-20210329204639300](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329204639300.png)

然后点击发布。重启coupon（生产中加入@RefreshScope即可），http://localhost:7000/coupon/coupon/test

![image-20210329204627175](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329204627175.png)

但是修改怎么办？实际生产中不能重启应用。在coupon的控制层上加`@RefreshScope`

```java
@RefreshScope   // 动态刷新
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    //从application.properties中获取
    //不要写user.name，他是环境里的变量
    // 测试配置中心
    @Value("${coupon.user.name}")
    private String name;
    @Value("${coupon.user.age}")
    private Integer age;
    @RequestMapping("/test")
    public R test(){
        return R.ok().put("name",name).put("age",age);
    }
```

重启后(让注解生效)，在nacos浏览器里修改配置，修改就可以观察到能动态修改了

nacos的配置内容优先于项目本地的配置内容。

总结：

![image-20210329205618509](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329205618509.png)

### 配置中心进阶

在nacos浏览器中还可以配置：

* **命名空间**：用作配置隔离。（一般每个微服务一个命名空间）

  * 默认public。默认新增的配置都在public空间下

  * 开发、测试、开发可以用命名空间分割。properties每个空间有一份。

    ![image-20210329210101606](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329210101606.png)

  * 在`bootstrap.properties`里配置（测试完去掉，学习不需要）

  * ```properties
    # 可以选择对应的命名空间 # 写上对应环境的命名空间ID
    spring.cloud.nacos.config.namespace=b176a68a-6800-4648-833b-be10be8bab00
    ```

  * 也可以为每个微服务配置一个命名空间，微服务互相隔离，只加载自己命名空间下的配置

* **配置集**：一组相关或不相关配置项的集合。

* **配置集ID**：类似于配置文件名，即Data ID

* **配置分组**：默认所有的配置集都属于`DEFAULT_GROUP`。双十一，618的优惠策略改分组即可

  * ```properties
    # 更改配置分组
    spring.cloud.nacos.config.group=DEFAULT_GROUP
    ```

最终方案：每个微服务创建自己的命名空间，然后使用配置分组区分环境（dev/test/prod）



**加载多配置集**

我们要把原来`application.yml`里的内容都分文件抽离出去。我们在nacos里创建好后，在coupons里指定要导入的配置即可。

数据源有关的放到`datasource.yml`中

```properties
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall_sms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
```

mybatis框架有关的配置放到`mybatis.yml`

```yaml
# MapperScan
# sql映射文件位置
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
```

其他配置放到`other.yml`

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848  #这个是nacos本机注册地址
  application:
    name: mall-coupon
    
server:
  port: 7000
```

![image-20210329225511545](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329225511545.png)

`bootstrap.properties`

在其中用数组`spring.cloud.nacos.config.extension-configs[]`写明每个配置集

```properties
# 改名字，对应nacos里的配置文件名
spring.application.name=mall-coupon
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.namespace=35a2b8db-69b1-4166-be0c-823d4def971a
spring.cloud.nacos.config.group=dev

spring.cloud.nacos.config.extension-configs[0].data-id=datasource.yml
spring.cloud.nacos.config.extension-configs[0].group=dev
spring.cloud.nacos.config.extension-configs[0].refresh=true

spring.cloud.nacos.config.extension-configs[1].data-id=mybatis.yml
spring.cloud.nacos.config.extension-configs[1].group=dev
spring.cloud.nacos.config.extension-configs[1].refresh=true

spring.cloud.nacos.config.extension-configs[2].data-id=other.yml
spring.cloud.nacos.config.extension-configs[2].group=dev
spring.cloud.nacos.config.extension-configs[2].refresh=true

```

总结：![image-20210329230320319](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329230320319.png)



# 11、GateWay网关

![image-20210329230342615](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329230342615.png)

![image-20210329230558561](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329230558561.png)



![image-20210329230613080](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329230613080.png)

**动态上下线**：发送请求需要知道商品服务的地址，如果商品服务器有123服务器，1号掉线后，还得改，所以需要网关动态地管理，他能从注册中心中实时地感知某个服务上线还是下线。【先通过网关，网关路由到服务提供者】

**拦截**：请求也要加上询问权限，看用户有没有权限访问这个请求，也需要网关。

所以我们使用spring cloud的`gateway组件`做网关功能。

网关是请求流量的入口，常用功能包括路由转发，权限校验，限流控制等。springcloud gateway取代了zuul网关。

https://spring.io/projects/spring-cloud-gateway

参考手册：https://cloud.spring.io/spring-cloud-gateway/2.2.x/reference/html/

**三大核心概念：**

1. **Route**: The basic building block of the gateway. It is defined by an ID, a destination URI, a collection of predicates断言, and a collection of filters. A route is matched if the aggregate predicate is true.发一个请求给网关，网关要将请求路由到指定的服务。路由有id，目的地uri，断言的集合，匹配了断言就能到达指定位置

2. **Predicate断言**: This is a Java 8 Function Predicate. The input type is a Spring Framework ServerWebExchange. This lets you match on anything from the HTTP request, such as headers or parameters.就是java里的断言函数，匹配请求里的任何信息，包括请求头等。根据请求头路由哪个服务

3. **Filter**: These are instances of Spring Framework `GatewayFilter` that have been constructed with a specific factory. Here, you can modify requests and responses before or after sending the downstream request.过滤器请求和响应都可以被修改。

客户端发请求给服务端。中间有网关。先交给映射器，如果能处理就交给handler处理，然后交给一系列filer，然后给指定的服务，再返回回来给客户端。

![image-20210329230431028](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210329230431028.png)

创建，使用initilizer，Group：com.kexin.mall，Artifact： mall-gateway，package：com.kexin.mall.gateway。 搜索gateway选中。

pom.xml里加上common依赖， 修改jdk版本，

在gateway服务中开启注册服务发现`@EnableDiscoveryClient`，配置nacos注册中心地址applicaion.properties。这样gateway也注册到了nacos中，其他服务就能找到nacos，网关也能通过nacos找到其他服务

```properties
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
spring.application.name=mall-gateway
server.port=88
```

bootstrap.properties 填写nacos配置中心地址

```properties
spring.application.name=gulimall-gateway
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.namespace=bfa85f10-1a9a-460c-a7dc-efa961b45cc1
```

本项目在nacos中的服务名

```properties
spring:
    application:
        name: gulimall-gateway
```

再去nacos里创建命名空间gateway（项目与项目用命名空间隔离），然后在命名空间里创建文件mall-gateway.yml

在项目里创建application.yml，根据条件转发到uri等

```properties
spring:
  cloud:
    gateway:
      routes:
        - id: test_route
          uri: https://www.baidu.com
          predicates:
            - Query=url,baidu

        - id: qq_route
          uri: https://www.qq.com
          predicates:
            - Query=url,qq

#        - id: product_route
#          uri: lb://gulimall-product
#          predicates:
#            - Path=/api/product/**
#          filters:
#            - RewritePath=/api/(?<segment>.*),/$\{segment}
#
#        - id: third_party_route
#          uri: lb://gulimall-third-party
#          predicates:
#            - Path=/api/thirdparty/**
#          filters:
#            - RewritePath=/api/thirdparty/(?<segment>.*),/$\{segment}
#
#        - id: member_route
#          uri: lb://gulimall-member
#          predicates:
#            - Path=/api/member/**
#          filters:
#            - RewritePath=/api/(?<segment>.*),/$\{segment}
#
#        - id: ware_route
#          uri: lb://gulimall-ware
#          predicates:
#            - Path=/api/ware/**
#          filters:
#            - RewritePath=/api/(?<segment>.*),/$\{segment}
#
#        - id: admin_route
#          uri: lb://renren-fast
#          predicates:
#            - Path=/api/**
#          filters:  # 这段过滤器和验证码有关，api内容缓存了/renren-fast，还得注意/renren-fast也注册到nacos中
#            - RewritePath=/api/(?<segment>.*),/renren-fast/$\{segment}
#


  ## 前端项目，/api前缀。开来到网关后断言先匹配到，过滤器修改url，比如跳转到renren微服务，所以要注意renren后端项目也注册到 nacos里
## http://localhost:88/api/captcha.jpg   http://localhost:8080/renren-fast/captcha.jpg
## http://localhost:88/api/product/category/list/tree http://localhost:10000/product/category/list/tree


```

测试 localhost:8080/hello?url=baidu





# 12、前端vue

TODO: 前端笔记



# 13、三级分类开发

一般来说的三级分类“

![image-20210330123109191](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330123109191.png)

此处三级分类最起码得启动renren-fast、nacos、gateway、product

### element-ui的使用 

npm i element-ui -S
https://element.eleme.cn/#/zh-CN/component/tree

提供了tree组件，他的数据是以data属性显示的。而他的子菜单是由data里的children属性决定的，当然这个属性可以改

```vue
defaultProps: {
    children: "children",
    label: "name"
}
```




### pms_category表说明

代表商品数据库的分类

```sql
CREATE DATABASE /*!32312 IF NOT EXISTS*/`gulimall_pms` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `gulimall_pms`;

SET FOREIGN_KEY_CHES=0;

DROP TABLE IF EXISTS `pms_category`;
CREATE TABLE `pms_category`  (
    `cat_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分类id',
    `name` char(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类名称',
    `parent_cid` bigint(20) NULL DEFAULT NULL COMMENT '父分类id',
    `cat_level` int(11) NULL DEFAULT NULL COMMENT '层级',
    `show_status` tinyint(4) NULL DEFAULT NULL COMMENT '是否显示[0-不显示，1显示]',
    `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
    `icon` char(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标地址',
    `product_unit` char(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '计量单位',
    `product_count` int(11) NULL DEFAULT NULL COMMENT '商品数量',
    PRIMARY KEY (`cat_id`) USING BTREE, 
    INDEX `parent_cid`(`parent_cid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1437 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品三级分类' ROW_FORMAT = Dynamic;

```

cat_id：分类id，cat代表分类，bigint(20)
name：分类名称
parent_cid：在哪个父目录下
cat_level：分类层级
show_status：是否显示，用于逻辑删除
sort：同层级同父目录下显示顺序
ico图标，product_unit商品计量单位，
InnoDB表，自增大小1437，utf编码，动态行格式



接着操作后台

localhost:8001 ， 点击系统管理，菜单管理，新增

- 目录
- `商品系统`
- 一级菜单

![image-20200425164019287](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/fbb8434bb556db0c66a04c4cc82a02a2.png)

刷新，看到左侧多了商品系统，添加的这个菜单其实是添加到了guli-admin.sys_menu表里

(新增了memu_id=31 parent_id=0 name=商品系统 icon=editor )

继续新增：

菜单
分类维护
商品系统
product/category
…
menu

![image-20200425164509143](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/6c67b73c8075604ff5b71aa4f95cdc43.png)

guli-admin.sys_menu表又多了一行，父id是刚才的商品系统id

**菜单路由**
在左侧点击【商品系统-分类维护】，希望在此展示3级分类。可以看到

* url是`http://localhost:8001/#/product-category`
* 填写的菜单路由是product/category
* 对应的视图是src/view/modules/product/category.vue

再如sys-role具体的视图在`renren-fast-vue/views/modules/sys/role.vue`

所以要自定义我们的product/category视图的话，就是创建`mudules/product/category.vue`

输入vue快捷生成模板，然后去https://element.eleme.cn/#/zh-CN/component/tree

看如何使用多级目录:

* el-tree中的data是要展示的树形数据
* props属性设置
* @node-click单击函数

```vue
<el-tree :data="data" :props="defaultProps" @node-click="handleNodeClick"></el-tree>

<script>
  export default {
    data() {
      return {
        data: [{
          label: '一级 1',
          children: [{
            label: '二级 1-1',
            children: [{
              label: '三级 1-1-1'
            }]
          }]
        }, {
          label: '一级 2',
          children: [{
            label: '二级 2-1',
            children: [{
              label: '三级 2-1-1'
            }]
          }, {
            label: '二级 2-2',
            children: [{
              label: '三级 2-2-1'
            }]
          }]
        }, {
          label: '一级 3',
          children: [{
            label: '二级 3-1',
            children: [{
              label: '三级 3-1-1'
            }]
          }, {
            label: '二级 3-2',
            children: [{
              label: '三级 3-2-1'
            }]
          }]
        }],
        defaultProps: {
          children: 'children',
          label: 'label'
        }
      };
    },
    methods: {
      handleNodeClick(data) {
        console.log(data);
      }
    }
  };
</script>

```



在CategoryController中

```java
/**
     * 查出所有分类以及子分类，以树形结构组装起来
     */
@RequestMapping("/list/tree")
public R list(){
    List<CategoryEntity> entities= categoryService.listWithTree();
    return R.ok().put("data", entities);
}
```

在CategoryService中添加

```java
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();
}
```

CategoryServiceImpl中添加，首先要把dao注入进来可以不写成`@Autowired CategoryDao categoryDao`;

```java
@Override
    public List<CategoryEntity> listWithTree() {
        // 1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        return entities;
    }
```

![image-20210330125511702](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330125511702.png)

需要组装成父子结构：

为了找到子分类，我们在category entity里面再添加一个属性，包含其所有子分类

```java
	/**
	 * 包含其所有子分类, （不是数据表里面的相关属性，需要注解）
	 */
	@TableField(exist = false)
	private List<CategoryEntity> children;
```

```java
// CategoryServiceImpl
@Override
    public List<CategoryEntity> listWithTree() {
        // 1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2、组装成父子的树形结构
        //      2.1）找到所有一级分类
        //      2.2）将当前菜单的子分类写进去（用一个递归方法找到所有子菜单）
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getCatLevel() == 1)
                .map(menu -> {
                    // 递归找到当前遍历菜单的所有子菜单
                    menu.setChildren(getChildens(menu, entities));
                    return menu;
                })
                // 找到子菜单后，排序子菜单。这里的空判断是为了防止了空指针
                .sorted(Comparator.comparingInt(item -> (item.getSort() == null ? 0 : item.getSort())))
                .collect(Collectors.toList());
        return level1Menus;
    }

    /**
     * 递归查找当前菜单的子菜单
     * @param root  当前菜单
     * @param all   从哪里获得菜单（所有菜单）
     * @return
     */
    private List<CategoryEntity> getChildens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> childList = all.stream()
                // 规范tips：包装类型间的相等判断应该用equals，而不是用==
                // 1、找到子菜单
                .filter(item -> item.getParentCid().equals(root.getCatId()))
                .map(menu -> {
                    menu.setChildren(getChildens(menu, all));
                    return menu;
                })
                // 2、菜单的排序
                .sorted(Comparator.comparingInt(item -> (item.getSort() == null ? 0 : item.getSort())))
                .collect(Collectors.toList());
        return childList;
    }
```

在找到当前菜单的子菜单们之后，同时还需要排序子菜单

![image-20210330133549014](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330133549014.png)



### 配置网关路由与路径重写

这里是对应关系

![image-20210330134929054](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330134929054.png)

路由规则前面的**/**会被替换成-

在后台系统手动添加新的目录“商品系统”，数据库的admin—sys_menu表就能看新添加的商品系统

![image-20210330134500662](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330134500662.png)

![image-20210330134612693](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330134612693.png)

前端：在views的modules目录下创建product目录, 创建包下的 category.vue

测试一下 请求失败，是给`localhost:8080/renren-fast`发起的请求，应该是给网关10000发起请求，

![image-20210330140458624](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330140458624.png)

1. 如果基准路径改成10000，如果给其他服务发请求，又要改地址
2. 商品服务还有其他端口如11000,12000 如果不能用啦，改为其他的，难道每次都要改基准路径吗

搭建个网关，让网关路由到10000（即将vue项目里的请求都给网关，网关经过url处理后，去nacos里找到管理后台的微服务，就可以找到对应的端口了，这样我们就无需管理端口，统一交给网关管理端口接口）

ctrl +shift+F 全局搜索基准路径

在`static/config/index.js`里

![image-20210330140713524](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330140713524.png)

我們应该给网关发请求， 然后路由到10000

```
window.SITE_CONFIG['baseUrl'] = 'http://localhost:88';  (之后添加api前缀)

// 意思是说本vue项目中要请求的资源url都发给88/api，那么我们就让网关端口为88，然后匹配到/api请求即可，
// 网关可以通过过滤器处理url后指定给某个微服务
// renren-fast服务已经注册到了nacos中
```

 

再次运行http://localhost:8001/#/login会报错，不显示验证码。 因为直接给网关发请求，但是验证码来源于renrenfast（验证码是请求88的，所以不显示。而验证码是来源于fast后台的）

* 现在的验证码请求路径为，http://localhost:88/api/captcha.jpg?uuid=69c79f02-d15b-478a-8465-a07fd09001e6
* 原始的验证码请求路径：http://localhost:8001/renren-fast/captcha.jpg?uuid=69c79f02-d15b-478a-8465-a07fd09001e6

> 88是gateway的端口
>
> ```
> # gateway微服务的配置
> spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
> spring.application.name=gulimall-gateway
> server.port=88
> ```



让网关默认把请求转给renrenfast，要转之前就要发现这个服务，这个服务首先要注册到nacos注册中心，先依赖gulimall-common工程，因为里面引入了nacos 注册和配置中心，这样请求88网关转发到8080fast

让fast里加入注册中心的依赖，而common中有nac即可os依赖，所以引入common

```xml
<dependency>
    <!-- 里面有nacos注册中心 -->
    <groupId>com.atguigu.gulimall</groupId>
    <artifactId>mall-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency> 

```

在renrenfast application.yml配置nacos注册中心

```properties
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848  #这个是nacos本机注册地址
  application:
    name: renren-fast
```

然后在fast启动类上加上注解`@EnableDiscoveryClient`，重启

然后在nacos的服务列表里看到了renren-fast

去gateway网关的配置文件

```properties
spring:
  cloud:
    gateway:
      routes:
        - id: test_route
          uri: https://www.baidu.com
          predicates:
            - Query=url,baidu
		## 增加路由配置 lb代表loadbalance负载均衡 断言表示哪种情况下需要路由给它
        - id: admin_route
          uri: lb://renren-fast
          predicates:
            - Path:/api/**
## 前端项目，/api

```

前端修改为

```
  // api接口请求地址
  window.SITE_CONFIG['baseUrl'] = 'http://localhost:88/api';
```

问题：

```
## 前端项目，都带上/api前缀。开来到网关后断言先匹配到，过滤器修改url，比如跳转到renren微服务，所以要注意renren后端项目也注册到 nacos里
## http://localhost:88/api/captcha.jpg（进入网关时的地址）   http://localhost:8080/renren-fast/captcha.jpg（真正能访问验证码）
## http://localhost:88/api/product/category/list/tree http://localhost:10000/product/category/list/tree
```



修改过vue里的api后，此时验证码请求的是http://localhost:88/api/captcha.jpg?uuid=72b9da67-0130-4d1d-8dda-6bfe4b5f7935

也就是说，他请求网关，路由到了fast，然后取nacos里找fast。

找到后拼接成了http://renren-fast:8080/api/captcha.jpg

但是正确的是localhost:8080/renren-fast/captcha.jpg

所以要利用网关带的路径重写Filter才能真正能访问验证码，参考https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-rewritepath-gatewayfilter-factory



```properties
        - id: admin_route
          uri: lb://renren-fast   # lb是指load balancer负载均衡
          predicates:
            - Path=/api/**
          filters:  # 这段过滤器和验证码有关，api内容缓存了/renren-fast，还得注意/renren-fast也注册到nacos中
            - RewritePath=/api/(?<segment>.*),/renren-fast/$\{segment}
```

还是报错（出现了跨域的问题，就是说vue项目是8001端口，却要跳转到88端口，为了安全性，不可以）

![image-20210330143131064](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330143131064.png)



### CORS配置跨域

问题描述：已拦截跨源请求：同源策略禁止8001端口页面读取位于 http://localhost:88/api/sys/login 的远程资源。（原因：CORS 头缺少 ‘Access-Control-Allow-Origin’）。

问题分析：这是一种跨域问题。访问的域名或端口和原来请求的域名端口一旦不同，请求就会被限制

跨域：指的是浏览器不能执行其他网站的脚本。它是由浏览器的同源策略造成的，是浏览器对js施加的安全限制。（ajax可以）
同源策略：是指`协议，域名，端囗`都要相同，其中有一个不同都会产生跨域；

js要获取数据，要发ajax请求，使用xmlHttpRequest对象，这个对象想要从本网站，如端口号8001到88发送请求。默认不允许，是用同源策略来限制

![image-20210330143250613](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330143250613.png)

https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Access_control_CORS

跨域流程：

这个跨域请求的实现是通过预检请求实现的，先发送一个OPSTIONS探路，收到响应允许跨域后再发送真实请求

> 什么意思呢？跨域是要请求的、新的端口那个服务器限制的，不是浏览器限制的。

```
跨域请求流程：
非简单请求(PUT、DELETE)等，需要先发送预检请求


       -----1、预检请求、OPTIONS ------>
       <----2、服务器响应允许跨域 ------
浏览器 |                               |  服务器
       -----3、正式发送真实请求 -------->
       <----4、响应数据   --------------

```

### 跨域的解决方案

- 方法1：设置nginx包含admin和gateway。都先请求nginx，这样端口就统一了
- 方法2：让服务器告诉预检请求能跨域

**解决方案1：**



![image-20210330143845572](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330143845572.png)



**解决方案二: 为在服务端2配置允许跨域**

在响应头中添加：参考：https://blog.csdn.net/qq_38128179/article/details/84956552

* Access-Control-Allow-Origin ： 支持哪些来源的请求跨域

* Access-Control-Allow-Method ： 支持那些方法跨域

* Access-Control-Allow-Credentials ：跨域请求默认不包含cookie，设置为true可以包含cookie

* Access-Control-Expose-Headers ： 跨域请求暴露的字段

  * CORS请求时，XMLHttpRequest对象的getResponseHeader()方法只能拿到6个基本字段：

    Cache-Control、Content-Language、Content-Type、Expires、Last-Modified、Pragma
    如果想拿到其他字段，就必须在Access-Control-Expose-Headers里面指定。

* Access-Control-Max-Age ：表明该响应的有效时间为多少秒。在有效时间内，浏览器无须为同一请求再次发起预检请求。请注意，浏览器自身维护了一个最大有效时间，如果该首部字段的值超过了最大有效时间，将失效



解决方法：在网关中定义“`MallCorsConfiguration`”类，该类用来做过滤，允许所有的请求跨域。

网关统一配置跨域问题

```java
package com.kexin.mall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration // 标注是配置类，gateway
public class MallCorsConfiguration {

    @Bean // 添加过滤器
    public CorsWebFilter corsWebFilter(){
        //基于url跨域，选用web.cors.reactive包下的，网关是webflux编程，响应式编程，
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// 跨域配置信息
        CorsConfiguration corsConfiguration=new CorsConfiguration();

        //配置跨域
        corsConfiguration.addAllowedHeader("*");// 允许跨域的头
        corsConfiguration.addAllowedMethod("*");// 允许跨域的请求方式
        corsConfiguration.addAllowedOrigin("*");// 允许跨域的请求来源
        corsConfiguration.setAllowCredentials(true);//允许携带cookie跨域

        //path:"/**":任意路径都要进行跨域配置
        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

}

```

再次访问：http://localhost:8001/#/login

出现了多个请求，并且也存在多个跨源请求。因为http://localhost:8001/renren已拦截跨源请求：同源策略禁止读取位于 http://localhost:88/api/sys/login 的远程资源。

（原因：不允许有多个 ‘Access-Control-Allow-Origin’ CORS 头）

![image-20210330150119533](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330150119533.png)

![image-20210330150049635](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330150049635.png)



为了解决这个问题，需要修改renren-fast项目，注释掉“`io.renren.config.CorsConfig`”类。然后再次进行访问。

![image-20210330150817833](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330150817833.png)

第一个是预检请求，第二个才是真实请求

第一次响应头多了这些字段，允许这些跨域![image-20210330150557504](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330150557504.png)

真实请求携带了真正的请求数据

![image-20210330150522194](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330150522194.png)



# 14、树形展示三级分类数据

> 之前解决了登录验证码的问题，/api/请求重写成了/renren-fast，但是vue项目中或者你自己写的数据库中有些是以/product为前缀的，它要请求product微服务，你要也让它请求renren-fast显然是不合适的。
>
> 解决办法是把请求在网关中以更小的范围先拦截一下，剩下的请求再交给renren-fast



分类维护显示404，请求的http://localhost:88/api/product/category/list/tree不存在

![image-20210330151007892](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330151007892.png)

这是因为网关上所做的路径映射不正确
映射后的路径为http://localhost:8001/renren-fast/product/category/list/tree
但是只有通过http://localhost:10000/product/category/list/tree路径才能够正常访问，所以会报404异常。



解决方案：先配置路由，再将product配置到注册中心

1. 定义一个product路由规则，在gateway中配置好路由进行路径重写：

```properties
        - id: product_route
          uri: lb://gulimall-product # 注册中心的服务
          predicates:
            - Path=/api/product/**
          filters:
            - RewritePath=/api/(?<segment>/?.*),/$\{segment}
```

2. 再配置好nacos注册中心和配置中心

   在product项目的application.yml

```properties
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/guli_pms?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848   #配置好nacos注册中心和配置中心

mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-value: 1
      logic-not-delete-value: 0

server:
  port: 10001
```

> 如果要使用nacos配置中心，可以这么做:
> 在nacos中新建命名空间，用命名空间隔离项目，(可以在其中新建mall-product.yml)
> 在product项目中新建bootstrap.properties，
>
> ```properties
> spring.application.name=mall-product
> spring.cloud.nacos.config.server-addr=127.0.0.1:8848
> spring.cloud.nacos.config.namespace=e6cd36a8-81a2-4df2-bfbc-f0524fa17664
> ```

为了让product注册到主类上加上注解@EnableDiscoveryClient

### 路由顺序

直接访问 localhost:88/api/product/category/list/tree是invalid token，非法令牌，后台管理系统中没有登录，所以没有带令牌

原因是后台登录没有带token访问导致的，admin_route拦截了，说明新配置的路由没有生效，请求是被负载均衡到renren-fast，转到后台管理系统（先匹配的先路由，fast和product路由重叠，fast要求登录），上面的断言提前生效，解决方法是调整一下路由顺序，将精确地路由放在高优先级，模糊路由放低优先级，会优先适配上面的断言

解决：在网关配置里面调整路由顺序，将精确的路由规则放置到模糊的路由规则的前面，否则的话，精确的路由规则将不会被匹配到，类似于异常体系中try catch子句中异常的处理顺序。

下面成功原因是：先访问gateway #88，网关路径重写后访问nacos8848，nacos找到服务

![image-20210330153514619](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330153514619.png)



接着修改前端`category.vue`，这里改的是点击分类维护后的右侧显示

把data解构出来，加上{}，把data的地方改成menus，解构对象获取到data

```javascript
  //方法集合
  methods: {
    getMenus() {
      this.$http({ // http://localhost:10000/renren-fast/product/category/list/tree
        url: this.$http.adornUrl("/product/category/list/tree"), // 体会一下我们要重写product项目里这个controller
        method: "get"
      })
        .then(({ data }) => { // success 响应到数据后填充到绑定的标签中
          this.menus = data.data; // 数组内容，把数据给menus，就是给了vue实例，最后绑定到视图上
        }) //fail
        .catch(() => {});
    },

```

此时有了3级结构，但是没有数据，在category.vue的模板中，数据是menus，而还有一个props。这是element-ui的规则

修改一下避免混淆，

label： 指定节点标签为节点对象的某个属性值

children：指定子树为节点对象的某个属性值

(参考开发文档https://element.eleme.cn/#/zh-CN/component/tree)

```javascript
<template>
  <el-tree :data="menus" :props="defaultProps" @node-click="handleNodeClick"></el-tree>
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      handleNodeClick(data) {
        console.log(data);
      },
      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data;
        })
      }
    }
  }
</script>
<style scoped>
</style>

```

当前结果：

![image-20210330154116587](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330154116587.png)



# 15、 删除分类-页面效果&逻辑删除

删除前提：没有子菜单、没有被其他菜单引用

### 页面效果

- 使用 render-content，渲染函数
- 使用 scoped slot：https://cn.vuejs.org/v2/guide/components-slots.html

> 可以通过两种方法进行树节点内容的自定义：render-content和 scoped slot。
>
> 使用render-content指定渲染函数，该函数返回需要的节点区内容即可。渲染函数的用法请参考 Vue 文档。
> 使用 scoped slot 会传入两个参数node和data，分别表示当前节点的 Node 对象和当前节点的数据。
> 注意：由于 jsfiddle 不支持 JSX 语法，所以render-content示例在 jsfiddle 中无法运行。但是在实际的项目中，只要正确地配置了相关依赖，就可以正常运行。

render-content：

```vue
<el-tree
      :data="data"
      show-checkbox
      node-key="id"
      default-expand-all
      :expand-on-click-node="false"
      :render-content="renderContent"> // 对应到函数，去得到数据并渲染
     
     匹配到了
     
      renderContent(h, { node, data, store }) {
        return ( // 返回要显示的dom元素
          <span class="custom-tree-node">
            <span>{node.label}</span>
            <span>
              <el-button size="mini" type="text" on-click={ () => this.append(data) }>Append</el-button>
              <el-button size="mini" type="text" on-click={ () => this.remove(node, data) }>Delete</el-button>
            </span>
          </span>);
      }

```

scoped slot（插槽）：在el-tree标签里把内容写到span标签栏里即可

根据文档添加新增和删除按钮，和对应的methods

![image-20210330154346092](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330154346092.png)



node与data
在element-ui的tree中，有3个非常重要的属性

* node代表当前结点（是否展开等信息，element-ui自带属性），
* data是结点数据，是自己的数据。
* data从哪里来：前面ajax发送请求，拿到data，赋值给menus属性，而menus属性绑定到标签的data属性。而node是ui的默认规则



```vue
<template>
  <el-tree :data="menus" :props="defaultProps" 
  :expand-on-click-node="false" show-checkbox node-key="catId">
    <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <!-- 只有一级和二级分类有添加，三级或者没有子节点的分类有删除-->
          <el-button v-if="node.level<=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini" @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
  </el-tree>
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      //该方法用于获取到ajax数据后设置到对应的tree属性上
      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data
        })
      },
      append(data) {
       console.log("append",data)
      },

      remove(node, data) {
        console.log("remove",node,data)
      }
    },
    //生命周期 - 创建完成（可以访问当前this实例）
    created() {
      this.getMenus();// 会设置"menus"变量的值
    }
  }
</script>
<style scoped>
</style>



```

页面加载完后，自动调用

```vue
created() {
	this.getMenus();// 会设置"menus"变量的值
}

注意到tree标签上有一个
    <el-tree :data="menus"
这样就自动注入到data上了

             
             
getMenus() : 该方法用于获取到ajax数据后设置到对应的tree属性上
             
也就是说，获取到数据后绑定到menus上 
而<el-tree :data="menus"
所以数据绑定好了
层级怎么体现的：后端返回的时候children就封装好了，因为ui-tree是按这个属性来的
java后端的实体类CategoryEntity有属性
private List<CategoryEntity> children;
该属性不是数据库现有的，而是在后端根据数据库信息现封装好的
```



### 逻辑删除

**初始化分类的controller**

返回的时候已经设置好了child属性，所以前端可以直接渲染

```java
/**
     * 查出所有分类 以及子分类，以树形结构组装起来
     注意这个方法的递归调用要多读一读，结合lambda确实对写代码思维有提高
     */
@RequestMapping("/list/tree")
public R list(){
    List<CategoryEntity> entities = categoryService.listWithTree();
    // 筛选出所有一级分类
    List<CategoryEntity> level1Menus = entities.stream().
        filter((categoryEntity) -> categoryEntity.getParentCid() == 0)
        .map((menu) -> { 
            //  递归设置// menu代表要求的root
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
        return (menu1.getSort() == null? 0 : menu1.getSort()) - (menu2.getSort() == null? 0 : menu2.getSort());
    })
        .collect(Collectors.toList());
    return R.ok().put("data", level1Menus);
}
/**
     * 递归找所有的子菜单、中途要排序
     */
private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all){
    List<CategoryEntity> children = all.stream().filter(categoryEntity ->
                                                        categoryEntity.getParentCid() == root.getCatId()
                                                       ).map(categoryEntity -> {
        categoryEntity.setChildren(getChildrens(categoryEntity, all));
        return categoryEntity;
    }).sorted((menu1,menu2) -> {
        return (menu1.getSort() == null? 0 : menu1.getSort()) - (menu2.getSort() == null? 0 : menu2.getSort());
    }).collect(Collectors.toList());
    return children;
}

```

重写2个按钮的事件，发送ajax操作数据库

要调整按钮的显示情况，用v-if=“node.level <= 2”

增加复选框 show-checkbox

结点唯一id：node-key=“catId”



先用Postman测试逻辑

![image-20210330160822878](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330160822878.png)

点击删除后再次查询数据库能够看到cat_id为1437的数据已经被删除了。

但是我们需要修改检查当前菜单是否被引用



#### 删除分类controller

我们可以删除某个分类，要点如下：

- 如果删除的不是最低级菜单，会提示删除包括父分类和所有子分类
- 删除的时候数据库里还有，只是标记某个字段 标记为不可见了

修改CategoryController类，添加如下代码：

```java
/**
     * 删除
     * @RequestBody 获取请求体，必须发送post请求
     * SpringMVC会自动将请求体的数据(json),转化为对应对象
     */
@RequestMapping("/delete")
//@RequiresPermissions("product:category:delete")
public R delete(@RequestBody Long[] catIds){
    //删除之前需要判断待删除的菜单那是否被别的地方所引用。
    //categoryService.removeByIds(Arrays.asList(catIds));

    categoryService.removeMenuByIds(Arrays.asList(catIds));
    return R.ok();
}

```

product.service.impl.CategoryServiceImpl

```java
@Override
public void removeMenuByIds(List<Long> asList) {
    //TODO 1.检查当前删除菜单是否被别的地方引用

    // 逻辑删除（非物理删除，只是改变数据库字段show_status）
    baseMapper.deleteBatchIds(asList);
}
```



然而多数时候，我们并不希望删除数据，而是标记它被删除了，这就是**逻辑删除**；

> 逻辑删除是mybatis-plus 的内容，会在项目中配置一些内容，告诉此项目执行delete语句时并不删除，只是标志位
>

假设数据库中有字段show_status为0，标记它已经被删除。

![image-20210330161638012](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330161638012.png)

mybatis-plus的逻辑删除：

1. 配置全局的逻辑删除规则（省略）

2. 配置逻辑删除的组件Bean（省略）

3. 给Bean加上逻辑删除注解`@TableLogic`

   

   > mybatis-plus说明:
   >
   > 只对自动注入的sql起效:
   >
   > 插入: 不作限制
   > 查找: 追加where条件过滤掉已删除数据,且使用 wrapper.entity 生成的where条件会忽略该字段
   > 更新: 追加where条件防止更新到已删除数据,且使用 wrapper.entity 生成的where条件会忽略该字段
   > 删除: 转变为 更新
   > 例如:
   >
   > 删除: `update user set deleted=1 where id = 1 and deleted=0`
   > 查找: `select id,name,deleted from user where deleted=0`
   > 字段类型支持说明:
   >
   > 支持所有数据类型(推荐使用 `Integer,Boolean,LocalDateTime`)
   > 如果数据库字段使用`datetime`,逻辑未删除值和已删除值支持配置为字符串`null`,另一个值支持配置为函数来获取值如now()
   > 附录:
   >
   > 逻辑删除是为了方便数据恢复和保护数据本身价值等等的一种方案，但实际就是删除。
   > 如果你需要频繁查出来看就不应使用逻辑删除，而是以一个状态去表示
   >

   

![image-20210330161654676](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330161654676.png)

配置全局的逻辑删除规则，在“src/main/resources/`application.yml`”文件中添加如下内容：

```properties
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-value: 1
      logic-not-delete-value: 0
```

修改product.entity.CategoryEntity实体类，添加上`@TableLogic`，表明使用逻辑删除：

```java
/**
	 * 是否显示[0-不显示，1显示]
	 */
@TableLogic(value = "1",delval = "0")
private Integer showStatus;

```

然后在POSTMan中测试一下是否能够满足需要。

#### 日志

在mall-product下设置日志级别可以看到具体的sql语句

```properties
logging:
  level:
    com.kexin.mall: debug
```

打印的日志：

```
 ==>  Preparing: UPDATE pms_category SET show_status=0 WHERE cat_id IN ( ? ) AND show_status=1 
 ==> Parameters: 1431(Long)
 <==    Updates: 1
 get changedGroupKeys:[]

```



### 删除效果细化

前端

```vue
<!-- slot -->
<span class="custom-tree-node" slot-scope="{ node, data }">
    <span>{{ node.label }}</span>
    <span>
        <el-button v-if="node.level <=2" type="text" size="mini" @click="() => append(data)">添加</el-button>
        <el-button type="text" size="mini" @click="edit(data)">编辑</el-button>
        <el-button
                   v-if="node.childNodes.length==0"
                   type="text"
                   size="mini"
                   @click="() => remove(node, data)"
                   >删除</el-button>
    </span>
</span>

```

前端的拦截逻辑

* 发送的请求：delete
* 发送的数据：this.$http.adornData(ids, false)
  * util/httpRequest.js中，封装了一些拦截器
* http.adornParams是封装get请求的数据
  * ajax的get请求会被缓存，就不会请求服务器了。所以我们在url后面拼接个date（使之无法url不一致），让他每次都请求服务器
* http.adornData是封装post请求的数据

```javascript
// 定义http对象，后面定义他的请求拦截器
const http = axios.create({
  timeout: 1000 * 30,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})

/**
 * 请求地址处理
 * @param {*} actionName action方法名称
 */
http.adornUrl = (actionName) => {
  // 非生产环境 && 开启代理, 接口前缀统一使用[/proxyApi/]前缀做代理拦截!
  return (process.env.NODE_ENV !== 'production' 
          && process.env.OPEN_PROXY ? '/proxyApi/' : window.SITE_CONFIG.baseUrl) + actionName
}

/**
 * 请求地址处理
 * @param {*} actionName action方法名称
 */
http.adornUrl = (actionName) => {
  // 非生产环境 && 开启代理, 接口前缀统一使用[/proxyApi/]前缀做代理拦截!
  return (process.env.NODE_ENV !== 'production' 
          && process.env.OPEN_PROXY ? '/proxyApi/' : window.SITE_CONFIG.baseUrl) + actionName
}

/**
 * get请求参数处理
 * @param {*} params 参数对象
 * @param {*} openDefultParams 是否开启默认参数?
 */
http.adornParams = (params = {}, openDefultParams = true) => {
  var defaults = {
    't': new Date().getTime()
  }
  return openDefultParams ? merge(defaults, params) : params
}

/**
 * post请求数据处理
 * @param {*} data 数据对象
 * @param {*} openDefultdata 是否开启默认数据?
 * @param {*} contentType 数据格式
 *  json: 'application/json; charset=utf-8'
 *  form: 'application/x-www-form-urlencoded; charset=utf-8'
 */
http.adornData = (data = {}, openDefultdata = true, contentType = 'json') => {
  var defaults = {
    't': new Date().getTime()
  }
  data = openDefultdata ? merge(defaults, data) : data
  return contentType === 'json' ? JSON.stringify(data) : qs.stringify(data)
}


```



抽取代码片段vue.code-snippets

```
{
    "http-get请求":{
        "prefix":"httpget",
        "body":[
            "this.\\$http({",
            "url:this,\\$http.adornUrl(''),",
            "method:'get',",
            "params:this.\\$http.adornParams({})",
            "}).then({data})=>{",
            "})"
        ],
        "description":"httpGET请求"
    },

    "http-post请求":{
        "prefix":"httppost",
        "body":[
            "this.\\$http({",
            "url:this,\\$http.adornUrl(''),",
            "method:'post',",
            "data: this.\\$http.adornData(data, false)",
            "}).then({data})=>{ })"
        ],
        "description":"httpPOST请求"
    }
}

```

- 删除时弹窗确认
- 删除成功弹窗
- 删除后重新展开父节点：重新ajax请求数据，指定展开的基准是:default-expanded-keys=“expandedKey”，返回数据后刷新this.expandedKey = [node.parent.data.catId];

```javascript
remove(node, data) {
      var ids = [data.catId];
    // 弹窗 确认
      this.$confirm(`是否删除【${data.name}】菜单?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => { // 点击确定
          this.$http({
              // 给delete发送
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false)
          }).then(({ data }) => {
              // 删除成功$message
            this.$message({
              message: "菜单删除成功",
              type: "success"
            });
            //刷新出新的菜单
            this.getMenus();
            //设置需要默认展开的菜单
            this.expandedKey = [node.parent.data.catId];
          });
        })
        .catch(// 取消
          () => {});
    }
  },
  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
    this.getMenus();
  },
  //生命周期 - 挂载完成（可以访问DOM元素）
  mounted() {},
  beforeCreate() {}, //生命周期 - 创建之前
  beforeMount() {}, //生命周期 - 挂载之前
  beforeUpdate() {}, //生命周期 - 更新之前
  updated() {}, //生命周期 - 更新之后
  beforeDestroy() {}, //生命周期 - 销毁之前
  destroyed() {}, //生命周期 - 销毁完成
  activated() {} //如果页面有keep-alive缓存功能，这个函数会触发
};

```



`httpRequest.js `定义了一个http对象，http对象定义了请求拦截，在发每一个请求之前，会从cookie里面获取到后台登录系统里面的token，

![image-20210330163055743](https://kexin-mall.oss-us-east-1.aliyuncs.com/images/typora-user-images/image-20210330163055743.png)

ajax的get请求会被缓存，有可能第一次发数据，就会被缓存，再发请求就不会向服务器获取到新的数据了，为了不缓存，随便在请求路径新增一个参数，这个参数不一样，随机数比如当前时间戳，请求就不会被缓存，每次的请求就会实时的访问服务器，。

:default-expanded-keys="expandedKey"

expandedKey:[],

```vue
<template>
  <el-tree 
    :data="menus" 
    :props="defaultProps" 
    :expand-on-click-node="false" 
    show-checkbox node-key="catId"
    :default-expanded-keys="expandedKey"
  >
    <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <!-- 只有一级和二级分类有添加，三级或者没有子节点的分类有删除-->
          <el-button v-if="node.level<=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini" @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
  </el-tree>
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        expandedKey:[],
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data
        })
      },
      append(data) {
       console.log("append",data)
      },

      remove(node, data) {
        let ids = [data.catId];
        this.$confirm(`是否删除【${data.name}】菜单, 是否继续?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$message({
            message: '菜单删除成功',
            type: 'success'
          });
          this.$http({
            url: this.$http.adornUrl('/product/category/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false),
          }).then(({data}) => {
            console.log("删除成功", data);
            //刷新出新的菜单
            this.getMenus();
            //默认要展示的菜单(删除节点父节点的id)
            this.expandedKey=[node.parent.data.catId];
          })
        }).catch(()=>{

        })

        console.log("remove", node, data);
      }
    }
  }
</script>
<style scoped>
</style>

```



# 16、新增

先去添加一个dialog对话框

https://element.eleme.cn/#/zh-CN/component/dialog

* 一个button的单击事件函数为@click=“dialogVisible = true”
* 一个会话的属性为:visible.sync=“dialogVisible”
* 导出的data中"dialogVisible = false"
* 点击确认或者取消后的逻辑都是@click=“dialogVisible = false” 关闭会话而已



v-model=  双向绑定表单中的某一个属性

```vue
<template>
<div>
  <el-tree 
    :data="menus" 
    :props="defaultProps" 
    :expand-on-click-node="false" 
    show-checkbox node-key="catId"
    :default-expanded-keys="expandedKey"
  >
    <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <!-- 只有一级和二级分类有添加，三级或者没有子节点的分类有删除-->
          <el-button v-if="node.level<=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini" @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
  </el-tree>

  <el-dialog
    title="提示"
    :visible.sync="dialogVisible"
    width="30%"
  >
  <!--嵌套表单-->
    <el-form :model="category"><!--绑定表单-->
      <el-form-item label="分类名称">
        <el-input v-model="category.name" autocomplete="off"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="addCategory">确 定</el-button>
    </span>
  </el-dialog>
</div>
  
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        expandedKey:[],
        category: {
          name:"",
          parentCid: 0,
          catLevel: 0,
          showStatus: 1,
          sort: 0,
          //catId: null,
        },
        dialogVisible: false,
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data
        })
      },
      append(data) {
        this.dialogVisible = true;  // 打开对话框
        console.log("append",data)

        this.category.parentCid = data.catId;
        this.category.catLevel = data.catLevel * 1 + 1;// str转int再计算，append的下一级为创建level
        this.category.showStatus = 1;
        this.category.sort = 0;

      },
      // 添加三级分类
      addCategory(){
        console.log("提交的三级分类数据", this.category);
        this.$http({
          url: this.$http.adornUrl("/product/category/save"),
          method: "post",
          data: this.$http.adornData(this.category, false),
        }).then(({ data }) => {
          this.$message({
            message: "The menu saved successfully",
            type: "success",
          });
          //关闭对话框
          this.dialogVisible = false;
          //刷新展开
          this.getMenus();
          //设置默认删除刷新完展开的菜单
          this.expandedKey = [this.category.parentCid];
        });

      },

      remove(node, data) {
        let ids = [data.catId];
        this.$confirm(`是否删除【${data.name}】菜单, 是否继续?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$message({
            message: '菜单删除成功',
            type: 'success'
          });
          this.$http({
            url: this.$http.adornUrl('/product/category/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false),
          }).then(({data}) => {
            console.log("删除成功", data);
            //刷新出新的菜单
            this.getMenus();
            //默认要展示的菜单(删除节点父节点的id)
            this.expandedKey=[node.parent.data.catId];
          })
        }).catch(()=>{

        })
        console.log("remove", node, data);
      }
    }
    //生命周期 - 创建完成（可以访问当前this实例）
    created() {
      this.getMenus();
    }
  }
</script>
<style scoped>
</style>

```

### 基本修改效果

- 点击修改弹出对话框，显示现有内容
- 输入新内容后确定，回显新内容
- 对话框是**复用**的添加的对话框，点击确定的时候回调的是同一个函数，为了区分当前对话框是单击修改还是点击添加打开的，所以添加一个`dialogType`、`title`属性。然后回调函数进行`if`判断
- 回显时候要发送请求获取最新数据

​     添加一个修改按钮<el-button type="text" size="mini" @click="() => modify(data)">Modify</el-button>

```vue
<template>
<div>
  <el-tree 
    :data="menus" 
    :props="defaultProps" 
    :expand-on-click-node="false" 
    show-checkbox node-key="catId"
    :default-expanded-keys="expandedKey"
  >
    <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <!-- 只有一级和二级分类有添加，三级或者没有子节点的分类有删除-->
          <el-button v-if="node.level<=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button type="text" size="mini" @click="() => modify(data)">Modify</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini" @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
  </el-tree>

  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="30%"
    :close-on-click-modal="false"
  >
  <!--嵌套表单-->
    <el-form :model="category"><!--绑定表单-->
      <el-form-item label="分类名称">
        <el-input v-model="category.name" autocomplete="off"></el-input>
      </el-form-item>
      <el-form-item label="图标">
          <el-input
            v-model="category.icon"
            autocomplete="off"
          ></el-input>
        </el-form-item>

        <el-form-item label="计量单位">
          <el-input
            v-model="category.productUnit"
            autocomplete="off"
          ></el-input>
        </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitData">确 定</el-button>
    </span>
  </el-dialog>
</div>
  
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        expandedKey:[],
        category: {
          name:"",
          parentCid: 0,
          catLevel: 0,
          showStatus: 1,
          sort: 0,
          catId: null,
          icon: "",
          productUnit: ""
        },
        dialogVisible: false,
        dialogType: "",  // add, modify
        title: "",
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data
        })
      },
      append(data) {
        // 要清空，改为默认值
        console.log("append",data)
        this.dialogVisible = true;  // 打开对话框
        this.dialogType = "add";
        this.title = "Add Category";
        this.category.parentCid = data.catId;
        this.category.catLevel = data.catLevel * 1 + 1;// str转int再计算，append的下一级为创建level
        this.category.name = "";
        this.category.catId = null;
        this.category.icon = "";
        this.category.productUnit ="";
        this.category.showStatus = 1;
        this.category.sort = 0;

      },
      // 修改菜单
      modify(data) {
        console.log("要修改的数据", data);
        this.dialogType = "modify";
        this.title = "Modify Category";
        this.dialogVisible = true;   
        
        //发送请求获得当前节点最新数据
        this.$http({
          url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
          method: "get",
        }).then(({ data }) => {
          //请求成功
          console.log("要回显的数据", data);
          this.category.name = data.data.name;
          this.category.catId = data.data.catId;
          this.category.icon = data.data.icon;
          this.category.productUnit = data.data.productUnit;
          this.category.parentCid = data.data.parentCid;
          this.category.catLevel = data.data.catLevel;
          this.category.showStatus = data.data.showStatus;
          this.category.sort = data.data.sort;
        });
      },
      submitData() {
        if (this.dialogType == "add") {
          this.addCategory();
        }
        if (this.dialogType == "modify") {
          this.modifyCategory();
        }
      },
      // 添加三级分类
      addCategory(){
        console.log("提交的三级分类数据", this.category);
        this.$http({
          url: this.$http.adornUrl("/product/category/save"),
          method: "post",
          data: this.$http.adornData(this.category, false),
        }).then(({ data }) => {
          this.$message({
            message: "The menu saved successfully",
            type: "success",
          });
          //关闭对话框
          this.dialogVisible = false;
          //刷新展开
          this.getMenus();
          //设置默认删除刷新完展开的菜单
          this.expandedKey = [this.category.parentCid];
        });

      },
      // 修改三级分类数据
      modifyCategory() {
        var {catId, name, icon, productUnit} = this.category;
        var data = {catId, name, icon, productUnit};
        this.$http({
          url: this.$http.adornUrl("/product/category/update"),
          method: "post",
          //data: this.$http.adornData(this.category, false),
          data: this.$http.adornData(data, false),
        }).then(({ data }) => {
          this.$message({
            message: "菜单修改成功",
            type: "success",
          });
          this.dialogVisible = false;
          //删除成功，刷新出新的菜单
          this.getMenus();
          //设置默认删除刷新完展开的菜单
          this.expandedKey = [this.category.parentCid];
        });
      },


      remove(node, data) {
        let ids = [data.catId];
        this.$confirm(`是否删除【${data.name}】菜单, 是否继续?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$message({
            message: '菜单删除成功',
            type: 'success'
          });
          this.$http({
            url: this.$http.adornUrl('/product/category/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false),
          }).then(({data}) => {
            console.log("删除成功", data);
            //刷新出新的菜单
            this.getMenus();
            //默认要展示的菜单(删除节点父节点的id)
            this.expandedKey=[node.parent.data.catId];
          })
        }).catch(()=>{

        })

        console.log("remove", node, data);
      }
    },

    //生命周期 - 创建完成（可以访问当前this实例）
    created() {
      this.getMenus();
    }
  }
</script>
<style scoped>
</style>

```

### 菜单拖动实现

element-ui tree组件：https://element.eleme.cn/#/zh-CN/component/tree

- allow-drop拖拽时判定目标节点能否被放置
- 被拖动的当前节点以及所在的父节点总层数不能大于3

| 同一个菜单内拖动                 | 正常 |
| -------------------------------- | ---- |
| 拖动到父菜单的前面或后面         | 正常 |
| 拖动到父菜单同级的另外一个菜单中 | 正常 |

关注的焦点在于，拖动到目标节点中，使得目标节点的catlevel+deep小于3即可。

### 拖拽条件与修改顺序/级别(难点)

**1）拖拽与数据库关联的内容：**(拖拽会影响到数据库的三个字段)

- catLevel
- parentCid
- sort

**2）拖拽相关函数**：

```
<el-tree
         :data="menus"  绑定的变量
         :props="defaultProps" 配置选项
         :expand-on-click-node="false"  只有点击箭头才会展开收缩
         show-checkbox 显示多选框
         node-key="catId" 数据库的id作为node id
         :default-expanded-keys="expandedKey" 默认展开的数组
         :draggable="draggable" 开启拖拽功能
         :allow-drop="allowDrop"  是否允许拖拽到目标结点，函数为Function(draggingNode源结点, dropNode目标结点, type前中后类型)
         @node-drop="handleDrop"  拖拽成功处理函数，函数为Function(draggingNode源结点, dropNode拖拽成功后的父结点, type前中后类型)
         ref="menuTree"
         >

```

**3）函数参数：**

- `draggingNode`：正在拖拽的结点
- `dropNode`：拓展成功后的父节点，我们把他称为**目的父节点**
- `type`：分为before、after、inner。拖拽到某个结点上还是两个结点之间

**4）先了解一下如何获取结点的深度**

需要监听拖拽事件

```javascript
在拖拽的时候首先会自动调用allowDrop()函数，他在第一句就调用了this.countNodeLevel(draggingNode);
-----------------------------------------;
// countNodeLevel()函数的作用是遍历拖拽结点的【子节点】，找到其中的最大层级
countNodeLevel(node) {
    //找到所有子节点，求出最大深度
    if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
            if (node.childNodes[i].level > this.maxLevel) {
                // 是赋值给了共享变量maxLevel
                this.maxLevel = node.childNodes[i].level;
            }
            // 递归子节点
            this.countNodeLevel(node.childNodes[i]);
        }
    }
}
----------------------------------------;
找到了拖拽结点的最大层级(深度)，那么就可以计算拖拽结点作为根节点的子树深度deep。;
另外注意maxLevel每次拖拽都会更新，是拖拽结点的最大层级;
let deep = Math.abs(this.maxLevel - draggingNode.level) + 1; 
// draggingNode为正在拖拽的结点

```

**5）拖拽合法性**

我们得到了子树的深度deep，就可以判断这个拖拽合不合法：

拖拽类型：以拖拽后新的父结点为基准分为：

- 结点前、后（两个结点之间）：

  ```
  deep + dropNode.parent.level <= 3;
  ```

- 中（结点上）：

  ```
  deep + dropNode.level <= 3;
  ```

**6）拖拽合法后的操作**

* 先得到拖拽成功后的父节点id、父节点新的子结点（包含了拖拽结点）

* 准备一个update[]数组，有变化的保存到里面，最后提交到数据库。（会变化的有新兄弟结点和拖拽子节点）

  * 遍历子节点for i
    非draggingNode结点直接push(兄弟结点id，排序)：this.updateNodes.push({ catId: siblings[i].data.catId, sort: i });
  * 是draggingNode结点更新其父节点和sort
    * 还需要更新子节点的level，因为element-ui已经提供了level，我们只需将新的level保存到update中，最后也发送到数据库中即可。

* 保存提交到数据库，弹出成功窗口。

* 刷新菜单，展开对于层级。只需要赋值给expandedKey即可

* 为了防止下次拖拽还有上回的updateNodes信息，所以操作完应该恢复原始状态

  

**7）更改分类controller**
对于后端更新数据库，加入controller。用postman测试

```java
/**
     * 批量修改层级
     {["catId":1,"sort":0],["catId":2,"catLevel":2]}
*/
@RequestMapping("/update/sort")
public R updateSort(@RequestBody CategoryEntity[] category){
    categoryService.updateBatchById(Arrays.asList(category));
    return R.ok();
}
```

**8）拖拽开关**

为了防止误操作，我们通过edit把拖拽功能开启后才能进行操作。所以添加switch标签，操作是否可以拖拽。我们也可以体会到el-switch这个标签是一个开关

希望整个界面，需要拖动功能的时候才拖动

```javascript
<template>
  <div>
    <el-switch v-model="draggable" active-text="开启拖拽" inactive-text="关闭拖拽"></el-switch>
    <el-button v-if="draggable" @click="batchSave">批量保存</el-button>
    <el-button type="danger" @click="batchDelete">批量删除</el-button>
    <!-- 把menus给data -->
    <el-tree
             :draggable="draggable"

```

**9）批量保存**
但是现在存在的一个问题是每次拖拽的时候，都会发送请求，更新数据库这样频繁的与数据库交互



现在想要实现一个拖拽过程中不更新数据库，拖拽完成后，统一提交拖拽后的数据。

<el-button v-if="draggable" @click="batchSave">批量保存</el-button>

* v-if是指开启开关后才显示
* 开启拖拽后应该使用的是node信息，而不是数据库信息，因为还没同步到数据库。把相关的信息都修改
* 之前为了防止上次数据遗落，归零了展开列表，这样列表又不展开了

现在还存在一个问题，如果是将一个菜单连续的拖拽，最终还放到了原来的位置，但是updateNode中却出现了很多节点更新信息，这样显然也是一个问题。

**10）批量删除与调用内置函数**

```
<el-button type="danger" @click="batchDelete">批量删除</el-button>
```

getCheckedNodes()返回当前选中的所有结点

如何调用内

```
<el-tree
         。。。
         ref="menuTree"
         />
然后在js里
this.$refs.menuTree.getCheckedNodes();
他有两个参数，默认的是我们想要用的

```

- 确认框
- 确认后发送ajax
- 刷新菜单



前端分类菜单拖拽功能全部实现：

```vue
<template>
<div>
  <el-switch v-model="draggable" active-text="开启拖拽" inactive-text="关闭拖拽"></el-switch>
  <el-button v-if="draggable" @click="batchSave">批量保存</el-button>
  <el-button type="danger" @click="batchDelete">批量删除</el-button>
  <el-tree 
    :data="menus" 
    :props="defaultProps" 
    :expand-on-click-node="false" 
    show-checkbox node-key="catId"
    :default-expanded-keys="expandedKey"
    :draggable="draggable"
    :allow-drop="allowDrop"
    @node-drop="handleDrop"
    ref="menuTree"
  >
    <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <!-- 只有一级和二级分类有添加，三级或者没有子节点的分类有删除-->
          <el-button v-if="node.level<=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button type="text" size="mini" @click="() => modify(data)">Modify</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini" @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
  </el-tree>

  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="30%"
    :close-on-click-modal="false"
  >
  <!--嵌套表单-->
    <el-form :model="category"><!--绑定表单-->
      <el-form-item label="分类名称">
        <el-input v-model="category.name" autocomplete="off"></el-input>
      </el-form-item>
      <el-form-item label="图标">
          <el-input
            v-model="category.icon"
            autocomplete="off"
          ></el-input>
        </el-form-item>

        <el-form-item label="计量单位">
          <el-input
            v-model="category.productUnit"
            autocomplete="off"
          ></el-input>
        </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitData">确 定</el-button>
    </span>
  </el-dialog>
</div>
  
</template>

<script>
  export default {
  //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        pCid: [],
        draggable: false,
        updateNodes: [],

        maxLevel: 0,
        menus: [],
        expandedKey:[],
        category: {
          name:"",
          parentCid: 0,
          catLevel: 0,
          showStatus: 1,
          sort: 0,
          catId: null,
          icon: "",
          productUnit: ""
        },
        dialogVisible: false,
        dialogType: "",  // add, modify
        title: "",
        defaultProps: {
          children: 'children',
          label: 'name'
        }
      };
    },
    methods: {
      allowDrop(draggingNode, dropNode, type) {
        console.log("allowDrop:", draggingNode, dropNode, type);
        // 1、被拖动的当前节点以及所在的父节点总层数不能大于3

        // 1）计算出被拖动节点的当前节点总层数
        // 求出当前节点最大深度（maxLevel）
        this.countNodeLevel(draggingNode);
        // 深度 = 最大深度 - 当前深度 + 1
        let deep = Math.abs(this.maxLevel - draggingNode.level) + 1;
        console.log('深度：', deep);

        // 当前正在拖动的节点 + 父节点所在的深度不大于3即可
        if (type == "inner") {
          return (deep + dropNode.level) <= 3;
        } else {
          return (deep + dropNode.parent.level) <= 3;
        }
      
      },
      // 找到所有子节点，求最大深度(递归)
      countNodeLevel(node) {
        if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (node.childNodes[i].level > this.maxLevel) {
            this.maxLevel = node.childNodes[i].level
          }
          this.countNodeLevel(node.childNodes[i])
        }
      }
      },
      
      // 拖拽成功后触发
      handleDrop (draggingNode, dropNode, dropType, ev) {
        console.log('handleDrop: ', draggingNode, dropNode, dropType)
        // 1、当前节点最新的父节点id
        let pCid = 0
        let siblings = null
        if (dropType === 'before' || dropType === 'after') {
          pCid =
            dropNode.parent.data.catId === undefined
              ? 0
              : dropNode.parent.data.catId
          siblings = dropNode.parent.childNodes
        } else {
          pCid = dropNode.data.catId
          siblings = dropNode.childNodes
        }
        this.pCid.push(pCid)
        // 2、当前拖拽节点的最新顺序，
        for (let i = 0; i < siblings.length; i++) {
          if (siblings[i].data.catId === draggingNode.data.catId) {
            // 如果遍历的是当前正在拖拽的节点
            let catLevel = draggingNode.level
            if (siblings[i].level !== draggingNode.level) {
              // 当前节点的层级发生变化
              catLevel = siblings[i].level
              // 修改他子节点的层级
              this.updateChildNodeLevel(siblings[i])
            }
            this.updateNodes.push({
              catId: siblings[i].data.catId,
              sort: i,
              parentCid: pCid,
              catLevel: catLevel
            })
          } else {
            this.updateNodes.push({ catId: siblings[i].data.catId, sort: i })
          }
        }
        // 3、当前拖拽节点的最新层级
        console.log('updateNodes', this.updateNodes)
      },

      // 递归修改node子节点层级
      updateChildNodeLevel (node) {
        if (node.childNodes.length > 0) {
          for (let i = 0; i < node.childNodes.length; i++) {
            var cNode = node.childNodes[i].data
            this.updateNodes.push({
              catId: cNode.catId,
              catLevel: node.childNodes[i].level
            })
            this.updateChildNodeLevel(node.childNodes[i])
          }
        }
      },

      batchDelete () {
        let catIds = []
        let checkedNodes = this.$refs.menuTree.getCheckedNodes()
        console.log('被选中的元素', checkedNodes)
        for (let i = 0; i < checkedNodes.length; i++) {
          catIds.push(checkedNodes[i].catId)
        }
        this.$confirm(`是否批量删除【${catIds}】菜单?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
          .then(() => {
            this.$http({
              url: this.$http.adornUrl('/product/category/delete'),
              method: 'post',
              data: this.$http.adornData(catIds, false)
            }).then(({ data }) => {
              this.$message({
                message: '菜单批量删除成功',
                type: 'success'
              })
              this.getMenus()
            })
          })
          .catch(() => {})
      },

      batchSave () {
        this.$http({
          url: this.$http.adornUrl('/product/category/update/sort'),
          method: 'post',
          data: this.$http.adornData(this.updateNodes, false)
        }).then(({ data }) => {
          this.$message({
            message: '菜单顺序等修改成功',
            type: 'success'
          })
          // 刷新出新的菜单
          this.getMenus()
          // 设置需要默认展开的菜单
          this.expandedKey = this.pCid
          this.updateNodes = []
          this.maxLevel = 0
          //this.pCid = [];
        })
      },

      getMenus() {
        this.$http({
          url: this.$http.adornUrl('/product/category/list/tree'),
          method: 'get',
        }).then(({data})=> {
          console.log("成功获取到菜单数据。。。", data.data)
          this.menus = data.data
        })
      },
      append(data) {
        // 要清空，改为默认值
        console.log("append",data)
        this.dialogVisible = true;  // 打开对话框
        this.dialogType = "add";
        this.title = "Add Category";
        this.category.parentCid = data.catId;
        this.category.catLevel = data.catLevel * 1 + 1;// str转int再计算，append的下一级为创建level
        this.category.name = "";
        this.category.catId = null;
        this.category.icon = "";
        this.category.productUnit ="";
        this.category.showStatus = 1;
        this.category.sort = 0;

      },
      // 修改菜单
      modify(data) {
        console.log("要修改的数据", data);
        this.dialogType = "modify";
        this.title = "Modify Category";
        this.dialogVisible = true;   
        
        //发送请求获得当前节点最新数据
        this.$http({
          url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
          method: "get",
        }).then(({ data }) => {
          //请求成功
          console.log("要回显的数据", data);
          this.category.name = data.data.name;
          this.category.catId = data.data.catId;
          this.category.icon = data.data.icon;
          this.category.productUnit = data.data.productUnit;
          this.category.parentCid = data.data.parentCid;
          this.category.catLevel = data.data.catLevel;
          this.category.showStatus = data.data.showStatus;
          this.category.sort = data.data.sort;
        });
      },
      submitData() {
        if (this.dialogType == "add") {
          this.addCategory();
        }
        if (this.dialogType == "modify") {
          this.modifyCategory();
        }
      },
      // 添加三级分类
      addCategory(){
        console.log("提交的三级分类数据", this.category);
        this.$http({
          url: this.$http.adornUrl("/product/category/save"),
          method: "post",
          data: this.$http.adornData(this.category, false),
        }).then(({ data }) => {
          this.$message({
            message: "The menu saved successfully",
            type: "success",
          });
          //关闭对话框
          this.dialogVisible = false;
          //刷新展开
          this.getMenus();
          //设置默认删除刷新完展开的菜单
          this.expandedKey = [this.category.parentCid];
        });

      },
      // 修改三级分类数据
      modifyCategory() {
        var {catId, name, icon, productUnit} = this.category;
        var data = {catId, name, icon, productUnit};
        this.$http({
          url: this.$http.adornUrl("/product/category/update"),
          method: "post",
          //data: this.$http.adornData(this.category, false),
          data: this.$http.adornData(data, false),
        }).then(({ data }) => {
          this.$message({
            message: "菜单修改成功",
            type: "success",
          });
          this.dialogVisible = false;
          //删除成功，刷新出新的菜单
          this.getMenus();
          //设置默认删除刷新完展开的菜单
          this.expandedKey = [this.category.parentCid];
        });
      },


      remove(node, data) {
        let ids = [data.catId];
        this.$confirm(`是否删除【${data.name}】菜单, 是否继续?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$message({
            message: '菜单删除成功',
            type: 'success'
          });
          this.$http({
            url: this.$http.adornUrl('/product/category/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false),
          }).then(({data}) => {
            console.log("删除成功", data);
            //刷新出新的菜单
            this.getMenus();
            //默认要展示的菜单(删除节点父节点的id)
            this.expandedKey=[node.parent.data.catId];
          })
        }).catch(()=>{

        })

        console.log("remove", node, data);
      }
    }
    ,
    //生命周期 - 创建完成（可以访问当前this实例）
    created() {
      this.getMenus();
    }
  }
</script>
<style scoped>
</style>

```


