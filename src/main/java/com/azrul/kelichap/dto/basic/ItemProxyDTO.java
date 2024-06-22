/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import com.azrul.kelichap.domain.ITEM_TYPE;
/*

/**
 *
 * @author azrul
 */
public class ItemProxyDTO {
    private Long id;
    private String name;
    

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

    @Override
    public String toString() {
        return "ItemProxyDTO{" + "id=" + id + ", name=" + name + '}';
    }

    
}
/**
 *
 * @author azrul
 */
//public class ItemProxyDTO {
//    private Long id;
//    private String name;
//    private String originalName;
//    private ITEM_TYPE itemType;
//     private Boolean active;
//    private Set<FolderAccessDTO> accessMap = new HashSet<>();
//
//    /**
//     * @return the id
//     */
//    public Long getId() {
//        return id;
//    }
//
//    /**
//     * @param id the id to set
//     */
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    /**
//     * @return the name
//     */
//    public String getName() {
//        return name;
//    }
//
//    /**
//     * @param name the name to set
//     */
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @Override
//    public String toString() {
//        return "ItemProxyDTO{" + "id=" + getId() + ", name=" + getName() + '}';
//    }
//
//    /**
//     * @return the originalName
//     */
//    public String getOriginalName() {
//        return originalName;
//    }
//
//    /**
//     * @param originalName the originalName to set
//     */
//    public void setOriginalName(String originalName) {
//        this.originalName = originalName;
//    }
//
//    /**
//     * @return the itemType
//     */
//    public ITEM_TYPE getItemType() {
//        return itemType;
//    }
//
//    /**
//     * @param itemType the itemType to set
//     */
//    public void setItemType(ITEM_TYPE itemType) {
//        this.itemType = itemType;
//    }
//
//    /**
//     * @return the active
//     */
//    public Boolean getActive() {
//        return active;
//    }
//
//    /**
//     * @param active the active to set
//     */
//    public void setActive(Boolean active) {
//        this.active = active;
//    }
//
//    /**
//     * @return the accessMap
//     */
//    public Set<FolderAccessDTO> getAccessMap() {
//        return accessMap;
//    }
//
//    /**
//     * @param accessMap the accessMap to set
//     */
//    public void setAccessMap(Set<FolderAccessDTO> accessMap) {
//        this.accessMap = accessMap;
//    }
//    
//}
