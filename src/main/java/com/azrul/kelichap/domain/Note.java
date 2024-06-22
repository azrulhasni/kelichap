/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author azrul
 */
@Entity
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String message;
    
    private String writerUserName;
    
    private String writerFullName;
    
    private LocalDateTime writtenDate;
    
    @Transient 
    private Boolean highlighted;
    
    @CreatedBy
    private String createdBy;
    
    @CreatedDate
    private LocalDateTime creationDate;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    
    
    @ManyToOne
    @JoinColumn(name = "doc_id")
    private DocumentData document;
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
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the writerUserName
     */
    public String getWriterUserName() {
        return writerUserName;
    }

    /**
     * @param writerUserName the writerUserName to set
     */
    public void setWriterUserName(String writerUserName) {
        this.writerUserName = writerUserName;
    }

    /**
     * @return the writerFullName
     */
    public String getWriterFullName() {
        return writerFullName;
    }

    /**
     * @param writerFullName the writerFullName to set
     */
    public void setWriterFullName(String writerFullName) {
        this.writerFullName = writerFullName;
    }

    /**
     * @return the writtenDate
     */
    public LocalDateTime getWrittenDate() {
        return writtenDate;
    }

    /**
     * @param writtenDate the writtenDate to set
     */
    public void setWrittenDate(LocalDateTime writtenDate) {
        this.writtenDate = writtenDate;
    }

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the creationDate
     */
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the lastModifiedDate
     */
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @return the document
     */
    public DocumentData getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DocumentData document) {
        this.document = document;
    }

    /**
     * @return the highlighted
     */
    public Boolean getHighlighted() {
        return highlighted;
    }

    /**
     * @param highlighted the highlighted to set
     */
    public void setHighlighted(Boolean highlighted) {
        this.highlighted = highlighted;
    }
    
}
