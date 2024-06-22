/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

/**
 *
 * @author azrul
 */
public enum FolderRight {
    FOLDER_OWNER("Owner"),
    CREATE_UPDATE_FOLDER("Create and update"),
    READ_FOLDER("Read");
    
    private String humanReadableValue;
    
    private FolderRight(String humanReadableValue){
        this.humanReadableValue=humanReadableValue;
    }

    /**
     * @return the humanReadableValue
     */
    public String getHumanReadableValue() {
        return humanReadableValue;
    }

    /**
     * @param humanReadableValue the humanReadableValue to set
     */
    public void setHumanReadableValue(String humanReadableValue) {
        this.humanReadableValue = humanReadableValue;
    }
    
    @Override
    public String toString(){
        return this.humanReadableValue;
    } 
    
}
