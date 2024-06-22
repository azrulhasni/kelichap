/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author azrul
 */
@Component
public class ItemAuditEventRegisterer {
   


	@Autowired
	private EntityManagerFactory entityManagerFactory;
    
	@PostConstruct
	public void registerEnversListeners() {
            SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
            EnversService enversService =sessionFactory.getServiceRegistry().getService(EnversService.class);
            EventListenerRegistry listenerRegistry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
            listenerRegistry.setListeners(EventType.PRE_UPDATE, new PreUpdateEventListener[]{new ItemAuditEventListener(enversService)});
	}
 
}
