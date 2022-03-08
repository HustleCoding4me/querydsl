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

<summary> <h1>정렬, 페이징, 조인 </h1> </summary>
  
### QueryDsl 정렬 예시

```java
/**
* 회원 정렬
* 1. 회원 나이 내림차순(desc)
* 2. 회원 이름 올림차순(asc)
* 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last) 대박
*/
@Test
public void sort() throws Exception {
        List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();
        //나이가 100살의 멤버중, 나이로 내림차순, 이름으로 올림차순인데 null이 마지막으로 뽑기
        //null을 먼저 뽑는 것도 있다. nullsFirst()
        /*
        select member1
        from Member member1
        where member1.age = 1001
        order by member1.age desc, member1.username asc nulls last
         */
    }

```



### QueryDsl 페이징 예시

```java
 @Test
    public void paging() throws Exception {
        //방법 1 그냥 fetch()하여 결과 리스트만 가져오기
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)//1개 넘겨서
                .limit(2)//2개 들고오는데
                .fetch();

        //방법 2 count + 결과 조회해주는 fetchResult로 실행.
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)//1개 넘겨서
                .limit(2)//2개 들고오는데
                .fetchResults();

    }

```

> 💥 fetchResult는 counting 쿼리에도 fetch와 똑같은 조건을 가져오기 때문에
> 실무에서는 count 따로, fetch 따로 해준다.



### 특정 값으로 select 시

> Dto를 사용하지 않으면 Tuple이라는 queryDsl이 제공해주는 객체로 담게 된다.

```java

    @Test
    public void aggregation() throws Exception {
        //원하는 정보를 꺼내고 싶을 때는 QueryDsl의 Tuple로 꺼낸다.
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        System.out.println(tuple.get(member.count()));
        System.out.println(tuple.get(member.age.sum()));
        System.out.println(tuple.get(member.age.avg()));
        System.out.println(tuple.get(member.age.max()));
        System.out.println(tuple.get(member.age.min()));

        /* select count(member1)
        , sum(member1.age)
        , avg(member1.age)
        , max(member1.age)
        , min(member1.age)
        from Member member1 */

        //data 타입이 여러개로 들어올 때는 Tuple을 쓰면 된다. 실무에서 잘 쓰지는 않고 Dto를 사용하지만 참고
    }
```

### 기본 join과 groupBy, Having 
  
```java

/**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void groupBy() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(team.name.ne("team3"))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        System.out.println(teamA.get(team.name));
        System.out.println(teamA.get(member.age.avg()));
        System.out.println(teamB.get(team.name));
        System.out.println(teamB.get(member.age.avg()));

        /* select team.name, avg(member1.age)
        from Member member1
        inner join member1.team as team
        group by team.name
        having team.name <> 'team3'1 */

    }
```

</details>

<details>

<summary> <h1> join + on절 + fetch join </h1> </summary>

### 기본적인 상관관계 Entity Join

> 기본적인 queryDsl 객체를 사용한 join 사용법<br>
>> public <P> Q leftJoin(EntityPath<P> target, Path<P> alias)<br> 
>> join을 걸고싶은 Entity의 연관관계 대상과 alias를 적어준다. (물론 QueryDsl의 Q객체들) 
>> member.team처럼 내부 선언 연관관계 대상을 join에 적어주면 알아서 Team Table에서 outerJoin한다.


```java
/**
     *
     * Team A에 속한 모든 회원을 leftjoin하는 방법.
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team) 
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

          /* select
            member1
        from
            Member member1
        left join
            member1.team as team
        where
            team.name = ?1 */
        
        /*
          실 작동 쿼리
          
           select
            member0_.user_id as user_id1_0_,
            member0_.age as age2_0_,
            member0_.team_id as team_id4_0_,
            member0_.username as username3_0_ 
        from
            member member0_ 
        left outer join
            team team1_ 
                on member0_.team_id=team1_.team_id 
        where
            team1_.name=?      
                
                
         */
        
    }
```

### 상관관계 없는 Entity끼리 Join

#### 방법 1. thetajoin
#### 방법 2. on절 이용해 조건걸기

1. thetajoin

> 위의 예시처럼 Member - Team 의 N대1 , Member에 선언된 team처럼 연관관계가 없어도 join이 가능하다.<br>
>> 연관관계가 없는 entity끼리 join을 하는 것을  `thetajoin`이라고 한다.
>> 그냥 from절에 나열해주면 된다.

```java
/**
 * 회원의 이름이 팀 이름과 같은 회원 조회
 */
@Test
public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

        assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
        
        /*
        select
            member0_.user_id as user_id1_0_,
            member0_.age as age2_0_,
            member0_.team_id as team_id4_0_,
            member0_.username as username3_0_ 
        from
            member member0_ cross 
        join
            team team1_ 
        where
            member0_.username=team1_.name
         */
}

```
### CrossJoin의 문제점과 해결
> Cross Join이란
>> 💥 모든 회원을 가져오고 모든 팀을 가져와서 다 join (`Cross Join : 집합에서 나올 수 있는 모든 경우`), 이후 where 에서 필터링 한다.<br>
>> ` from member member0_ cross` 에서 알 수 있듯이 Cross join을 실시했다. 
>> (db에서 자동으로 최적화를 진행하지만 당연히 일반 join보다 성능이 좋지 않다.)
> 원인
>> 별도의 join 없이 from에서 선언한 Table을 where에서 사용한 `암묵적 조인` 은 `Hibernate`가 `CrossJoin`을 하는 경향이 있다. 
> 해결책
>> 명시적 Join으로 수정하면 된다.

> 💥 `thetaJoin`은 outer 조인이 불가능하다. -> 최근에는 on을 사용하여 outer Join도 가능하게 추가되었다.

2.on절 이용해 조건 걸기

> On절 사용법<br>
1. 연관관계 없는 Entity 외부 조인 (일명 막조인)
2. join 대상 필터링

> 1. 연관관계 없는 Entity 외부 조인 (일명 막조인)
>> 흔히 말하는 막Join이다. 연관관계가 없는 두 Entity를 Join하는 방식이기 때문에,<br>
>> 연관관계 객체를 넣어주지 않으면 ex).leftJoin(member.team)이 아닌,<br>  
>> leftJoin(team) id값으로 매칭을 해주는 기존 연관관계 join과 다르게, 순수 on절의 조건으로 매칭을 시킨다. <br>
>> 해당 예시에서는 on(member.username.eq(team.name)) => MemberEntity, TeamEntity 조인하고 memberUsername으로 거른다.<br>
> 
>> 💥 주의, 막조인이기 때문에 join에 연관관계 Entity가 아니라 단독으로 들어간다.<br>
>>  일반조인 : `leftJoin(member.team,team)`
>>  on 조인 (막조인) : `.join(team).on(member.username.eq(team.name))`

```java
    /*
     * 회원의 이름이 팀 이름과 같은 대상 찾기
     */
    @Test
public void join_on_no_relation() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

        List<Tuple> result
                = queryFactory
                .select(member, team)
                .from(member)
                .join(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
        /*
       select
            member0_.user_id as user_id1_0_0_,
            team1_.team_id as team_id1_1_1_,
            member0_.age as age2_0_0_,
            member0_.team_id as team_id4_0_0_,
            member0_.username as username3_0_0_,
            team1_.name as name2_1_1_
        from
            member member0_
        inner join
            team team1_
                on (
                    member0_.username=team1_.name
                )
         */

        /*
        결과

        [Member(id=10, username=teamA, age=0), Team(id=1, name=teamA)]
        [Member(id=11, username=teamB, age=0), Team(id=2, name=teamB)]
         */

}

```

> 2.join 대상 필터링

```java
/**
     * ex ) 회원과 팀을 조인하면서, 팀 이름이 'teamA'인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void joinOnFiltering() throws Exception {
        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        **innerJoin 할거면 그냥 where절을 쓰는게 낫다.**

        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();


        /*
        select
            member0_.user_id as user_id1_0_0_,
            team1_.team_id as team_id1_1_1_,
            member0_.age as age2_0_0_,
            member0_.team_id as team_id4_0_0_,
            member0_.username as username3_0_0_,
            team1_.name as name2_1_1_
        from
            member member0_
        left outer join
            team team1_
            
               join 후 on절의 조건에 추가되는 모습. 걸러진다.
               ************************
                on member0_.team_id=team1_.team_id
                and (
                    team1_.name=?
                )
                **********************
         */
    }

```






</details>

<details>

<summary> <h1>QueryDsl 설정방법 </h1> </summary>
  

  
</details>
