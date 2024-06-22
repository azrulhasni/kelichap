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
public class UpdateFolderResponseDTO {
    private FolderDTO folder;

    /**
     * @return the folderDTO
     */
    public FolderDTO getFolder() {
        return folder;
    }

    /**
     * @param folderDTO the folderDTO to set
     */
    public void setFolder(FolderDTO folderDTO) {
        this.folder = folderDTO;
    }
}
