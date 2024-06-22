/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

import com.azrul.kelichap.domain.converter.StringSetOfStringConverter;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author azrul
 */
@Entity
@Audited 
@EntityListeners(AuditingEntityListener.class)
@DiscriminatorValue("DOCUMENT")
public class DocumentData extends Item{
    
    @Transient
    private ITEM_TYPE itemType = ITEM_TYPE.DOCUMENT;
    
    @NotEmpty
    private String fileLocation;
    
    @NotEmpty
    private String mimeType;
    
    private Long size;
    
    @Nullable
    private String originalName;
    
    @Nullable
    private String originalSize;
    
    @Nullable
    private String originalMimeType;
    
    
    @Column(name = "tags")
    @Convert(converter = StringSetOfStringConverter.class)
    private Set<String> tags = new HashSet<>();
    
   
    @OneToMany(mappedBy = "document",cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Note> notes = new HashSet<>();

    /**
     * @return the file
     */
    public String getFileLocation() {
        return fileLocation;
    }

    /**
     * @param file the file to set
     */
    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    /**
     * @return the tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the notes
     */
    public Set<Note> getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(Set<Note> notes) {
        this.notes = notes;
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

    /**
     * @return the originalSize
     */
    public String getOriginalSize() {
        return originalSize;
    }

    /**
     * @param originalSize the originalSize to set
     */
    public void setOriginalSize(String originalSize) {
        this.originalSize = originalSize;
    }

    /**
     * @return the originalMimeType
     */
    public String getOriginalMimeType() {
        return originalMimeType;
    }

    /**
     * @param originalMimeType the originalMimeType to set
     */
    public void setOriginalMimeType(String originalMimeType) {
        this.originalMimeType = originalMimeType;
    }

    
    
    
   
  

    

}
