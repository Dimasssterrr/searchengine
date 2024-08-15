package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteId;

    @Column(name = "lemma", nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "lemmaId", fetch = FetchType.LAZY)
    private List<Index> indexes;

    public int getSiteById() {
        return siteId.getId();
    }

}
