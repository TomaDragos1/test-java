package com.example.chat_api.repos;

import com.example.hr.model.CandidateAnswer;
import com.example.hr.model.CandidateAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CandidateAnswerRepository extends JpaRepository<CandidateAnswer, CandidateAnswerId> {

    @Query("""
        select ca from CandidateAnswer ca
        left join fetch ca.question q
        where ca.id.roundId = :roundId and ca.id.userId = :userId
        order by q.idQuestion
    """)
    List<CandidateAnswer> findAllForRoundAndUser(Long roundId, Long userId);
}
