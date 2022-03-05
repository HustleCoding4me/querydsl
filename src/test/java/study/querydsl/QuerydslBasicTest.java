package study.querydsl;

import com.querydsl.core.QueryResults;
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
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;

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

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
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
}
