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
public class DocumentDataDTO extends ItemDTO {
    private String fileLocation;
    private String mimeType;
    

    @Override
    public String toString() {
        return super.toString()+" DocumentDataDTO{" + "fileLocation=" + fileLocation + ", mimeType=" + mimeType + ", size=" + size + ", originalName=" + originalName + ", originalSize=" + originalSize + ", originalMimeType=" + originalMimeType + ", tags=" + tags + ", notes=" + notes + '}';
    }
    private Long size;
    private String originalName;
    private String originalSize;
    private String originalMimeType;
    private Set<String> tags = new HashSet<>();
    private Set<NoteDTO> notes = new HashSet<>();

    /**
     * @return the fileLocation
     */
    public String getFileLocation() {
        return fileLocation;
    }

    /**
     * @param fileLocation the fileLocation to set
     */
    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
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
     * @return the notes
     */
    public Set<NoteDTO> getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(Set<NoteDTO> notes) {
        this.notes = notes;
    }
}
