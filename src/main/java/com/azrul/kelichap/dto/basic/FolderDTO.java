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
public class FolderDTO extends ItemDTO {
    private Set<FolderAccessDTO> accessMap = new HashSet<>();
    private Boolean active;

    @Override
    public String toString() {
        return super.toString()+ " FolderDTO{" + "accessMap=" + accessMap + ", active=" + active + '}';
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
