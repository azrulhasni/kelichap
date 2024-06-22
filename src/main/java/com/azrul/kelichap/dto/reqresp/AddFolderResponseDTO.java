/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.FolderDTO;

/**
 *
 * @author azrul
 */
public class AddFolderResponseDTO {
   private FolderDTO folder;

    /**
     * @return the folder
     */
    public FolderDTO getFolder() {
        return folder;
    }

    /**
     * @param folder the folder to set
     */
    public void setFolder(FolderDTO folder) {
        this.folder = folder;
    }
}
