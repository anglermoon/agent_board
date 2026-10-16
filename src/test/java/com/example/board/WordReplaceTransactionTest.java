package com.example.board;

import com.example.board.entity.Post;
import com.example.board.repository.PostRepository;
import com.example.board.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:replacetxdb;DB_CLOSE_DELAY=-1")
class WordReplaceTransactionTest {

    @Autowired
    PostService service;

    @Autowired
    PostRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Post save(String title, String content) {
        Post p = new Post();
        p.setTitle(title);
        p.setName("kim");
        p.setContent(content);
        return repository.save(p);
    }

    @Test
    void 중간에_실패하면_앞서_처리한_글도_모두_원복된다() {
        Post ok = save("kfc 후기", "kfc 맛있다");
        Post tooLong = save("kfc" + "가".repeat(97), "제목이 정확히 100자");   // 치환 후 100자 초과

        assertThatThrownBy(() -> service.replaceWord(List.of(ok.getId(), tooLong.getId()), "kfc", "치킨전문점"))
                .isInstanceOf(Exception.class);

        assertThat(repository.findById(ok.getId()).orElseThrow().getTitle()).isEqualTo("kfc 후기");
        assertThat(repository.findById(ok.getId()).orElseThrow().getContent()).isEqualTo("kfc 맛있다");
        assertThat(repository.findById(tooLong.getId()).orElseThrow().getTitle()).startsWith("kfc");
    }

    @Test
    void 모두_성공하면_전부_반영된다() {
        Post a = save("kfc 후기", "kfc 맛있다");
        Post b = save("오늘 kfc", "kfc 최고");

        int changed = service.replaceWord(List.of(a.getId(), b.getId()), "kfc", "bhc");

        assertThat(changed).isEqualTo(2);
        assertThat(repository.findById(a.getId()).orElseThrow().getTitle()).isEqualTo("bhc 후기");
        assertThat(repository.findById(b.getId()).orElseThrow().getContent()).isEqualTo("bhc 최고");
    }
}
