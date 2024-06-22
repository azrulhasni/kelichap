/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author azrul
 */
@Repository
public interface DocumentDataRepository extends 
        JpaRepository<DocumentData, Long>,
        JpaSpecificationExecutor<DocumentData>,
        RevisionRepository<DocumentData,Long,Long>{

    @Modifying
    @Query("update DocumentData d set d.fileLocation = :fileLocation where d.id = :id")
    void updateFileLocation(@Param(value = "id") Long id, @Param(value = "fileLocation") String fileLocation);
    
     @Query( "select distinct item from Item item "
    + "where item.parent = :parent "
    + "and TYPE(item)=DocumentData  ")
    public List<DocumentData> getDocsUnderFolder(Folder parent);
    
    //<T> List<T> findAll(Specification<DocumentData> spec, Class<T> type);
}
