/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Item;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 *
 * @author azrul
 */
public interface ItemRepository extends JpaRepository<Item, Long>,JpaSpecificationExecutor<Item>  {

    @Query( "select distinct item from Item item "
           + "left join item.parent parent "
           + "left join fetch item.accessMap accessMap "
           + "left join parent.accessMap parentAccessMap "
    + "where parent = :parent "
    + "and item.active in :activeStates "
    + "and ((TYPE(item)=DocumentData and ("
            + "(parentAccessMap.authority in :usernames or :isAdmin=true) "
         + ")) or"
         + "(TYPE(item)=Folder))  ")
    public List<Item> getItemsUnderFolder(Folder parent, Set<Boolean> activeStates, Set<String> usernames, Sort sort, Boolean isAdmin);
    

    @Query( "select count(item) from Item item where item in "
           + "("
           + "select distinct item from Item item "
           + "left join item.parent parent "
           + "left join item.accessMap accessMap "
           + "left join parent.accessMap parentAccessMap "
    + "where parent = :parent "
    + "and item.active in :activeStates "
    + "and ((TYPE(item)=DocumentData and ("
            + "(parentAccessMap.authority in :usernames or :isAdmin=true) "
         + ")) or"
         + "(TYPE(item)=Folder)))  ")
    public Integer countItemsUnderFolder(Folder parent, Set<Boolean> activeStates, Set<String> usernames, Boolean isAdmin);
    
 

    @Modifying
    @Query(value= "delete from item where item.id in (select it.id from item it "
           + "left join folder_access_map fam "
    + "on it.id=fam.folder_id "
    + "where it.path_to_root like CONCAT(:inclusivePathToRoot,'%')) ",nativeQuery = true)
    public void deleteAllDescendantItemsUnderFolder(String inclusivePathToRoot);
    
    @Modifying
    @Query(value= "delete from folder_access_map fam where fam.folder_id in (select it.id from item it "
           + "left join folder_access_map fam "
    + "on it.id=fam.folder_id "
    + "where it.path_to_root like CONCAT(:inclusivePathToRoot,'%')) ",nativeQuery = true)
    public void deleteAllDescendantFolderAccessUnderFolder(String inclusivePathToRoot);
    
    @Query(value= "select it.* from item it "
           + "left join folder_access_map fam "
    + "on it.id=fam.folder_id "
    + "where it.item_type = 'DOCUMENT' "
    + "and it.path_to_root like CONCAT(:inclusivePathToRoot,'%')",nativeQuery = true)
    public List<Item> getAllDescendantDocsUnderFolder(String inclusivePathToRoot);
    
    @Modifying
    @Query(value= "update folder_access_map fam set fam.authority= :newUser where fam.authority= :oldUser",nativeQuery = true)
    public void reassignToNewUser(String oldUser, String newUser);
    
}
