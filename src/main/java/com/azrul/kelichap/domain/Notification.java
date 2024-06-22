/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 *
 * @author azrul
 */
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String fromUserName;

    private String fromFullName;

    private String toUserName;
    
    private String toFullName;
    
    private LocalDateTime dateTime;
    
    private NotificationType type;
    
    private NotificationStatus status;
    
    private FolderRight requestedRight;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "item_id",foreignKey =  @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Item item;
    
    @Nullable
    private Long messageId;
    
    @Nullable
    private String message;

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
     * @return the fromUserName
     */
    public String getFromUserName() {
        return fromUserName;
    }

    /**
     * @param fromUserName the fromUserName to set
     */
    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    /**
     * @return the fromFullName
     */
    public String getFromFullName() {
        return fromFullName;
    }

    /**
     * @param fromFullName the fromFullName to set
     */
    public void setFromFullName(String fromFullName) {
        this.fromFullName = fromFullName;
    }

    /**
     * @return the toUserName
     */
    public String getToUserName() {
        return toUserName;
    }

    /**
     * @param toUserName the toUserName to set
     */
    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    /**
     * @return the toFullName
     */
    public String getToFullName() {
        return toFullName;
    }

    /**
     * @param toFullName the toFullName to set
     */
    public void setToFullName(String toFullName) {
        this.toFullName = toFullName;
    }

    /**
     * @return the dateTime
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * @param dateTime the dateTime to set
     */
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * @return the type
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(NotificationType type) {
        this.type = type;
    }

    /**
     * @return the status
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    /**
     * @return the item
     */
    public Item getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * @return the messageId
     */
    public Long getMessageId() {
        return messageId;
    }

    /**
     * @param messageId the messageId to set
     */
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
     * @return the requestedRight
     */
    public FolderRight getRequestedRight() {
        return requestedRight;
    }

    /**
     * @param requestedRight the requestedRight to set
     */
    public void setRequestedRight(FolderRight requestedRight) {
        this.requestedRight = requestedRight;
    }

 
    
}
