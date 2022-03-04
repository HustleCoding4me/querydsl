<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  
> `QueryDsl`은 기존 dependency들을 추가하는 경우와 다르게 설정파일들을 조금 손봐줘야한다.
> 최신 스프링5 버전에서 사용하기 위해 설정하지 않으면
> `Unable to load class 'com.mysema.codegen.model.Type'` compileQuerydsl 에러가 난다.
  
  
1. plugin 추가
2. 라이브러리 추가
3. 각종 dir, config   
  
```properties
  
  
**************************************************************
buildscript {
	ext {
		queryDslVersion = "5.0.0"
	}
}
***************************************************************
  
  
plugins {
	id 'org.springframework.boot' version '2.6.4'
	id 'io.spring.dependency-management' version '1.0.11.RELEASE'
  
  1. plugin 추가
	**************************************************************
	id 'com.ewerk.gradle.plugins.querydsl' version '1.0.10'
  **************************************************************

	id 'java'
}

group = 'study '
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.7'
	compileOnly 'org.projectlombok:lombok'

  
  2.라이브러리 추가
	**************************************************************
	implementation "com.querydsl:querydsl-jpa:${queryDslVersion}"
	implementation "com.querydsl:querydsl-apt:${queryDslVersion}"
  **************************************************************

	runtimeOnly 'com.h2database:h2'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
	useJUnitPlatform()
}


 3. 각종 dir, config 
**************************************************************
def querydslDir = "$buildDir/generated/querydsl"

querydsl {
	jpa = true
	querydslSourcesDir = querydslDir
}
sourceSets {
	main.java.srcDir querydslDir
}
compileQuerydsl{
	options.annotationProcessorPath = configurations.querydsl
}
configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
	querydsl.extendsFrom compileClasspath
}
**************************************************************
  
```  
> compileQuerydsl을 돌리면 3번에 설정해놓은 설정파일대로 해당 경로에 QueryDsl 전용 Entity를 생성해준다.  
  
![image](https://user-images.githubusercontent.com/37995817/156384544-5d6d53b1-271a-4bda-ac8f-38b0048f3ac7.png)
  
> "$buildDir/generated/querydsl"에 생성된 모습
  
  ![image](https://user-images.githubusercontent.com/37995817/156384811-46c66359-0916-49db-8943-94ee27b44cea.png)


>> 터미널에서 명령어로도 가능

```linux
  
./gradlew clean

./gradlew compileQuerydsl

```
  
### QueryDsl 잘 적용됐는지 Test하기  

1. 간단하게 Hello Entity 생성
2. QuerydslCompile시에 생긴 QHello 확인
3. query 실행을 위임할 `Querydsl`의 `JPAQueryFactory` 객체로 쿼리 호출

  
  
```java
//1. 간단하게 Hello Entity 생성
  
package study.querydsl.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter @Setter
public class Hello {

    @GeneratedValue @Id
    private Long id;

}
```
  
```java
//test코드 작성  
  
@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = QHello.hello;

		//쿼리와 관련된 것은 compileQuerydls을 돌려 만든
		//qEntity를 사용해야 한다.
		Hello result = query.selectFrom(qHello)
				.fetchOne();

		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}

}


```  
  
  
  
  
</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>
