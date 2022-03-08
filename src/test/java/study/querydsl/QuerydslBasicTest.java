package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        //given
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        Member member5 = new Member(null, 100);
        Member member6 = new Member("member6", 100);
        Member member7 = new Member("member7", 100);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.persist(member5);
        em.persist(member6);
        em.persist(member7);

    }

    @Test
    public void startJPQL() throws Exception {
        //member1을 찾아라
        Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findByJPQL.getUsername()).isEqualTo("member1");
    }

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

    //chain and 조건
    @Test
    public void search() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();


        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //, and 조건
    @Test
    public void searchAndParam() throws Exception {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
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
        //SQL에서 and가 상위 (먼저 실행) 되어서 실 동작은 (e1 and e2) or (e3 and e4) 이렇게 된다.
    }

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
        //Paging처럼 count 쿼리까지 나간ㄷ.
        /* select count(member1)
        from Member member1 */
        /* select member1
        from Member member1 */

        //fetchCount count만 가져오기
        long couint = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

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

        Member member6 = result.get(0);
        Member member7 = result.get(1);
        Member member5 = result.get(2); //이름 null

        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(member7.getUsername()).isEqualTo("member7");
        assertThat(member5.getUsername()).isNull();
        /*
        select member1
        from Member member1
        where member1.age = 1001
        order by member1.age desc, member1.username asc nulls last
         */
    }

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


    /**
     *
     * Team A에 속한 모든 회원
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
    }

    //연관관계가 없어도 join을 하는 경우
    //thetajoin이라고 한다.

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
        //모든 회원을 가져오고 모든 팀을 가져와서 다 join, 이후 where 에서 필터링 한다.

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
    //from절에 여러 entity를 선택해서 세타 조인가능하다.
    //outer 조인이 불가능하다. -> on을 사용하여 외부 조인


    /////////////On절
    //1. 조인 대상 필터링
    //2. 연관관계 없는 엔티티 외부 조인

//2. 연관관계 없는 엔티티 외부 조인

    /**
     * 연관관계가 없는 Entity끼리 outer 조인을 할 떄 사용한다.
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
        // 흔히 말하는 막Join이다. 연관관계가 없는 두 Entity를 Join하는 방식이기 때문에,
        //연관관계 객체를 넣어주지 않으면 ex).leftJoin(member.team)이 아닌, .leftJoin(team)
        //id값으로 매칭을 해주는 기존 연관관계 join과 다르게, 순수 on절의 조건으로 매칭을 시킨다.

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
        /*
       elect
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


    //1. 조인 대상 필터링
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

        //innerJoin 할거면 그냥 where절을 쓰는게 낫다.

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
               ************************
                on member0_.team_id=team1_.team_id
                and (
                    team1_.name=?
                )
                **********************
         */
    }


    //주의사항 상관관계 없는 on 조인은 leftJoin()에 entity 하나씩 들어간다.
    //일반 조인 : `leftJoin(member.team, team)`
    //on 조인 : `from(member).leftJoin(team).on(xxx)`



}
