package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "page", indexes = {
        @javax.persistence.Index(name = "index_path",  columnList = "path, site_id", unique = true)})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "pageId", fetch = FetchType.LAZY)

    private List<Index> indexes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return Objects.equals(getId(), page.getId()) && Objects.equals(getSite(), page.getSite()) && Objects.equals(getPath(), page.getPath()) && Objects.equals(getCode(), page.getCode()) && Objects.equals(getContent(), page.getContent()) && Objects.equals(getIndexes(), page.getIndexes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSite(), getPath(), getCode(), getContent(), getIndexes());
    }
}
