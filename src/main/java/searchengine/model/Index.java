package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "`index`")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(optional = false) //cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id", nullable = false)
//    @Column(name = "page_id")
    private Page pageId;

    @ManyToOne(optional = false)// cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id", nullable = false)
//    @Column(name = "lemma_id")
    private Lemma lemmaId;

    @Column(name = "`rank`", nullable = false)
    private Float rank;

}
