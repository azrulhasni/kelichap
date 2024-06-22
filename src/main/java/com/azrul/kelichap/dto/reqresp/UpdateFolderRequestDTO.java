/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.FolderAccessDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class UpdateFolderRequestDTO {
    
    private String folderName;
    
    private Boolean active;
    
    private Set<FolderAccessDTO> accessMap = new HashSet<>();

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

    /**
     * @return the accessMap
     */
    public Set<FolderAccessDTO> getAccessMap() {
        return accessMap;
    }

    /**
     * @param accessMap the accessMap to set
     */
    public void setAccessMap(Set<FolderAccessDTO> accessMap) {
        this.accessMap = accessMap;
    }

    
}
