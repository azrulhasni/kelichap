/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import com.azrul.kelichap.domain.ITEM_TYPE;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class FolderProxyDTO extends ItemProxyDTO{
    private final ITEM_TYPE itemType=ITEM_TYPE.FOLDER;
    private Boolean active;
    private Set<FolderAccessDTO> accessMap = new HashSet<>();
    

    @Override
    public String toString() {
        return super.toString()+" FolderProxyDTO{" + "itemType=" + itemType + '}';
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