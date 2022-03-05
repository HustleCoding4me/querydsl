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

<summary> <h1>QueryDsl 기본 사용법, 검색 유형별 예시 </h1> </summary>

### JPAQueryFactory(EntityManager)로 query를 만든다.

> 기존 JPQL로 제작할 때,

```java
@Test
    public void startJPQL() throws Exception {
        //member1을 찾아라
        Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findByJPQL.getUsername()).isEqualTo("member1");
    }
```

> QueryDsl을 도입하게 되면?
>> java로 쿼리를 실행시킬 수 있어, Runtime때 오류를 파악할 수 있었던
>> JPQL과는 다르게 Runtime시에 오류를 잡아낼 수 있다. (오타라던지)
>> 또한 파라미터 바인딩도 자동으로 해준다.
 
```java
@Test
    public void startQuerydsl() throws Exception {
        //member1을 찾아라
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
```

> 기본적으로 제공되는 메서드는 거의 SQL기능과 동일하다.

```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'
member.username.isNotNull() //이름이 is not null
member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30
member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30
member.username.like("member%") //like 검색
member.username.contains("member") // like ‘%member%’ 검색
member.username.startsWith("member") //like ‘member%’ 검색

```
### 검색 유형

> `chain and 조건`

```java
//chain and 조건
    @Test
    public void search() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
```

> `, and 조건`

```java
//, and 조건
    @Test
    public void searchAndParam() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
```

> `(e1 or e2) and e3`

```java
// 구현 : e1.or(e2).and(e3))
    // 실 동작 쿼리 : (e1 or e2) and e3
    @Test
    public void searchTest1() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where((member.username.eq("member1").or(member.age.eq(10)))
                        ,member.age.goe(10)
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
        
        /* select member1
        from Member member1
        where (member1.username = 'member1'1 or member1.age = 102) and member1.age >= 103 */
    }
```

> `(e1 or e2) and (e3 or e4)`

```java
// 구현 : e1.or(e2).and(e3.or(e4)))
    // 실 동작 쿼리 : (e1 or e2) and (e3 or e4)
    @Test
    public void searchTest2() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1").or(member.age.eq(10)).and(member.age.goe(10).or(member.age.goe(5)))
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
        
        /* select member1
        from Member member1
        where (member1.username = ?1 or member1.age = ?2) and (member1.age >= ?3 or member1.age >= ?4) */
    }
```

> `(e1 and e2) or (e3 and e4)`
>> ✨ 유의사항
>> SQL에서 and가 상위 (먼저 실행) 명령이라 나가는 쿼리는 `e1 and e2 or e3 and e4`
>> But, 실 동작은 `(e1 and e2) or (e3 and e4)` 이렇게 된다.
```java
// 구현 : e1.and(e2).or(e3.and(a4))
// 실 동작 쿼리 : e1 and e2 or e3 and e4
    @Test
    public void searchTest3() throws Exception {
        JPAQuery<Member> member1 = queryFactory.selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)).or(member.age.goe(10).and(member.age.goe(5)))
                ).fetchAll();
        Member findMember = member1.fetchFirst();
        assertThat(findMember.getUsername()).isEqualTo("member1");
        
        /* select member1
        from Member member1 fetch all properties
        where member1.username = 'member1'1 and member1.age = 102 or member1.age >= 103 and member1.age >= 54  */
        
    }
```


</details>

<details>

<summary> <h1>Querydsl 다양한 Fetch 결과 </h1> </summary>
  
```java

  @Test
    public void resultFetch() throws Exception {
       
        //fetch
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
        
        //fetchOne
        queryFactory
                .selectFrom(member)
                .fetchOne();

        //fetchFirst
        queryFactory
                .selectFrom(member)
                .fetchFirst();

        //fetchResult 페이징,count 포함
        QueryResults<Member> pagingResult = queryFactory
                .selectFrom(member).limit(5).offset(0)
                .fetchResults();

        pagingResult.getTotal();
        List<Member> data = pagingResult.getResults();
        long limit = pagingResult.getLimit();
        long offset = pagingResult.getOffset();

        for (Member m : data) {
            System.out.println("============Member : " + m);
        }
        //Paging처럼 count 쿼리까지 나간다.
        /* select count(member1)
        from Member member1 */
        /* select member1
        from Member member1 */

          
        //fetchCount count만 가져오기
        long couint = queryFactory
                .selectFrom(member)
                .fetchCount();
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
