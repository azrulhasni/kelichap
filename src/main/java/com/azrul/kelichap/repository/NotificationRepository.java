/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import com.azrul.kelichap.domain.Notification;
import com.azrul.kelichap.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author azrul
 */
public interface NotificationRepository extends JpaRepository<Notification, Long>,JpaSpecificationExecutor<Notification>   {
 
    @Modifying
    @Query("update Notification n set n.status = :status where n.toUserName = :username")
    void updateStatus(@Param(value = "username") String username, @Param(value = "status") NotificationStatus status);
    
}
