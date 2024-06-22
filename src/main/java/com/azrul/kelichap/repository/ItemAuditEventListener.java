/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversPreUpdateEventListenerImpl;
import org.hibernate.event.spi.PreUpdateEvent;

/**
 *
 * @author azrul
 */
public class ItemAuditEventListener extends EnversPreUpdateEventListenerImpl {

    public ItemAuditEventListener(EnversService enversService) {
        super(enversService);
        
    }

     @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
//        System.out.println("in audit listener:"+event.getId());
//        for (int i=0;i<event.getPersister().getPropertyNames().length;i++){
//            if (StringUtils.equals(event.getPersister().getPropertyNames()[i],"version")){ //if version is different
//                if (!Objects.equals(event.getOldState()[i],event.getState()[i])){ //file has changed
//                    ((Item)event.getEntity()).setFileChangeFlag(1);
//                }
//            }
//        }
        return super.onPreUpdate(event);
    }
}