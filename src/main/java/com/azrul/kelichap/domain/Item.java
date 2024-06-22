/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

import com.azrul.kelichap.domain.converter.StringListOfLongConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author azrul
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type",
        discriminatorType = DiscriminatorType.STRING)
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    private String name;

    private String workflowId;

    @Audited(withModifiedFlag = true)
    private Integer version;

//    @Column(name = "path_to_root")
//    @Convert(converter = StringListOfLongConverter.class)
//    private List<Long> pathToRoot = new ArrayList<>();
    @CreatedBy
    private String createdBy;

    @CreatedDate
    private LocalDateTime creationDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "root_folder_id")
    private Folder root;

    private Integer level;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder parent;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true/*,fetch = FetchType.EAGER*/)
    @JoinColumn(name = "folder_id")
    @NotAudited
    private Set<Item> children = new HashSet<>();

    //path to root is parent -> root (exclude currrent).
    //root = path to root empty
    //must be List to maintain order
    @Column(name = "pathToRoot")
    @Convert(converter = StringListOfLongConverter.class)
    private List<Long> pathToRoot = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "FOLDER_ACCESS_MAP",
            joinColumns = @JoinColumn(name = "FOLDER_ID"),
            uniqueConstraints= @UniqueConstraint(columnNames={"folder_id","authority"})
    )
    private Set<FolderAccess> accessMap = new HashSet<>();
    /**
     * @return the accessMap
     */
    public Set<FolderAccess> getAccessMap() {
        return accessMap;
    }

    /**
     * @param accessMap the accessMap to set
     */
    public void setAccessMap(Set<FolderAccess> accessMap) {
        this.accessMap = accessMap;
    }

    /**
     * @return the items
     */
    public Set<Item> getChildren() {
        return children;
    }

    public Folder[] getFolderChildren() {
        Set<Folder> folders = children.stream().filter(Folder.class::isInstance).map(Folder.class::cast).collect(Collectors.toSet());
        return folders.toArray(Folder[]::new);
    }

    public DocumentData[] getDocumentChildren() {
        Set<DocumentData> docs =  children.stream().filter(DocumentData.class::isInstance).map(DocumentData.class::cast).collect(Collectors.toSet());
        return docs.toArray(DocumentData[]::new);
    }
    
   

    /**
     * @param items the items to set
     */
    public void setChildren(Set<Item> items) {
        this.children = items;
    }

    public Folder getParent() {
        return parent;
    }

    /**
     * @param folder the folder to set
     */
    public void setParent(Folder folder) {
        this.parent = folder;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    @PrePersist
    public void init() {
        if (version == null) {
            version = 1;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.getId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        return Objects.equals(this.getId(), other.getId());
    }

    /**
     * @return the workflowId
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * @param workflowId the workflowId to set
     */
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "Item{" + "id=" + id + ", name=" + name + ", workflowId=" + workflowId + '}';
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the creationDate
     */
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the lastModifiedDate
     */
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer incrementVersion() {
        this.version++;
        return this.version;
    }

    /**
     * @return the root
     */
    public Folder getRoot() {
        return root;
    }

    /**
     * @param root the root to set
     */
    public void setRoot(Folder root) {
        this.root = root;
    }

    /**
     * @return the level
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    public Boolean isRoot() {
        return parent == null;
    }

    /**
     * @return the pathToRoot
     */
    public List<Long> getPathToRoot() {
        return pathToRoot;
    }

    /**
     * @param pathToRoot the pathToRoot to set
     */
    public void setPathToRoot(List<Long> pathToRoot) {
        this.pathToRoot = pathToRoot;
    }

    public List<Long> getPathToRootInclusive() {
        List<Long> pathToRootInc = new ArrayList<>();

        pathToRootInc.addAll(pathToRoot);
        pathToRootInc.add(id);
        return pathToRootInc;
    }

    public String getPathToRootAsCSV(String separator) {
        return StringUtils.join(pathToRoot, separator);
    }
    
     public String getPathToRootInclusiveInCSV(String separator) {
        List<Long> pathToRootInc = getPathToRootInclusive();
        return StringUtils.join(pathToRootInc, separator);
    }

}
