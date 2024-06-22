/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.domain.FolderAccess;
import java.util.List;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class AddFolderRequestDTO {
    private String workflowId;
    private String folderName;
    private Long parentFolderId;
    private Set<FolderAccess> accessMap;

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
     * @return the parentFolderId
     */
    public Long getParentFolderId() {
        return parentFolderId;
    }

    /**
     * @param parentFolderId the parentFolderId to set
     */
    public void setParentFolderId(Long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

}
