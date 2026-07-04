package com.lms.quiz;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 객관식 문항. choices(보기)는 JSON으로 저장. correctIndex(정답)는 응답에 노출하지 않는다.
 */
@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor
public class Question extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false, updatable = false)
    private UUID quizId;

    @Column(nullable = false)
    private String body;

    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false)
    private List<String> choices;

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    public Question(UUID quizId, String body, List<String> choices, int correctIndex, int orderNo) {
        this.quizId = quizId;
        this.body = body;
        this.choices = choices;
        this.correctIndex = correctIndex;
        this.orderNo = orderNo;
    }

    /** 채점: 제출한 보기 인덱스가 정답인가. */
    public boolean isCorrect(int answerIndex) {
        return answerIndex == this.correctIndex;
    }

    /** 문항 수정 (강사/관리자). quizId는 바뀌지 않는다. */
    public void update(String body, List<String> choices, int correctIndex, int orderNo) {
        this.body = body;
        this.choices = choices;
        this.correctIndex = correctIndex;
        this.orderNo = orderNo;
    }
}
