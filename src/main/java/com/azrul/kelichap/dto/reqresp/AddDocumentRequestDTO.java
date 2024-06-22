/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.FolderAccessDTO;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class AddDocumentRequestDTO {
    private Long folderId;
    private String workflowId;
    private String folderName;
    private Set<String> tags;
    private Set<FolderAccessDTO> folderAccess;

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
     * @return the folderAccess
     */
    public Set<FolderAccessDTO> getFolderAccess() {
        return folderAccess;
    }

    /**
     * @param folderAccess the folderAccess to set
     */
    public void setFolderAccess(Set<FolderAccessDTO> folderAccess) {
        this.folderAccess = folderAccess;
    }
   
 

  
}
