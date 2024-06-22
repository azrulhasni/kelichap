/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import com.azrul.kelichap.domain.ITEM_TYPE;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class ItemDTO {

    private Long id;
    private String name;
    private String workflowId;
    private Integer version;
    private String createdBy;
    private LocalDateTime creationDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private Long rootId;
    private Integer level;
    private Long parentId;
    private ITEM_TYPE itemType;
    private Set<ItemProxyDTO> children = new HashSet<>();

    @Override
    public String toString() {
        return "ItemDTO{" + "id=" + id + ", name=" + name + ", workflowId=" + workflowId + ", version=" + version + ", createdBy=" + createdBy + ", creationDate=" + creationDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate + ", level=" + level + ", children=" + children + '}';
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

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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

//    /**
//     * @return the root
//     */
//    public ItemDTO getRoot() {
//        return root;
//    }
//
//    /**
//     * @param root the root to set
//     */
//    public void setRoot(ItemDTO root) {
//        this.root = root;
//    }
//
//    /**
//     * @return the parent
//     */
//    public ItemDTO getParent() {
//        return parent;
//    }
//
//    /**
//     * @param parent the parent to set
//     */
//    public void setParent(ItemDTO parent) {
//        this.parent = parent;
//    }
    /**
     * @return the children
     */
    public Set<ItemProxyDTO> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(Set<ItemProxyDTO> children) {
        this.children = children;
    }

    public void setFolderChildren(FolderProxyDTO[] folders) {
         if (folders==null){
            return;
        }
        this.children.addAll(Arrays.asList(folders));
    }

    public void setDocumentChildren(DocumentDataProxyDTO[] docs) {
        if (docs==null){
            return;
        }
        this.children.addAll(Arrays.asList(docs));
    }
    
   

    /**
     * @return the rootId
     */
    public Long getRootId() {
        return rootId;
    }

    /**
     * @param rootId the rootId to set
     */
    public void setRootId(Long rootId) {
        this.rootId = rootId;
    }

    /**
     * @return the parentId
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

}
