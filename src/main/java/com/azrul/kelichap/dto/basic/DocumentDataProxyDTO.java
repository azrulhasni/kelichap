/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import com.azrul.kelichap.domain.ITEM_TYPE;

/**
 *
 * @author azrul
 */
public class DocumentDataProxyDTO extends ItemProxyDTO{
    private ITEM_TYPE itemType=ITEM_TYPE.DOCUMENT;
    private String originalName;

    /**
     * @return the itemType
     */
    public ITEM_TYPE getItemType() {
        return itemType;
    }

    /**
     * @param itemType the itemType to set
     */
    public void setItemType(ITEM_TYPE itemType) {
        this.itemType = itemType;
    }

    /**
     * @return the originalName
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @param originalName the originalName to set
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    @Override
    public String toString() {
        return super.toString()+" DocumentDataProxyDTO{" + "itemType=" + itemType + ", originalName=" + originalName + '}';
    }
    
    
}