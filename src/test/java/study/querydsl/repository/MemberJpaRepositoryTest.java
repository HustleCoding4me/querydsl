package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void testJPA() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findMember);

        List<Member> all = memberJpaRepository.findAll();
        assertThat(all).containsExactly(member);

        List<Member> member1 = memberJpaRepository.findByUsername("member1");
        assertThat(member1).containsExactly(member);

    }

    @Test
    void testQueryDsl() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findMember);

        List<Member> member1 = memberJpaRepository.findByUsername_Querydsl("member1");
        assertThat(member1).containsExactly(member);

        List<Member> all = memberJpaRepository.findAll_Querydsl();
        assertThat(all).containsExactly(member);

    }

    @Test
    public void searchTest() throws Exception {

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


        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(30);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly("member3","member4");
    }

}