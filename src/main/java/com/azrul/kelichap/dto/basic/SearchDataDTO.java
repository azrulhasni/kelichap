/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class SearchDataDTO {

    
    private Long docId;
    private Set<String> tags = new HashSet<>();
    private String type;
    private String workflowId;
    private String folderName;
    private Long folderId;
    private Integer page;
    private String fileName;
    private String fileLocation;
    private Integer version;
    private Integer size;
    private Boolean active;
    private Long rootFolderId;
    private Long noteId;

    private Set<String> snippets;

    private Float vectorDistance;

    /**
     * @return the id
     */
    public Long getDocId() {
        return docId;
    }

    /**
     * @param id the id to set
     */
    public void setDocId(Long id) {
        this.docId = id;
    }

    /**
     * @return the fileLocation
     */
    public String getFileLocation() {
        return fileLocation;
    }

    /**
     * @param fileLocation the fileLocation to set
     */
    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    /**
     * @return the name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param name the name to set
     */
    public void setFileName(String name) {
        this.fileName = name;
    }

    /**
     * @return the size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * @return the tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * @return the snippet
     */
    public Set<String> getSnippets() {
        return snippets;
    }

    /**
     * @param snippet the snippet to set
     */
    public void setSnippets(Set<String> snippets) {
        this.snippets = snippets;
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
     * @return the vectorDistance
     */
    public Float getVectorDistance() {
        return vectorDistance;
    }

    /**
     * @param vectorDistance the vectorDistance to set
     */
    public void setVectorDistance(Float vectorDistance) {
        this.vectorDistance = vectorDistance;
    }

    /**
     * @return the folderName
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * @param folderName the folderName to set
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * @return the folderId
     */
    public Long getFolderId() {
        return folderId;
    }

    /**
     * @param folderId the folderId to set
     */
    public void setFolderId(Long folderId) {
        this.folderId = folderId;
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
     * @return the rootFolderId
     */
    public Long getRootFolderId() {
        return rootFolderId;
    }

    /**
     * @param rootFolderId the rootFolderId to set
     */
    public void setRootFolderId(Long rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the noteId
     */
    public Long getNoteId() {
        return noteId;
    }

    /**
     * @param noteId the noteId to set
     */
    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    /**
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    
}
