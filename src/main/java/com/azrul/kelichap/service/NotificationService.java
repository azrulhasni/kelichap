/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.domain.Notification;
import com.azrul.kelichap.domain.NotificationStatus;
import com.azrul.kelichap.repository.NoteRepository;
import com.azrul.kelichap.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 *
 * @author azrul
 */
@Service
public class NotificationService {
    //private Integer COUNT_PER_PAGE=5;
    
    private final NotificationRepository notifRepo;
    
    public NotificationService(@Autowired NotificationRepository notifRepo){
        this.notifRepo=notifRepo;
    }
    
    public Page<Notification> getForUser(
            String username,
            Integer page,
            Integer countPerPage,
            String sortField,
            Boolean asc){
         Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page,countPerPage, sort);
        return notifRepo.findAll(whereToUserEquals(username),pageable);
    }
    
    public Page<Notification> getForUser(
            String username,
            Integer page,
            Integer countPerPage,
            Sort sort){
        Pageable pageable = PageRequest.of(page,countPerPage, sort);
        return notifRepo.findAll(whereToUserEquals(username),pageable);
    }
    
     public Integer countForUser(String username){
        
        return Long.valueOf(notifRepo.count(whereToUserEquals(username))).intValue();
    }
     
   
    
     public Integer countForUserAndStatus(String username, NotificationStatus status){
        return Long.valueOf(notifRepo.count(whereToUserAndStatusEquals(username,status))).intValue();
    }
     
     public Long countByItemToUserFromUserStatusAndRequestedRight(Item item,String toUserName, String fromUser, NotificationStatus status,FolderRight requestedRight){
         return notifRepo.count(whereItemToUserFromUserStatusAndRequestedRightEquals(item, toUserName, fromUser,status, requestedRight));
     }
    
    
    public void save(Notification notif){
        notifRepo.save(notif);
    }
    
    public void saveAll(List<Notification> notifs){
        notifRepo.saveAll(notifs);
    }
    
    public void delete(Notification notif){
        notifRepo.delete(notif);
    }
    
    public void deleteAllForUser(String username){
        notifRepo.delete(whereToUserEquals(username));
    }
    
    public void mark(Notification notif, NotificationStatus status){
        notif.setStatus(status);
        notifRepo.save(notif);
    }
    
    public void markAllForUser(String username, NotificationStatus status){
        notifRepo.updateStatus(username, status);
    }
    
    
    static Specification<Notification> whereToUserEquals(String username) {
        return (notif, cq, cb) -> {
           return cb.equal(notif.get("toUserName"),username);
        };
    }
    
    static Specification<Notification> whereToUserAndStatusEquals(String username, NotificationStatus status) {
        return (notif, cq, cb) -> {
           return cb.and(
                   cb.equal(notif.get("toUserName"),username),
                   cb.equal(notif.get("status"),status)
           );
        };
    }
    
    static Specification<Notification> whereItemToUserFromUserStatusAndRequestedRightEquals(Item item, String toUsername, String fromUsername,NotificationStatus status, FolderRight requestedRight) {
        return (notif, cq, cb) -> {
           return cb.and(
                   cb.equal(notif.get("toUserName"),toUsername),
                   cb.equal(notif.get("fromUserName"),fromUsername),
                   cb.equal(notif.get("status"),status),
                   cb.equal(notif.get("item"),item),
                   cb.equal(notif.get("requestedRight"),requestedRight)
           );
        };
    }
}
