### logback-dendrobe-apollo

[![Build](https://github.com/CharLemAznable/logback-dendrobe-apollo/actions/workflows/build.yml/badge.svg)](https://github.com/CharLemAznable/logback-dendrobe-apollo/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.charlemaznable/logback-dendrobe-apollo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.charlemaznable/logback-dendrobe-apollo/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
![GitHub code size](https://img.shields.io/github/languages/code-size/CharLemAznable/logback-dendrobe-apollo)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=alert_status)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=bugs)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=security_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=sqale_index)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=code_smells)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=ncloc)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=coverage)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-apollo&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-apollo)

使用Apollo热更新[logback-dendrobe](https://github.com/CharLemAznable/logback-dendrobe)配置.

##### Maven Dependency

```xml
<dependency>
  <groupId>com.github.charlemaznable</groupId>
  <artifactId>logback-dendrobe-apollo</artifactId>
  <version>2023.0.3</version>
</dependency>
```

##### Maven Dependency SNAPSHOT

```xml
<dependency>
  <groupId>com.github.charlemaznable</groupId>
  <artifactId>logback-dendrobe-apollo</artifactId>
  <version>2023.0.4-SNAPSHOT</version>
</dependency>
```

#### 本地配置需要读取的Apollo配置坐标

在本地类路径默认配置```logback-dendrobe.properties```文件中, 添加如下配置:

```
logback.apollo.namespace=XXX
logback.apollo.propertyName=YYY
```

即指定使用Apollo配置```namespace:XXX property:YYY```热更新logback-dendrobe配置.

```logback.apollo.namespace```配置默认值: Logback
```logback.apollo.propertyName```配置默认值: default

#### 使用Apollo配置logback-dendrobe数据库日志的Eql连接

当配置数据库日志为```{logger-name}[eql.connection]=XXX```时, 读取Apollo配置```namespace:EqlConfig property:XXX```作为Eql连接配置.

#### 使用Apollo配置logback-dendrobe Vert.x日志的Vert.x实例

当配置Vert.x日志为```{logger-name}[vertx.name]=XXX```时, 读取Apollo配置```namespace:VertxOptions property:XXX```作为Vert.x实例配置.

#### 使用Apollo配置logback-dendrobe ElasticSearch日志的es客户端

当配置ElasticSearch日志为```{logger-name}[es.name]=XXX```时, 读取Apollo配置```namespace:EsConfig property:XXX```作为es客户端配置.
