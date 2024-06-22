/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.repository;

import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author azrul
 */
@Repository
public interface FolderRepository  extends JpaRepository<Folder, Long>,JpaSpecificationExecutor<Folder> {
    
  
    @Query("select folder from Folder folder "
           + "join fetch folder.accessMap accessMap "
    + "where folder.workflowId = :workflowId "
    + "and accessMap in :folderAccess")
    Optional<Folder> findFolderByWorkflowIdWithFolderAccess(String workflowId,  Set<FolderAccess> folderAccess);
    
     @Query("select folder from Folder folder "
           + "join fetch folder.accessMap accessMap "
    + "where folder.workflowId = :workflowId "
    + "and accessMap.authority in :currentAuthorities")
    Optional<Folder> findFolderByWorkflowIdAccessibleByAuthorities(String workflowId,  Set<String> currentAuthorities);
//    
    @Modifying
    @Query("update Folder f set f.name = :folderName where f.id = :id")
    void updateFolderName(@Param(value = "id") Long id, @Param(value = "folderName") String folderName);

    @Query("select distinct folder from Folder folder "
           + "left join fetch folder.accessMap accessMap "
    + "where folder.parent is null "
    + "and folder.active in :activeStates "
    + "and ("
            + "(accessMap.authority in :usernames or :isAdmin=true) "
            + "or accessMap.rights = :folderright"
         + ") "
    + "and folder in ("
    + " select root_ from Folder folder_ "
            + "join folder_.accessMap accessMap_ "
            + "join folder_.root root_"
    + " where (accessMap_.authority in :usernames or :isAdmin=true)"
    + ")")
    public Page<Folder> findRootFoldersAndDownstreamIsAccessible(Set<Boolean> activeStates, Set<String> usernames, FolderRight folderright, Pageable pageable, Boolean isAdmin);
 
    @Query("select count(distinct folder) from Folder folder "
    + "where folder.parent is null "
    + "and folder.active in :activeStates "
    + "and folder in ("
    + " select root_ from Folder folder_ "
            + "join folder_.accessMap accessMap_ "
            + "join folder_.root root_ "
    + " where (accessMap_.authority in :usernames or :isAdmin = true) "
    + ")")
    public Integer countRootFoldersAndDownstreamIsAccessible(Set<Boolean> activeStates, Set<String> usernames, Boolean isAdmin);

    @Query("select folder from Folder folder "
           + "join fetch folder.accessMap accessMap "
    + "where folder = :staleFolder "
    + "and ("
            + "accessMap.authority in :usernames "
            + "or accessMap.rights = :folderright"
         + ") ")
    public Folder refresh(Folder staleFolder, Set<String> usernames, FolderRight folderright);
    
    @Query("select distinct folder "
    + "from Folder folder "
             + "join fetch folder.accessMap accessMap "
    + "where folder = :folder_ ")
    public Folder getFolderWithCompleteAccessMap(Folder folder_);
    
     @Query("select distinct folder "
    + "from Folder folder "
             + "join fetch folder.accessMap accessMap "
    + "where folder.id = :folderId_ ")
    public Optional<Folder> findByIdWithCompleteAccessMap(Long folderId_);
    
//     @Query("select distinct folder "
//    + "from Folder folder "
//             + "join fetch folder.accessMap accessMap "
//    + "where folder.active in :activeStates "      
//    + "and folder.id = :folderId_ ")
//    public Optional<Folder> findByIdWithCompleteAccessMap(Long folderId_,Set<Boolean> activeStates);
//    
      @Query("select distinct folder "
    + "from Folder folder "
             + "join fetch folder.accessMap accessMap "
    + "where folder.active in :activeStates "      
    + "and folder.id = :folderId_ ")
    public Optional<Folder> findByIdWithCompleteAccessMap(Long folderId_,Set<Boolean> activeStates);
    
    
    //WARNING! only fetches accessMap stated in condition
    @Query("select distinct folder from Folder folder "
           + "join fetch folder.accessMap accessMap "
    + "where folder.active in :activeStates "      
    + "and (accessMap in :folderAccessMap_ ) ")
    public Set<Folder> findFoldersByAccessMap(Set<FolderAccess> folderAccessMap_,Set<Boolean> activeStates );
   
//     @Query("select distinct folder2 "
//    + "from Folder folder2 "
//             + "left join fetch folder2.accessMap accessMap2 "
//    + "where folder2.active in :activeStates "      
//    + "and folder2 in (select distinct folder from Folder folder "
//           + "join folder.accessMap accessMap "   
//    + "where accessMap in :folderAccessMap_  )")
//    public Set<Folder> findFoldersByAccessMap(Set<FolderAccess> folderAccessMap_,Set<Boolean> activeStates );
//   
    @Query("select folder from Folder folder "
           + "join fetch folder.accessMap accessMap "
    + "where folder = :staleFolder "
    + "and accessMap in :folderAccess")
    public Folder accessibleFolder (Folder staleFolder, Set<FolderAccess> folderAccess);
    
    @Query(value="select distinct am.authority from item it " +
            "join "+
            "folder_access_map am " +
            "on it.id=am.folder_id " +
            "where it.id=:folderId " +
            "and am.rights = 0",nativeQuery = true )
    public String getOwner(Long folderId);
    
    @Query(value="select distinct am.authority from item it " +
            "join "+
            "folder_access_map am " +
            "on it.id=am.folder_id " +
            "where it.id=:folderId " +
            "and am.rights in (0,1)",nativeQuery = true )
    public Set<String> getWriters(Long folderId);
    
    @Query(value="select distinct am.authority from item it " +
            "join "+
            "folder_access_map am " +
            "on it.id=am.folder_id " +
            "where it.id=:folderId ",nativeQuery = true )
    public Set<String> getReaders(Long folderId);
    
     @Query(value="select distinct am.authority from item it " +
            "join "+
            "folder_access_map am " +
            "on it.id=am.folder_id " +
            "where it.id in (:folderIds) " +
            "and am.rights in (0,1)",nativeQuery = true )
    public Set<String> getWriters(Set<Long> folderIds);
    
    @Query(value="select distinct am.authority from item it " +
            "join "+
            "folder_access_map am " +
            "on it.id=am.folder_id " +
            "where it.id in (:folderIds)",nativeQuery = true )
    public Set<String> getReaders(Set<Long> folderIds);
    
    
    
}
