package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "status_time",nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd hh:mm")
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "url",nullable = false)
    private String url;

    @Column(name = "name",nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "site", fetch=FetchType.LAZY)
    private List<Page> pages;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "siteId", fetch=FetchType.LAZY)
    private List<Lemma> lemmas;

}
