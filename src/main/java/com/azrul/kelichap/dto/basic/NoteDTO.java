/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 *
 * @author azrul
 */
public class NoteDTO {
    private Long id;
    private String message;
    private String writerUserName;
    private LocalDateTime writtenDate;

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

    @Override
    public String toString() {
        return "NoteDTO{" + "id=" + id + ", message=" + message + ", writerUserName=" + writerUserName + ", writtenDate=" + writtenDate + '}';
    }
  
}
