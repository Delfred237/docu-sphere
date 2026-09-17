package com.docusphere.folder.domain;

import com.docusphere.auth.domain.User;
import com.docusphere.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
public class Folder extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    // On n'utilise pas CascadeType.REMOVE ici pour éviter les suppressions accidentelles en cascade
    // La suppression est gérée par le service métier (soft delete ou déplacement)
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<Folder> children = new HashSet<>();

    public void addChild(Folder child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void removeChild(Folder child) {
        this.children.remove(child);
        child.setParent(null);
    }

    public boolean isRoot() {
        return this.parent == null;
    }
}
