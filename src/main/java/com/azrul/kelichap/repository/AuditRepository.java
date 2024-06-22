/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.order.AuditOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author azrul
 */
@Repository
public class AuditRepository {

    @Autowired
    private EntityManagerFactory factory;


    public <T> Integer getRevisionCount(
            final Class<T> tClass,
            final String prop,
            final Object propValue) {
        EntityManager em = factory.createEntityManager();
        AuditReader audit = AuditReaderFactory.get(em);
        try {
            Long c = (Long) audit.createQuery()
                    .forRevisionsOfEntity(tClass, true, false)
                    .add(AuditEntity.property("version").hasChanged())
                    .add(AuditEntity.property(prop).eq(propValue))
                    .addProjection(AuditEntity.id().count())
                    .getSingleResult();
            return c.intValue();
        } finally {
            em.close();
        }
    }

    public <T> List<T> getRevisions(
            final Class<T> tClass,
            final String prop,
            final Object propValue,
            final Integer page,
            final Integer maxItemCount,
            final String orderBy,
            final boolean asc) {

        if (Objects.isNull(tClass) || !StringUtils.hasText(prop) || Objects.isNull(propValue)) {
            throw new IllegalArgumentException("Invalid params.");
        }
        AuditOrder order = null;
        if (asc) {
            order = AuditEntity.property(orderBy).asc();
        } else {
            order = AuditEntity.property(orderBy).desc();
        }
        EntityManager em = factory.createEntityManager();
        AuditReader audit = AuditReaderFactory.get(em);
        try {

            List<T> items =audit.createQuery()
                    .forRevisionsOfEntity(tClass, true, false)
                    .add(AuditEntity.property(prop).eq(propValue))
                    .add(AuditEntity.property("version").hasChanged())
                    .setFirstResult(page * maxItemCount)
                    .setMaxResults((page + 1) * maxItemCount)
                    .addOrder(order)
                    .getResultList();
            return items;
        } finally {
            em.close();
        }
    }
}
