/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author azrul
 */
@Embeddable
@Audited 
@EntityListeners(AuditingEntityListener.class)
public class FolderAccess {
    private String authority;
    private FolderRight rights;
    
  

    public FolderAccess() {
    }

    public FolderAccess(String authority, FolderRight rights) {
        this.authority = authority;
        this.rights = rights;
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
     * @return the right
     */
    public FolderRight getRights() {
        return rights;
    }

    /**
     * @param right the right to set
     */
    public void setRights(FolderRight rights) {
        this.rights = rights;
    }

   
    
    
    
     @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Objects.hashCode(this.authority);
        hash = 19 * hash + Objects.hashCode(this.rights);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FolderAccess other = (FolderAccess) obj;
        if (!Objects.equals(this.authority, other.authority)) {
            return false;
        }
        return this.rights == other.rights;
    }

   
}
