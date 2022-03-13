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


### Fetch Join

> fetchJoin 미사용시

>> @PersistenceUnit <br>
>> EntityManagerFactory emf;
>
>> 이 어노테이션은 PersistenceUnitUtil을 가져와 Entity의 변수가 Loading이 되었는지 아닌지 check할 수 있다.
>> boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());



```java

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //LAZY LOADING이기 때문에, team은 조회가 안된다.
        //PersistenceUnitUtil로 객체인지 Proxy인지 구별할 수 있다. 주로 테스트에서 많이 쓰임
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치조인 미적용으로 TEAM LAZYLOADING PROXY객체임").isFalse();
    }
```

> fetchJoin 사용시
>
>> 기존 join처럼 쓰는데, 뒤에 .fetchJoin을 붙이면 한번에 가져오게 된다.
>> .join(member.team, team).fetchJoin()

```java
    @Test
    public void fetchJoinYes() throws Exception {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치조인 미적용으로 TEAM LAZYLOADING PROXY객체임").isTrue();
        
        /*
            select
        member1 
    from
        Member member1   
    inner join
        fetch member1.team as team 
    where
        member1.username = ?1  
        */
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
        on member0_.team_id=team1_.team_id
        where
        member0_.username=?
        
         */
    }


```



</details>





<details>

<summary> <h1>[QueryDsl] 서브쿼리 사용하기 </h1> </summary>

### 앞서서, 서브쿼리 사용시 생각해야할 점

> 현재 from절에서 서브쿼리가 불가능하다.
>
> `원인`
>> JPA JPQL에서 from 절의 서브쿼리를 지원하지 않기 때문에, JPQL 기반의 QueryDsl도 지원되지 않는다.<br>
>
>> 하이버네이트 구현체를 사용하면(`JPAExpressions`) select절의 서비 쿼리는 지원한다.
>
> from절의 서브쿼리 해결 방안 3가지로는
>> 1. 서브쿼리를 join으로 변경 시도
>
> > 2. app에서 쿼리를 분리해서 2번 실행하여 거름
>
> > 3. nativeSQL을 사용하기
>
> 💢그러나 from절에서 서브쿼리를 사용하는 많은 이유 두 가지는
>> 1. 화면에 완전 Fit하게 가져오기 위해
>
>> 2. 성능상의 이유로 단 한번의 query만 날려 가져오게 하기 위해
>
> 정도로 구분할 수 있는데, 과연 `DB에서 순수하게 Data를 가져오는 역할을 시켜서 재사용성을 높히는 설계 `와, <br>
> `걸러내는 로직을 Server에서 한다` 를 포기할 만한 가치가 있는가 생각해보자. <br>
>
> 또한, from 서브쿼리로 하나에 1000줄 짤거, sql을 두 세번 날리면 각각 100줄정도로 나눌 수 있는데<br>
> 그렇게까지 쿼리 한두번이 아쉬울 정도의 고성능을 요구하려면 이미 cache나 다른 조치를 취해야 하는게 맞다.


### JPAExpressions를 사용해 서브쿼리 제작하기

1. where절에 서브쿼리
> 예시 1. where 절 innerjoin으로 나이 가장 많은 회원 조회하기
>> 💫주의사항 : 서브쿼리용 Entity는 alias가 달라야하기 때문에 따로 QMember 생성해준다.
>> JPAExpressions를 static import로 빼면 코드가 더 간결해진다.

```java
 @Test
    public void subQuery() throws Exception {
        *****
        //Member InnerJoin을 위해 alias를 새로 선언해서 QMember 생성해주는 모습
        *****
        QMember subMember = new QMember("subMember");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(subMember.age.max())
                                .from(subMember)
                )).fetch();

        assertThat(result).extracting("age");
    
          /* select
        member1 
    from
        Member member1 
    where
        member1.age = (
            select
                max(subMember.age) 
            from
                Member subMember
        ) */ 
        
        /*select
        member0_.user_id as user_id1_0_,
                member0_.age as age2_0_,
        member0_.team_id as team_id4_0_,
                member0_.username as username3_0_
        from
        member member0_
        where
        member0_.age=(
                select
        max(member1_.age)
        from
        member member1_
            )*/
    }

```

> 예시 2. 나이 평균이상 멤버만 구하기

```java
 @Test
    public void subQuery_avg() throws Exception{
        //서브쿼리용 Entity는 alias가 달라야하기 때문에 따로 QMember 생성해준다.
        QMember subMember=new QMember("subMember");
        List<Member> result=queryFactory
        .selectFrom(member)
        .where(member.age.goe(
        JPAExpressions
        .select(subMember.age.avg())
        .from(subMember)
        )).fetch();
        }
```

> 예시 3. 💫유용한 서브쿼리로 & in으로 조회

```java
@Test
    public void subQuery_in() throws Exception {
        //서브쿼리용 Entity는 alias가 달라야하기 때문에 따로 QMember 생성해준다.
        QMember subMember = new QMember("subMember");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subMember.age)
                                .from(subMember)
                                .where(subMember.age.gt(10))
                )).fetch();

        assertThat(result).extracting("age");
 /* select
        member1
    from
        Member member1
    where
        member1.age in (
            select
                subMember.age
            from
                Member subMember
            where
                subMember.age > ?1
        ) */
        /*select
        member0_.user_id as user_id1_0_,
                member0_.age as age2_0_,
        member0_.team_id as team_id4_0_,
                member0_.username as username3_0_
        from
        member member0_
        where
        member0_.age in (
                select
        member1_.age
                from
        member member1_
        where
        member1_.age>?
            )*/
    }


```

2.select에 서브쿼리

> 간단 예시 : 유저 이름과 평균 나이 함께 출력하기

```java
@Test
    public void subQuery_select() throws Exception {
        QMember subMember = new QMember("subMember");
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }

         /* select
        member1.username,
        (select
            avg(subMember.age)
        from
            Member subMember)
    from
        Member member1 */
        /*select
        member0_.username as col_0_0_,
                (select
        avg(cast(member1_.age as double))
        from
        member member1_) as col_1_0_
        from
        member member0_*/
    }
```


</details>










<details>

<summary> <h1> Case문, 상수 출력, 특정 문자값 붙여 출력하기 </h1> </summary>

### QueryDsl Case문 예제

쿼리를 사용할 때, 경우에 따라 다른 값으로 치환을 Data에서 바로 할 경우가 있다. <br> 
주로 화면에 Fit하게 가져올 때 사용할 것 같은데 <br>
DB는 그냥 퍼올려서 Stream으로 Dto생성해서 처리하는것보다 좋을지는 역시나 고민해봐야할 문제
---
### `기본 CASE문`, `caseBuilder CASE문`

> 간단한 Case문
> 
> > 그냥 when(경우).then(치환글) 만 사용하면 된다.<br> 
> > 말 그대로 간단한 경우

```java
 @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }
```


> 복잡한 Case문
>> 복잡하다는 의미는 when(조건) 절에 조건들이 까다롭다는 의미이다.
>
> > 이럴땐 `package com.querydsl.core.types.dsl` queryDsl이 제공하는 `CaseBuilder` 객체를 사용한다.
> >  참고로 caseBuilder의 when과 그냥 Simple when은 받는 인자가 다르다.
> 
> > `caseBuilder의 when` 
```java
public CaseWhen<A,Q> when(Predicate b) {
            return new CaseWhen<A,Q>(this, b);
        }
```
>> `일반 Simple when`
```java
public CaseWhen<T, Q> when(D when) {
            return when(ConstantImpl.create(when));
        }
```

```java
 @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
        for (String s : result) {
            System.out.println(s);
        }
    }
```


### 특정 상수와 문자열 연결하여 출력하기

> 그냥 끝에 특정 상수값 함께 출력하는 법
> > QueryDsl의 Expressions.constant를 사용한다.

```java
 @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
```
> 문자열 연결하여 출력하는 법
>> concat을 통해 문자열 연결하는데, 해당 변수가 String이 아닐 경우 `stringValue()`를 붙여준다.
>
> > 💥 `stringValue()`는 enum 타입들도 변환할 때 사용해준다.


```java
        @Test
        public void concat() throws Exception {
            //username_age로 붙여 쓰기
            List<String> result = queryFactory
                    .select(member.username.concat("_").concat(member.age.stringValue()))//stringValue() enum타입들도 변환시에 유용하다.
                    .from(member)
                    .where(member.username.eq("member1"))
                    .fetch();

            for (String s : result) {
                System.out.println(s);
            }
        }
```

  
</details>



<details>

<summary> <h1> 프로젝션과 결과 반환 </h1> </summary>

### 프로젝션
select 절에 뭘 가져올지 대상을 지정하는 것

1. 대상이 1개일 때
-> 명확하게 타입 지정하여 반환
2. 대상이 둘 이상일 떄
-> Dto나 튜플로 반환

> 대상이 1개일 때

```java
 @Test
        public void oneProjection() throws Exception {
            //userName을 String으로 받는 모습
            List<String> result = queryFactory
                    .select(member.username)
                    .from(member)
                    .fetch();
            
        }
        //member 객체 하나만 받는것도 원프로젝션이라 한다.
        List<Member> result2 = queryFactory
        .select(member)
        .from(member)
        .fetch();
        
```
---

> 대상이 2개 이상일 때
>> 튜플인 경우
>
> > 💥Tuple의 경우 `package com.querydsl.core` 즉 QueryDsl에 종속되어 있어서<br>
> > `Repository` 영역을 벗어나서 사용되는 것은 지양해야한다.<br>
> > (business 영역에서 queryDsl을 쓰는지 아닌지 관심 없어야 한다)<br>
> > Dto로 수정해서 보내는게 낫다.

```java
@Test
    public void tupleProjection() throws Exception {
        List<Tuple> result1 = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        //튜플 출력 방법
        for (Tuple tuple : result1) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
        }
    }
```

### 💥핵심 ! DTO로 조회
>> QueryDsl 사용시 실무에서 많이 쓰이는 방법이다.

1.JPA로 JPQL로 짜기
```java
   @Test
public void findDtoByJPQL() throws Exception {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
        .getResultList();
```

>> 단점
>>> new 명령어가 DTO 경로까지 적어줘야 해서 번잡스럽다. 이 생성자 방식만 지원된다.

> QueryDsl로 Dto 받기
1. Setter 접근법 `Projections.bean`
2. 필드 직접 접근법 `Projections.fields`
3. 생성자 사용법 `Projections.constructor`

#### 앞서서
`package com.querydsl.core.types의 Projections`을 사용하여 Dto 객체로 Mapping이 쉽게 가능하다.

>> 1.Setter 접근법
>>> 경이롭게 쉬워졌다. Setter가 있고, NoArgsConstructor가 있어야 가능하다.

```java
@Test
    public void findDtoByQueryDsl_Setter() throws Exception {
        List<MemberDto> result = queryFactory
        *****************************************************************************
        //Projections.bean 사용
                .select(Projections.bean(MemberDto.class, member.username, member.age))
        *****************************************************************************
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }
```

>> 2.필드 직접 접근법
>>> Setter 대신 Field에 직접 꽂아 넣어주는 방식으로, Setter가 없어도 동작한다.

```java
@Test
    public void findDtoByQueryDsl_Field() throws Exception {
        List<MemberDto> result = queryFactory
        *****************************************************************************
        //Projections.fields 사용
                .select(Projections.fields(MemberDto.class, member.username, member.age))
        *****************************************************************************
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }
```

>> 3.생성자 사용법
>>> 생성자를 통해서 만드는데, 인자 순서를 잘 지켜줘야 한다.
> 
>>> 생성자

```java
@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
```

```java
  @Test
    public void findDtoByQueryDsl_Constructor() throws Exception {
        List<MemberDto> result = queryFactory
        *****************************************************************************
        //Projections.constructor 사용
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
        *****************************************************************************
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }
```
---

### 응용

> 자유롭게 Dto를 만들어서 Field로 Dto에 Fit하게 맞추는 방법 (`as`,`ExpressionsUtils.as`와 서브쿼리를 사용해서)

앞서서 Field를 이용해 Dto에 맞추는 방법을 알아봤다. 기본적으로 사용하게 되면 
넣으려는 Entity의 멤버변수명과 Dto의 Field명이 동일하게 유지되어야 한다.

> Member Entity의 username, age와 MemberDto의 username, age가 동일하여
> 앞서 수행했던  .select(Projections.fields(MemberDto.class, member.username, member.age)) 매핑이 성공된 이유

```java
@Entity
public class Member {

    @GeneratedValue
    @Id
    @Column(name = "user_id")
    private Long id;
    private String username;

    private int age;
}

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}

//Member.username == MemberDto.username, Member.age == MemberDto.age가 동일하다.
```

> Fit하고 싶은 Dto의 Field Name이 Entity의 것과 다르다면?

Test를 위해 UserDto로 Field 이름을 바꿔서 진행해본다.

```java
@Data
@NoArgsConstructor
public class UserDto {
    private String name; //Member.username != UserDto.name
    private int age;
}

```
> 그냥 Projectiosn.fields로 UserDto.class를 받아 member.username을 넣어주면,<br>
> 
> Compile 때 오류는 나지 않지만 결과값을 인식을 못해 null로 넣어준다.

```java
 @Test
    public void findDtoByQueryDsl_Field2() throws Exception{
        List<UserDto> result=queryFactory
        .select(Projections.fields(UserDto.class,member.username,member.age))
        .from(member)
        .fetch();

        for(UserDto userDto:result){
        System.out.println(userDto);
        }
        /*
        UserDto에는 username이란 Field가 없어서 null로 들어간다 (인식 불가)
        UserDto(name=null, age=10)
        UserDto(name=null, age=20)
        UserDto(name=null, age=30)
        UserDto(name=null, age=40)
         */
        }
```

> 따라서 as로 alias 설정을 해주어야 한다. (member.username -> "name" 으로 as 설정)

```java
 List<UserDto> result2 = queryFactory
        *********************************************************************************
                .select(Projections.fields(UserDto.class, **member.username.as("name")**, member.age))
        **********************************************************************************
                .from(member)
                .fetch();

        for (UserDto userDto : result2) {
            System.out.println(userDto);
        }
        /*
        UserDto(name=member1, age=10)
        UserDto(name=member2, age=20)
        UserDto(name=member3, age=30)
        UserDto(name=member4, age=40)
        잘 들어온 모습
         */
```

> 그럼 이 alias를 이용하면, SubQuery의 결과값도 삽입이 가능한거 아니야? 그렇다.
>
> > ExpressionUtils.as(SubQuery문,alias)를 사용하여 1번째 인자로 SubQuery를, 2번째 인자로 그 alias를 넣어줘서 
> > UserDto를 뽑아내는데 "age"로 alias와 UserDto의 field명을 맞춰주었다. 서브쿼리는 무조건 ExpressionUtils로 감싸야 한다.


```java
 QMember subMember = new QMember("subMember");
        queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name")
        ********************************************************************************
                                , **ExpressionUtils.as(JPAExpressions
                                        .select(subMember.age.max())
                                        .from(subMember), "age")**
        *********************************************************************************
                        )
                ).from(member)
                .fetch();
```

> 번외로 Constructor로 맞춰줄 때는, 인자 타입만 맞으면 잘 들어가게 된다.

```java
   @Test
    public void findDtoByQueryDsl_Constructor2() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }
 // UserDto의 Constructor의 인자 타입과 순서만 맞으면 테스트가 성공한다.
```



### `@QueryProjection` 으로 DtoMapping하기

1. 원하는 Dto의 Constructor에 @QueryProjection을 붙인다.
```java
public class MemberDto {

    private String username;
    private int age;
**********************************************************************
    @QueryProjection
**********************************************************************
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
```

2. compileQuerydsl 돌린다.
![img.png](img.png)

3. QMemberDto가 생성된 모습과 우리가 사용할 생성자가 생긴 모습.
![img_1.png](img_1.png)

4. 이후 냅다 그냥 3에서 생성된 생성자로 select 하면 된다.
```java
    @Test
    public void findDtoByQueryDsl_QueryProjection() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }
```
💥장점은 당연히 complie 시점에 형식 오류를 잡아준다는 것

`Projection.constructor vs @QueryProjection`
* `Projection.constructor`는 Runtime에 오류가 잡힌다.
* `@QueryProjection`는 Complie 시점에 오류가 잡힌다.

**But,** 고민거리는?
#### compile 시점에 Type 체크, 변수 체크 보장이 됨에도 고민되는점은....
* **DTO까지 QFile을 생성해줘야 하는 점** 
* 또한, 설계상 Dto는 Repository, Service 등 여러 구조에서 사용되게 될텐데, 
@QueryProjection을 사용한 Dto가  QueryDsl에 의존적이게 되어서, QueryDsl이 없으면 안되게 되어버린다는 점.

> Dto를 플레인하게 짜고 싶으면 Projection.constructor를 사용하는게 맞다.
>
> 이정도는 허용하고 쉽게 사용하려면 @QueryProjection을 사용하자.



</details>




<details>

<summary> <h1> 동적쿼리짜기 </h1> </summary>

###QueryDsl의 동적 쿼리를 해결하는 방식

기본적으로 쿼리를 동적으로 사용한다는 의미는
`파라미터의 값이 Null이냐 아니냐`에 따라 동적으로 쿼리가 작성이 되는게 목적이다.
ex) 검색조건에서 많이 사용되는 것들`

1.BooleanBuilder 사용하기
2.where절에 다중 파라미터 사용하기

---

### BooleanBuilder 사용

`package com.querydsl.core BooleanBuilder` 사용한다.

> BooleanBuilder는 두개의 생성자를 가지고 있다.
>
> > 최초 선언에 Predicate를 선언할 수 있는데, 생성하면서 null이면 안되는<br>
> > Param들을 미리 선언해주면 된다.


```java
    /**
     * Create an empty BooleanBuilder
     */
    public BooleanBuilder() {  }

    /**
     * Create a BooleanBuilder with the given initial value
     *
     * @param initial initial value
     */
    public BooleanBuilder(Predicate initial) {
        predicate = (Predicate) ExpressionUtils.extract(initial);
    }
```



```java
 @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

       List<Member> result =  searchMember1(usernameParam, ageParam);
       assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
     
*******************************************************************************
       //1번. default Builder 생성으로 구현
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }
        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }
*********************************************************************************

*********************************************************************************
        //2번. Builder 초기값 삽입으로 구현 (member.username이 필수값인 경우)
        BooleanBuilder builder = new BooleanBuilder(member.username.eq("member1"));
        if(ageParam != null){
        builder.and(member.age.eq(ageParam));
        }
**********************************************************************************
        
        
        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();

    }
```
+ 💥builder도 and, or 등 추가 where 연산이 가능하다.

---

### 💫동적쿼리 where 다중 파라미터로 처리하기

앞서서

#### 장점
* Main query를 깔끔하게 유지하고, 명시성이 좋다.
* BooleanExpression을 반환하여 새로운 조건들을 조합해서 사용할 수 있다. ex) 나이가 40이상, 이름 xx는 이벤트 대상자
* 재사용성이 좋다.
---
> where절 안에 들어갈 true,false 값을 param에 따라 메서드로 추출하여 제작한다.
>
* 📀 queryFactory의 where절에서 null이 들어가면 자동으로 skip으로 간주하기 때문에 동적 쿼리가 가능하다.

```java
ex)  return queryFactory
        .selectFrom(member)
        .where(null, ageEq(ageParam))
        .fetch();
```
* 위 상황에서는 age만 같은지 체크함


> 기본 코드

```java
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result =  searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }
```

```java
    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
        *******************************************************************************
                .where(usernameEq(usernameParam), ageEq(ageParam)) //where 절에 메서드를 제작해 동작. (Predicate만 받으면 된다)
        *******************************************************************************
                .fetch();
    }
```

#### where Param별 조건 메서드 생성

```java
    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }
```
* null은 skip하는 queryFactory의 where 조건절의 특성을 이용해 그냥 null을 리턴한다.

> 이 true,false만 지켜주면 어떤 메서드든 조합하여 만들 수 있다.

```java
  private BooleanExpression allEq(String usernameParam, Integer ageParam){
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }
```

* usernameEq + ageEq를 조합해서 allEq를 만든 모습

```java
    private List<Member> searchMember3(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameParam,ageParam))
                .fetch();
    }
```

* 극한으로 깔끔해졌다.





</details>


<details>

<summary> <h1> [QueryDsl] 벌크 update,delete </h1> </summary>

전체 데이터를 한번에 수정하는 경우를 `벌크` 연산이라고 한다.
종종 전체 데이터를 수정해야 하는 일이 생기는데, 어떻게 QueryDsl에서 벌크연산을 하는지 알아보자.

### 조건부로 특정 값 일괄 Update

```java
    @Test
    public void bulkUpdate() throws Exception {
        //28살 미만은 다 비회원으로 이름을 변경하는 예시


        long updateCnt = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        
        em.flush();
        em.clear();
        /* update Member member1
            set member1.username = '비회원'1
            where member1.age < 282 */
    }
```

### 조건 부로 숫자 값들 덧셈,뺄셈,곱셈
> 뺄때는 minus 메서드가 없어서 add(-1) 식으로 수행한다.

```java
  
    @Test
    public void bulkAdd() throws Exception {
        //모든 회원의 나이를 1살씩 더하기

        long updateCnt = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                //.set(member.age, member.age.add(-1))
                //.set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

    }
```

### 삭제

```java
    
    @Test
    public void bulkDelete() throws Exception {

        long updateCnt = queryFactory
                .delete(member)
                .where(member.age.eq(18))
                .execute();

        em.flush();
        em.clear();
    }

```

### 주의사항
bulk 연산은 `영속성 context`와 별개로 db에 바로 Update를 하기 때문에,
연산 이후 `영속성 context`와 db값이 차이가 생긴다.
bulk 연산 이후, select를 한다면 db가 아닌, `영속성 Context`에서 가져온다. (항상 우선권은 영속성Context)  
그래서 💥 bulk연산 이후는 꼭 `em.flush(), em.clear()` 를 수행해주자.




</details>


<details>

<summary> <h1> sqlFunction 사용하기</h1> </summary>

보통 일반적인 내장 function들은 기본제공된다.
각 DB에 맞춘 Dialect에 선언된 Function들은 기본 제공되지만, 자기가 DB에 만든 Function은
기본 Dialect를 상속받아 만든 파일을 등록하고, yml같은 파일에 선언해줘야한다.

### H2Dialect에 선언된 기본 기능들



```java
 @Test
    public void sqlFunction() throws Exception {
        //username의 memeber를 다 m으로 치환하는 Function
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "m")
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }

        /* select function('replace', member1.username, 'member'1, 'm'2)
from Member member1 */
        /*
        select replace(member0_.username, NULL, ?) as col_0_0_ from member member0_;
         */
    }
```
```java

    @Test
    public void sqlFunction2() throws Exception {
        //username의 memeber를 다 소문자로 변환시키는 Function
        List<Member> result = queryFactory
                .select(member
                ).from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (Member s : result) {
            System.out.println(s);
        }

        /* select function('lower', member1.username)
from Member member1 */
        /*
        select lower(member0_.username) as col_0_0_ from member member0_;
         */
    }

```

</details>