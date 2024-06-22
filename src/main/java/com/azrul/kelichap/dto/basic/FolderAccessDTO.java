/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import com.azrul.kelichap.domain.FolderRight;

/**
 *
 * @author azrul
 */
public class FolderAccessDTO {
    private String authority;
    private FolderRight rights;

    @Override
    public String toString() {
        return "FolderAccessDTO{" + "authority=" + authority + ", rights=" + rights + '}';
    }

    /**
     * @return the authority
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * @param authority the authority to set
     */
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    /**
     * @return the rights
     */
    public FolderRight getRights() {
        return rights;
    }

    /**
     * @param rights the rights to set
     */
    public void setRights(FolderRight rights) {
        this.rights = rights;
    }
}
