/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.controller.UnauthorizedException;
import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.domain.projection.TagsProjection;
import com.azrul.kelichap.repository.DocumentDataRepository;
import com.azrul.kelichap.repository.FolderRepository;
import com.azrul.kelichap.repository.ItemRepository;
import com.vaadin.flow.component.notification.Notification;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author azrul
 */
@Service
public class DocumentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final String minioBucket;

    private final MinioClient minioClient;

    private final DocumentSearchService searchService;

    private final UserService userService;

    private final DocumentDataRepository docRepo;

    private final FolderRepository folderRepo;

    private final ItemRepository itemRepo;

    private final ConversionService conversionService;

    private static final Pattern camelCasePattern = Pattern.compile("(?<=[a-z])[A-Z]");

    private static final Pattern alphaNumericPattern = Pattern.compile("^[a-zA-Z0-9_:\\.\\-\\s]+$");

    public DocumentService(
            @Autowired MinioClient minioClient,
            @Autowired DocumentDataRepository docRepo,
            @Autowired FolderRepository folderRepo,
            @Autowired ItemRepository itemRepo,
            @Autowired ConversionService conversionService,
            @Autowired DocumentSearchService searchService,
            @Autowired UserService userService,
            @Value("${minio.bucket}") String minioBucket
    ) {
        this.minioClient = minioClient;
        this.docRepo = docRepo;
        this.folderRepo = folderRepo;
        this.minioBucket = minioBucket;
        this.itemRepo = itemRepo;
        this.conversionService = conversionService;
        this.searchService = searchService;
        this.userService = userService;
    }

    @Transactional
    public List<String> getReadersOfDocument(DocumentData doc, Map<String, UserRepresentation> allUsers) {
        Folder folder = folderRepo.getFolderWithCompleteAccessMap(doc.getParent());
        Set<String> userNames = folder.getAccessMap().stream().map(fa -> fa.getAuthority()).collect(Collectors.toSet());

        return userNames.stream().flatMap(userName -> {
            var user = allUsers.get(userName);
            if (user == null) {
                return Stream.empty();
            } else {
                return Stream.of(user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
            }
        }).collect(Collectors.toList());

    }

    @Transactional
    public List<String> getReadersOfDocumentWithoutCurrentUser(DocumentData doc, String currentUserName) {
        Folder folder = folderRepo.getFolderWithCompleteAccessMap(doc.getParent());
        Set<String> userNames = folder
                .getAccessMap()
                .stream()
                .filter(fa -> !StringUtils.equals(fa.getAuthority(), currentUserName))
                .map(fa -> fa.getAuthority()).collect(Collectors.toSet());

        return userNames.stream().flatMap(userName -> {
            var user = userService.getUser(userName);//allUsers.get(userName);
            if (user == null) {
                return Stream.empty();
            } else {
                return Stream.of(user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
            }
        }).collect(Collectors.toList());

    }

    public List<DocumentData> getDocsInFolder(
            Folder folder,
            String sortField,
            Boolean asc,
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {

        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);

        return docRepo.findAll(whereDocsFolderEquals(folder, accessMap, isActiveOnly), sort);
    }

    public List<Item> getItemsInFolder(
            Folder folder,
            String sortField,
            Boolean asc,
            Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Boolean isAdmin
    ) {

        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);

        var items = itemRepo.getItemsUnderFolder(folder, activeStates, currentAuthorities, sort, isAdmin);
        return items;
    }

    @Transactional
    public List<Folder> getAllDownStreamOfFolder(Folder folder) {
        String searchPrefix = folder.getPathToRootAsCSV(" ") + " " + folder.getId().toString();
        List<Folder> descendants = entityManager.createNativeQuery("select * from item where path_to_root ~>=~ :searchPrefix order by item.'level' DESC , item.id DESC").setParameter("searchPrefix", searchPrefix).getResultList();
        return descendants;
    }

    @Transactional
    public Integer countItemsInFolder(Folder folder,
            Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Boolean isAdmin) {
        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);
        return itemRepo.countItemsUnderFolder(folder, activeStates, currentAuthorities, isAdmin);
//        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
//        return Long.valueOf(itemRepo.count(whereItemsFolderEquals(folder, accessMap, isActiveOnly))).intValue();
    }

    public Optional<DocumentData> getDocById(
            Long id,
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
        return docRepo.findOne(whereDocIdFolderAccessIn(id, accessMap, isActiveOnly));
    }

//    @Transactional
//    public Optional<Folder> getFolderById(Long id) {
//        return folderRepo.findOne(whereFolderIdEquals(id, false));
//    }
    public Optional<Folder> getFolderById(
            Long id,
            Set<String> currentAuthorities
    ) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
        Set<Boolean> activeStates = Set.of(true, false);
        var of = folderRepo.findByIdWithCompleteAccessMap(id, activeStates);
        return of.filter(f -> f.getAccessMap().stream().anyMatch(fa -> accessMap.contains(fa)));
    }

    public Optional<Folder> getFolderById(
            Long id,
            Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Boolean isAdmin
    ) {

        final Set<FolderAccess> accessMap = (isAdmin) ? null: buildReaderAccessMap(currentAuthorities);
        
        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);
        var of = folderRepo.findByIdWithCompleteAccessMap(id, activeStates);
        if (accessMap != null) {
            return of.filter(f -> f.getAccessMap().stream().anyMatch(fa -> accessMap.contains(fa)));
        } else {
            return of;
        }
    }

    @Transactional
    public Folder createRootFolder(String folderName,
            Set<FolderAccess> sharedWith,
            String workflowId,
            Set<String> currentAuthorities) {
        return createFolder(folderName, null, sharedWith, workflowId, currentAuthorities);
    }

    @Transactional
    public Folder createFolder(String folderName,
            Folder parent,
            Set<FolderAccess> sharedWith,
            String workflowId,
            Set<String> currentAuthorities) {
        Matcher matcher = alphaNumericPattern.matcher(folderName);
        if (!matcher.matches()) {
            return null;
        }

        if (parent != null) {
            //check is active
            if (!this.isActive(parent)) {
                return null;
            }

            //check access
            Set<FolderAccess> accessMap = buildWriterAccessMap(currentAuthorities);

            if (!isFolderAccessible(parent, accessMap)) {
                return null;
            }
        }

        Folder folder = new Folder();
        Set<FolderAccess> accessMap = new HashSet<>();
        for (var s : sharedWith) {
            for (var currentAuthority : currentAuthorities) {
                if (!s.getAuthority().equals(currentAuthority)) {
                    var access = new FolderAccess();
                    access.setAuthority(s.getAuthority());
                    access.setRights(s.getRights());
                    accessMap.add(access);
                }
            }
        }

        //for current user
        for (var currentAuthority : currentAuthorities) {
            var access = new FolderAccess();
            access.setAuthority(currentAuthority);
            access.setRights(FolderRight.FOLDER_OWNER);
            accessMap.add(access);
        }
        //create folder
        folder.setAccessMap(accessMap);
        folder.setWorkflowId(workflowId);
        folder.setName(folderName);
        folder.setActive(Boolean.TRUE);
        folder.setParent(parent);
        if (parent != null) {
            folder.setRoot(parent.getRoot());
            folder.setLevel(parent.getLevel() + 1);
            folder.getPathToRoot().addAll(parent.getPathToRoot());
            folder.getPathToRoot().add(parent.getId());
        } else {//root
            folder.setRoot(folder);//set itself as root
            folder.setLevel(0);

        }

        //folder.setUpdateDate(LocalDateTime.now());
        Folder f = folderRepo.save(folder);

        searchService.indexFolder(f);

        //allow read access of assigned users to folder all the way to the root
        //(f, accessMap);
        return f;

    }

    public Folder refresh(Folder folder, Set<String> authorities) {
        return folderRepo.refresh(folder, authorities, FolderRight.FOLDER_OWNER);
    }

    public boolean isFolderAccessible(Folder folder, Set<FolderAccess> accessMap) {
        Folder folder_ = folderRepo.accessibleFolder(folder, accessMap);
        return folder_ != null;
    }

    @Transactional
    private List<Folder> findActiveFoldersWithCompleteAccessMapByAccessMap(List<FolderAccess> accessMap) {
        StringBuilder query = new StringBuilder(
                """
            select
                    f1_0.id,
                    am1_0.authority,
                    am1_0.rights,
                    f1_0.created_by,
                    f1_0.creation_date,
                    f1_0.last_modified_by,
                    f1_0.last_modified_date,
                    f1_0.level,
                    f1_0.name,
                    f1_0.folder_id,
                    f1_0.path_to_root,
                    f1_0.root_folder_id,
                    f1_0.version,
                    f1_0.workflow_id,
                    f1_0.active 
                from
                    item f1_0 
                join
                    folder_access_map am1_0 
                        on f1_0.id=am1_0.folder_id 
                where
                    f1_0.active in (true) 
                    and f1_0.id in (select
                        distinct f2_0.id 
                    from
                        item f2_0 
                    join
                        folder_access_map am2_0 
                            on f2_0.id=am2_0.folder_id 
                    where
                        (am2_0.authority, am2_0.rights) in (""");
        for (int i = 0; i < accessMap.size(); i++) {
            FolderAccess fa = accessMap.get(i);

            query.append("('" + fa.getAuthority() + "'," + fa.getRights().ordinal() + ")");
            if (i < (accessMap.size() - 1)) {
                query.append(",");
            }
        }
        query.append("""
                                 )         and f2_0.item_type='FOLDER') 
                              and f1_0.item_type='FOLDER'
                      \torder by f1_0.id""");
        return entityManager.createNativeQuery(query.toString(), Folder.class).getResultList();
    }

    @Transactional
    public void reassignToNewUser(String oldUserName, String newUserName) {
        List<FolderAccess> fas = List.of(
                new FolderAccess(oldUserName, FolderRight.CREATE_UPDATE_FOLDER),
                new FolderAccess(oldUserName, FolderRight.FOLDER_OWNER),
                new FolderAccess(oldUserName, FolderRight.READ_FOLDER),
                new FolderAccess(newUserName, FolderRight.CREATE_UPDATE_FOLDER),
                new FolderAccess(newUserName, FolderRight.FOLDER_OWNER),
                new FolderAccess(newUserName, FolderRight.READ_FOLDER)
        );
        List<Folder> folders = findActiveFoldersWithCompleteAccessMapByAccessMap(fas);//folderRepo.findFoldersByAccessMap(fas, Set.of(true));

        for (Folder folder : folders) {
            // Optional<Folder> ftemp = folderRepo.findByIdWithCompleteAccessMap(folder.getId());
            Set<FolderAccess> accessMap = folder.getAccessMap().stream().filter(
                    fa -> StringUtils.equals(newUserName, fa.getAuthority()) || StringUtils.equals(oldUserName, fa.getAuthority())
            ).collect(Collectors.toSet());

            if (accessMap.size() == 1) { //no overlap, only old user (OR new user) is assigned to these folders
                //simply replace old with new
                folder.getAccessMap().stream().forEach(fa -> {
                    if (StringUtils.equals(oldUserName, fa.getAuthority())) {
                        fa.setAuthority(newUserName);
                    }
                });
            } else { //there is overlap
                Set<FolderAccess> resultFolderAccess = new HashSet<>();
                resultFolderAccess.addAll(folder.getAccessMap());
                accessMapLoop:
                for (FolderAccess fa : accessMap) {

                    //oldUser:owner, newUser:c&u or reader => delete new user and replace ownerr with new user
                    if (fa.getRights() == FolderRight.FOLDER_OWNER && StringUtils.equals(oldUserName, fa.getAuthority())) {
                        for (FolderAccess fa2 : accessMap) {
                            if ((fa2.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                                    || fa2.getRights() == FolderRight.READ_FOLDER)
                                    && StringUtils.equals(newUserName, fa2.getAuthority())) {
                                resultFolderAccess.remove(fa2);
                                resultFolderAccess.remove(fa);

                                resultFolderAccess.add(new FolderAccess(newUserName, FolderRight.FOLDER_OWNER));
                                break accessMapLoop;
                            }
                        }
                    }

                    if (fa.getRights() == FolderRight.FOLDER_OWNER && StringUtils.equals(newUserName, fa.getAuthority())) {
                        for (FolderAccess fa2 : accessMap) {
                            if ((fa2.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                                    || fa2.getRights() == FolderRight.READ_FOLDER)
                                    && StringUtils.equals(oldUserName, fa2.getAuthority())) {
                                resultFolderAccess.remove(fa2);
                            }
                        }
                    }

                    if ((fa.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                            || fa.getRights() == FolderRight.READ_FOLDER)
                            && StringUtils.equals(newUserName, fa.getAuthority())) {
                        for (FolderAccess fa2 : accessMap) {
                            if ((fa2.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                                    || fa2.getRights() == FolderRight.READ_FOLDER)
                                    && StringUtils.equals(oldUserName, fa2.getAuthority())) {
                                if (fa.getRights() == FolderRight.CREATE_UPDATE_FOLDER && fa2.getRights() == FolderRight.READ_FOLDER) {
                                    resultFolderAccess.remove(fa2);
                                } else if (fa.getRights() == FolderRight.READ_FOLDER && fa2.getRights() == FolderRight.CREATE_UPDATE_FOLDER) {
                                    resultFolderAccess.remove(fa);
                                    resultFolderAccess.remove(fa2);
                                    resultFolderAccess.add(new FolderAccess(newUserName, FolderRight.CREATE_UPDATE_FOLDER));

                                } else {
                                    resultFolderAccess.remove(fa2);
                                }
                            }
                        }
                    }

                }
                folder.setAccessMap(resultFolderAccess);

            }

        }
        for (Folder folder : folders) {
            folderRepo.save(folder);
            searchService.emptyDocReadersList(folder);
            searchService.updateDocReadersList(folder);
        }

        //=======================
        //MISSING!!!!! -- reassign iin Typeesense
    }

    @Transactional
    public boolean deleteFolder(Folder folder,
            Set<String> currentAuthorities,
            Boolean isActiveOnly) {
        if (isActive(folder)) {
            Set<FolderAccess> accessMap = new HashSet<>();

            //only owner can delete
            for (var currentAuthority : currentAuthorities) {
                var access3 = new FolderAccess();
                access3.setAuthority(currentAuthority);
                access3.setRights(FolderRight.FOLDER_OWNER);
                accessMap.add(access3);
            }
            accessMap.retainAll(folder.getAccessMap());

            if (accessMap.isEmpty()) { //no intersection
                return false;
            }
            //do not allow deleting managed folder
            if (folder.getWorkflowId() != null) {
                return false;
            }
            List<Item> itemsDesc = itemRepo.getAllDescendantDocsUnderFolder(folder.getPathToRootInclusiveInCSV(" "));
            List<DocumentData> itemsChildren = docRepo.getDocsUnderFolder(folder);

            itemRepo.deleteAllDescendantFolderAccessUnderFolder(folder.getPathToRootInclusiveInCSV(" "));
            itemRepo.deleteAllDescendantItemsUnderFolder(folder.getPathToRootInclusiveInCSV(" "));

            //deleteFolderRecursively(folder, accessMap);
            folderRepo.delete(folder);

            for (Item item : itemsDesc) {
                if (item instanceof DocumentData doc) {
                    searchService.deleteDoc(doc);
                    deleteDocFromMinio(doc);
                }
            }

            for (Item item : itemsChildren) {
                if (item instanceof DocumentData doc) {
                    searchService.deleteDoc(doc);
                    deleteDocFromMinio(doc);
                }
            }
            return true;
        } else {
            return false;
        }
    }

//    @Transactional
//    private void deleteFolderRecursively(Item item, Set<FolderAccess> accessMap) {
//        List<Item> children = itemRepo.findAll(childrenWhereFolderEquals(item));
//        if (children.isEmpty()) {
//            return;
//        } else {
//            for (Item c : children) {
//                deleteFolderRecursively(c, accessMap);
//            }
//            deleteLeafItems((Folder) item, accessMap, true);
//            searchService.deleteFolder((Folder) item);
//            return;
//        }
//
//    }
//    @Transactional
//    public void deleteLeafItems(Folder folder,
//            Set<FolderAccess> accessMap,
//            Boolean isActiveOnly) {
//        if (!isActive(folder)) {
//            return;
//        }
//
//        List<Item> items = itemRepo.findAll(whereItemsFolderIsUpdatableAndEquals(folder, accessMap, isActiveOnly));
//
//        items.stream()
//                .filter(DocumentData.class::isInstance)
//                .map(DocumentData.class::cast)
//                .forEach(doc -> {
//                    searchService.deleteDoc(doc);
//                    deleteDocFromMinio(doc);
//                });
//
//        itemRepo.deleteAllInBatch(items);
//    }
    private void deleteDocFromMinio(DocumentData doc) {
        try {
            String fileLocation = doc.getFileLocation();
            var args = RemoveObjectArgs.builder().bucket(minioBucket).object(fileLocation).bypassGovernanceMode(true).build();
            minioClient.removeObject(args);
        } catch (IllegalArgumentException
                | ErrorResponseException
                | InternalException
                | IOException
                | InvalidKeyException
                | XmlParserException
                | InsufficientDataException
                | NoSuchAlgorithmException
                | InvalidResponseException
                | ServerException ex) {
            LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
        }
    }

    @Transactional
    public void deleteDoc(DocumentData doc, Set<String> currentAuthorities, Boolean isActiveOnly) {
        Folder parent = doc.getParent();
        if (!isActive(parent)) {
            return;
        }
        Set<FolderAccess> accessMap = buildWriterAccessMap(currentAuthorities);

        if (!isFolderAccessible(parent, accessMap)) {
            return;
        }
        searchService.deleteDoc(doc);
        deleteDocFromMinio(doc);
        docRepo.delete(doc);
    }

    private Set<FolderAccess> buildOwnerAccessMap(Set<String> currentAuthorities) {
        Set<FolderAccess> accessMap = new HashSet<>();
        for (var currentAuthority : currentAuthorities) {

            var access3 = new FolderAccess();
            access3.setAuthority(currentAuthority);
            access3.setRights(FolderRight.FOLDER_OWNER);
            accessMap.add(access3);
        }
        return accessMap;
    }

    private Set<FolderAccess> buildReaderAccessMap(Set<String> currentAuthorities) {
        Set<FolderAccess> accessMap = new HashSet<>();
        for (var currentAuthority : currentAuthorities) {
            var access = new FolderAccess();
            access.setAuthority(currentAuthority);
            access.setRights(FolderRight.CREATE_UPDATE_FOLDER);
            accessMap.add(access);

            var access2 = new FolderAccess();
            access2.setAuthority(currentAuthority);
            access2.setRights(FolderRight.READ_FOLDER);
            accessMap.add(access2);

            var access3 = new FolderAccess();
            access3.setAuthority(currentAuthority);
            access3.setRights(FolderRight.FOLDER_OWNER);
            accessMap.add(access3);
        }
        return accessMap;
    }

    private Set<FolderAccess> buildWriterAccessMap(Set<String> currentAuthorities) {
        Set<FolderAccess> accessMap = new HashSet<>();
        for (var currentAuthority : currentAuthorities) {
            var access = new FolderAccess();
            access.setAuthority(currentAuthority);
            access.setRights(FolderRight.CREATE_UPDATE_FOLDER);
            accessMap.add(access);

            var access3 = new FolderAccess();
            access3.setAuthority(currentAuthority);
            access3.setRights(FolderRight.FOLDER_OWNER);
            accessMap.add(access3);
        }
        return accessMap;
    }

    @Transactional
    public Integer countDocsInFolder(Folder folder,
            Set<String> currentAuthorities,
            Boolean isActiveOnly) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
        return Long.valueOf(docRepo.count(whereDocsFolderEquals(folder, accessMap, isActiveOnly))).intValue();
    }

    public List<Folder> getAllAccessibleFolders(
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {

        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);

        return folderRepo.findAll(whereFolderAccessIn(accessMap, isActiveOnly));
    }

    public Set<String> getAllAccessibleTags(Set<String> currentAuthorities) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);

        List<TagsProjection> tagsList = docRepo.findBy(whereDocsFolderAccessIn(accessMap, false), q
                -> q.project("tags") // query hint (not required)
                        .as(TagsProjection.class) // projection result class
                        .all());
        Set<String> tags = new HashSet<>();
        for (TagsProjection tagsDTO : tagsList) {
            tags.addAll(tagsDTO.getTags());
        }
        return tags;
    }

    public Page<DocumentData> getAllAccessibleDocs(
            Integer page,
            Integer countPerPage,
            String sortField,
            Boolean asc,
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {
        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, countPerPage, sort);
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
        return docRepo.findAll(whereDocsFolderAccessIn(accessMap, isActiveOnly), pageable);
    }

//    public Optional<Folder> getFolderByWorkflowId(String workflowId, Set<String> currentAuthorities, Boolean isActiveOnly) {
//        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);
//        return folderRepo.findFolderByWorkflowIdWithFolderAccess(workflowId,accessMap);
//    }
    @Transactional
    public Optional<Folder> getFolderOfDocIfUpdatable(DocumentData doc,
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {
        Set<FolderAccess> accessMap = buildWriterAccessMap(currentAuthorities);
        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);

        var of = folderRepo.findByIdWithCompleteAccessMap(doc.getParent().getId(), activeStates);
        return of.filter(f -> f.getAccessMap().stream().anyMatch(fa -> accessMap.contains(fa)));
    }

    public Set<Folder> getUpdatableFolders(Set<String> currentAuthorities/*, Boolean isActiveOnly*/) {

        Set<FolderAccess> accessMap = buildWriterAccessMap(currentAuthorities);

        return folderRepo.findAll(whereFolderAccessInAndIsActive(accessMap/*, isActiveOnly*/))
                .stream()
                .collect(Collectors.toSet());

    }

//    @Transactional
//    public Set<FolderAccess> getParentAccessMap(DocumentData doc) {
//        return doc.getParent().getAccessMap();
//    }
//    
//     @Transactional
//    public Set<FolderAccess> getParentAccessMap(DocumentData doc) {
//        return doc.getParent().getAccessMap();
//    }
    @Transactional
    public void addForlderAccessMap(Folder folder_, FolderAccess folderAccess) {

        if (isActive(folder_)) {
            Folder folder = folderRepo.getFolderWithCompleteAccessMap(folder_);
            Set<FolderAccess> accessMap = buildReaderAccessMap(Set.of(folderAccess.getAuthority()));
            if (isFolderAccessible(folder, accessMap)) {
                return; //access already exist
            }
            Optional<FolderAccess> folderAccessOfCurrentUser = folder.getAccessMap().stream()
                    .filter(fa -> StringUtils.equals(fa.getAuthority(), folderAccess.getAuthority()))
                    .findFirst();

            folderAccessOfCurrentUser.ifPresentOrElse(facu -> {
                folder.getAccessMap().remove(facu);
                folder.getAccessMap().add(folderAccess);
                folderRepo.save(folder);
                searchService.updateDocReadersList(folder);
            }, () -> {
                folder.getAccessMap().add(folderAccess);
                folderRepo.save(folder);
                searchService.updateDocReadersList(folder);
            });
        }
    }

    @Transactional
    public Set<FolderAccess> getAccessMap(Folder folder) {
        Folder f = folderRepo.getFolderWithCompleteAccessMap(folder);
        return f.getAccessMap();
    }

    @Transactional
    public String getOwner(Folder folder) {
        return folderRepo.getOwner(folder.getId());
    }

    @Transactional
    public Set<String> getReadersUserName(Folder folder) {
        return folderRepo.getReaders(folder.getId());
    }

    public Set<Folder> getFolders(Set<String> currentAuthorities, Boolean isActiveOnly) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);

        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);
        return folderRepo.findFoldersByAccessMap(accessMap, activeStates);

    }

//    public Page<Item> getFolders(
//            String sortField,
//            Boolean asc,
//            Integer page,
//            Integer countPerPage,
//            Integer maxPerPage,
//            Set<String> currentAuthorities,
//            Boolean isActiveOnly) {
//
//        Set<FolderAccess> accessMap = new HashSet<>();
//
//        for (var currentAuthority : currentAuthorities) {
//            var access = new FolderAccess();
//            access.setAuthority(currentAuthority);
//            access.setRights(FolderRight.CREATE_UPDATE_FOLDER);
//            accessMap.add(access);
//
//            var access2 = new FolderAccess();
//            access2.setAuthority(currentAuthority);
//            access2.setRights(FolderRight.READ_FOLDER);
//            accessMap.add(access2);
//
//            var access3 = new FolderAccess();
//            access3.setAuthority(currentAuthority);
//            access3.setRights(FolderRight.FOLDER_OWNER);
//            accessMap.add(access3);
//        }
//        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
//
//        Pageable pg = PageRequest.of(page, maxPerPage, sort);
//
//        Page<Item> items = folderRepo.findAll(whereFolderAccessIn(accessMap, isActiveOnly), pg).map(Item.class::cast);
//        return items;
//    }
    public Integer countFolders(Set<String> currentAuthorities,
            Boolean isActiveOnly) {
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);

        return Long.valueOf(folderRepo.count(whereFolderAccessIn(accessMap, isActiveOnly))).intValue();

    }

    @Transactional
    public Integer countUpdatableFolders(Set<String> currentAuthorities,
            Boolean isActiveOnly) {
        Set<FolderAccess> accessMap = buildWriterAccessMap(currentAuthorities);

        return Long.valueOf(folderRepo.count(whereFolderAccessIn(accessMap, isActiveOnly))).intValue();

    }

    public List<Folder> getRootFolders(
            String sortField,
            Boolean asc,
            Integer page,
            Integer maxPerPage,
            Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Boolean isAdmin
    ) {
        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);

        Pageable pg = PageRequest.of(page, maxPerPage, sort);

        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);
        Page<Folder> folders = folderRepo.findRootFoldersAndDownstreamIsAccessible(activeStates, currentAuthorities, FolderRight.FOLDER_OWNER, pg, isAdmin);

        return folders.getContent();
    }

    public Integer countRootFolders(Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Boolean isAdmin) {
        Set<Boolean> activeStates = isActiveOnly == true ? Set.of(true) : Set.of(true, false);
        Integer count = folderRepo.countRootFoldersAndDownstreamIsAccessible(activeStates, currentAuthorities, isAdmin);
        return count;
    }

//    public Optional<ImmutablePair<Folder, DocumentData>> addDoc(
//            final String workflowId,
//            final String currentUser,
//            final String currentRole,
//            final String tags,
//            final Long folderId,
//            final String foldername,
//            final FolderRight rights,
//            final String originalFileName,
//            final String mimeType,
//            final Long size,
//            byte[] fileContent) {
//        //Create return object
//        //Get the folder we need by workflowId
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        | folderId           | null          | null                           | not null                     | not null                     |
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        | workflowId         | null          | not null                       | null                         | not null                     |
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        | action             | create folder |query* folder based on workflow |query* folder based on folder |query* folder based on folder |
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        | alternate action   |               | create folder                  | create folder                |query* based on workflow      |
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        | alternate action 2 |               |                                |                              | create folder                |
//        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
//        //        * To query and create doc in the folder, we need CREATE_AND_UPDATE right on the folder
//        Optional<ImmutablePair<Folder, DocumentData>> opFolderAndDocument = Optional.empty();
//        opFolderAndDocument = createOrAddDoc(
//                folderId,
//                workflowId,
//                opFolderAndDocument,
//                originalFileName,
//                foldername,
//                currentUser,
//                currentRole,
//                rights,
//                mimeType,
//                tags,
//                size);
//
//        
//
//    }
    public Optional<DocumentData> updateDocTags(
            Long docId,
            Set<String> tags,
            Set<String> currentAuthorities,
            Boolean isActiveOnly
    ) {
        if (tags == null) {
            return Optional.empty();
        }
        Set<FolderAccess> accessMap = buildReaderAccessMap(currentAuthorities);

        return docRepo.findOne(whereDocIdFolderAccessIn(docId, accessMap, isActiveOnly)).flatMap(d -> {
            try {
                d.setTags(tags);
                if (isActive(d)) {
                    docRepo.save(d);
                    searchService.updateTags(tags, d);

                    return Optional.of(d);
                } else {
                    return Optional.empty();
                }
            } catch (Exception ex) {
                LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
                return Optional.empty();
            }

        });
    }

    //Only owner can change active state
    //Only writer (owner included) can change folder name and access
    //Disallow writer  (owner included) to change his own access
    @Transactional
    public Optional<Folder> updateFolder(
            Long folderId,
            String folderName,
            Boolean active,
            Set<FolderAccess> newFolderAccess,
            Set<String> currentAuthorities,
            Boolean isAdmin) {
        //active cannot be null
        if (active == null) {
            return Optional.empty();
        }

        //update folder
        return folderRepo.findByIdWithCompleteAccessMap(folderId).flatMap(folder -> {
            Boolean oldIsActive = this.isActive(folder);

            //if folder is inactive and we are not activating it, don't allow any changes
            if (oldIsActive == false && active == false) {
                return Optional.empty();
            }

            //check if newFolderAccess contains the current login user (currentAuthorities)
            //Disallow this as the current login user should not be allowed to change his own rights
            if (newFolderAccess.stream().filter(fa -> {
                return currentAuthorities.contains(fa.getAuthority())
                        && !folder.getAccessMap().contains(fa);
            }).count() > 0) {
                return Optional.empty();
            }

            if (isFolderAccessible(folder, buildOwnerAccessMap(currentAuthorities))) {
                if (active == true) {
                    if (folderName != null && !folderName.isBlank()) {
                        folder.setName(folderName);
                    }
                    if (folder.isRoot()) {
                        folder.setActive(active); //only update if root
                    }
                    assignAccessMap(folder, currentAuthorities, newFolderAccess);
                } else { //if we are making this folder inactive, only change 'active' field
                    if (folder.isRoot()) {
                        folder.setActive(active);
                    }
                }
            } else if (isFolderAccessible(folder, buildWriterAccessMap(currentAuthorities))) {
                if (active == true) {
                    if (folderName != null && !folderName.isBlank()) {
                        folder.setName(folderName);
                    }
                    assignAccessMap(folder, currentAuthorities, newFolderAccess);
                }
            } else if (isAdmin) {
                if (active == true) {
                    if (folder.isRoot()) {
                        folder.setActive(active); //only update if root
                    }
                    assignAccessMap(folder, currentAuthorities, newFolderAccess);
                } else { //if we are making this folder inactive, only change 'active' field
                    if (folder.isRoot()) {
                        folder.setActive(active);
                    }
                }
            }

            //save
            folderRepo.save(folder);

            //setReadibilityAllTheWayToRoot(f, newFolderAccess);
            folderRepo.flush();

            searchService.updateFolder(folder, folderName, active, oldIsActive);

            //return updated folder
            return Optional.of(folder);
        });
    }

    public List<Folder> getAllFoldersWithIds(List<Long> folderIds) {
        return folderRepo.findAll(whereFolderIdIn(folderIds));
    }

//    private void setReadibilityAllTheWayToRoot(Folder folder, Set<FolderAccess> newFolderAccess) {
//        if (isActive(folder)) {
//            //Find new auths and filter out owner
//            Set<String> auths = newFolderAccess
//                    .stream()
//                    .filter(fa -> fa.getRights() != FolderRight.FOLDER_OWNER)
//                    .map(fa -> fa.getAuthority())
//                    .distinct()
//                    .collect(Collectors.toSet());
//
//            //Find all folder from here (folder) to root
//            List<Long> fids = folder.getPathToRoot();
//            List<Folder> foldersToBeUpdated = getAllFoldersWithIds(fids);
//
//            //for each one of them
//            for (Folder f : foldersToBeUpdated) {
//                //if auth is not in  their current access list
//                for (String auth : auths) {
//                    if (f.getAccessMap().stream().filter(fa -> StringUtils.equals(auth, fa.getAuthority())).count() == 0) {
//                        //add them as reader
//                        f.getAccessMap().add(new FolderAccess(auth, FolderRight.READ_FOLDER));
//                    }
//                }
//            }
//
//            //save everything again
//            folderRepo.saveAll(foldersToBeUpdated);
//
//            for (Folder f : foldersToBeUpdated) {
//                searchService.updateDocReadersList(f);
//            }
//        }
//
//    }
    private void assignAccessMap(Folder folder, Set<String> currentAuthorities, Set<FolderAccess> newFolderAccess) {
        if (isActive(folder)) {
            if (newFolderAccess == null) {
                return;
            }

            //first, retain 
            // 1) the owner 
            // 2) current user who is making these changes
            Set<FolderAccess> accessMap = folder.getAccessMap()
                    .stream()
                    .filter(fa
                            -> fa.getRights() == FolderRight.FOLDER_OWNER
                    || currentAuthorities.contains(fa.getAuthority())
                    )
                    .collect(Collectors.toSet());

            //then add the new access
            accessMap.addAll(newFolderAccess);
            folder.setAccessMap(accessMap);
        }
    }

    //@Transactional
    public Boolean isActive(Item item) {
        if (item == null) {
            return false;
        }
        if (item instanceof Folder folder) {

            if (folder.isRoot()) {
                return folder.getActive();
            } else {
                return folder.getRoot().getActive();//folderRepo.findById(folder.getId()).map(f -> f.getRoot().getActive()).orElse(false);
            }
        } else {
            DocumentData doc = (DocumentData) item;
            Folder folder = doc.getParent();
            if (folder.isRoot()) {
                return folder.getActive();
            } else {
                return folder.getRoot().getActive();//folderRepo.findById(folder.getId()).map(f -> f.getRoot().getActive()).orElse(false);
            }
        }
    }

    @Transactional
    public Optional<DocumentData> addDocument(
            final String workflowId,
            final String currentUser,
            final Set<String> tags,
            final Long folderId,
            final String origFileName,
            final String origMimeType,
            final Long origSize,
            final byte[] origFileContent,
            final String folderName,
            final Set<FolderAccess> folderAccess) {

        //if file is MS Office doc, then convert to PDF
        String fileName = Stream.of(origMimeType).map(m -> {
            if (m.equals("application/pdf")) {
                return origFileName;
            } else {

                return origFileName + ".pdf";
            }
        }).findAny().get();

        byte[] fileContent = Stream.of(origMimeType).map(m -> {
            if (m.equals("application/pdf")) {
                return origFileContent;
            } else {
                try {
                    return conversionService.convertToPDF(origFileContent, origFileName);
                } catch (IOException ex) {
                    LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
                }
                return new byte[0];
            }
        }).findAny().get();

        Long size = Stream.of(origMimeType).map(m -> {
            if (m.equals("application/pdf")) {
                return origSize;
            } else {
                return Integer.toUnsignedLong(fileContent.length);
            }
        }).findAny().get();

        String mimeType = "application/pdf";

        //Create return object
        //Get the folder we need by workflowId
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        | folderId           | null          | null                           | not null                     | not null                     |
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        | workflowId         | null          | not null                       | null                         | not null                     |
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        | action             | create folder |query* folder based on workflow |query* folder based on folder |query* folder based on folder |
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        | alternate action   |               | create folder                  | create folder                |query* based on workflow      |
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        | alternate action 2 |               |                                |                              | create folder                |
        //        +--------------------+---------------+--------------------------------+------------------------------+------------------------------+
        //        * To query and create doc in the folder, we need CREATE_AND_UPDATE right on the folder
        Optional<DocumentData> opDocument = Optional.empty();
        if (folderId == null && workflowId == null) {

            opDocument = Optional.of(addDocumentToNewRootFolder(
                    workflowId,
                    currentUser,
                    tags,
                    fileName,
                    mimeType,
                    size,
                    origFileName,
                    origMimeType,
                    origSize,
                    folderName,
                    folderAccess
            ));
        } else if (folderId != null && workflowId == null) {

            var fnd = folderRepo.findByIdWithCompleteAccessMap(folderId).map(folder -> {

                if (!isActive(folder)) {
                    return null;
                }
                //System.out.println("access map:"+folder.getAccessMap().iterator().next());
                Set<FolderAccess> accessMap = buildWriterAccessMap(Set.of(currentUser));
                if (isFolderAccessible(folder, accessMap)) {

                    return addDocumentToExistingFolder(currentUser,
                            folder,
                            tags,
                            fileName,
                            mimeType,
                            size,
                            origFileName,
                            origMimeType,
                            origSize);
                } else {
                    return null; //folder exist but I have no access
                }
            }).orElseGet(() -> {
                return addDocumentToNewRootFolder(
                        workflowId,
                        currentUser,
                        tags,
                        fileName,
                        mimeType,
                        size,
                        origFileName,
                        origMimeType,
                        origSize,
                        folderName,
                        folderAccess);
            });
            opDocument = Optional.ofNullable(fnd);
        } else if (folderId == null && workflowId != null) {
//            var fnd = folderRepo.findFolderByWorkflowIdAccessibleByAuthorities(workflowId, Set.of(currentUser)).map(folder -> {
//                if (!isActive(folder)) {
//                    return null;
//                }
//                Set<FolderAccess> accessMap = buildWriterAccessMap(Set.of(currentUser));
//                if(isFolderAccessible(folder, accessMap)){
//                    return addDocumentToExistingFolder(currentUser,
//                            folder,
//                            tags,
//                            fileName,
//                            mimeType,
//                            size,
//                            origFileName,
//                            origMimeType,
//                            origSize);
//                } else {
//                    return null;
//                }
//
//            }).orElseGet(() -> {
//
//                return addDocumentToNewRootFolder(
//                        workflowId,
//                        currentUser,
//                        tags,
//                        fileName,
//                        mimeType,
//                        size,
//                        origFileName,
//                        origMimeType,
//                        origSize,
//                        folderName,
//                        folderAccess
//                );
//            });
//            opDocument = Optional.ofNullable(fnd);
        } else if (folderId != null && workflowId != null) {
            var fnd = folderRepo.findByIdWithCompleteAccessMap(folderId).map(folder -> {
                if (!isActive(folder)) {
                    return null;
                }
                Set<FolderAccess> accessMap = buildReaderAccessMap(Set.of(currentUser));
                if (isFolderAccessible(folder, accessMap)) {
                    return addDocumentToExistingFolder(currentUser,
                            folder,
                            tags,
                            fileName,
                            mimeType,
                            size,
                            origFileName,
                            origMimeType,
                            origSize);
                } else {
                    return null;
                }

            }).orElseGet(() -> {

                var fnd2 = folderRepo.findFolderByWorkflowIdAccessibleByAuthorities(workflowId, Set.of()).map(folder -> {
                    if (!isActive(folder)) {
                        return null;
                    }
                    Set<FolderAccess> accessMap = buildWriterAccessMap(Set.of(currentUser));
                    if (isFolderAccessible(folder, accessMap)) {
                        return addDocumentToExistingFolder(currentUser,
                                folder,
                                tags,
                                fileName,
                                mimeType,
                                size,
                                origFileName,
                                origMimeType,
                                origSize);
                    } else {
                        return null;
                    }
                }).orElseGet(() -> {
                    return addDocumentToNewRootFolder(
                            workflowId,
                            currentUser,
                            tags,
                            fileName,
                            mimeType,
                            size,
                            origFileName,
                            origMimeType,
                            origSize,
                            folderName,
                            folderAccess);
                });
                return fnd2;
            });
            opDocument = Optional.ofNullable(fnd);
        }

        return opDocument.flatMap(document -> {
            if (document == null) {
                return Optional.empty();
            }
            final Folder folder = document.getParent();
            final String fileName_ = document.getName();
            final Set<String> tagSet = document.getTags();

            final Long docId = document.getId();
            final String folderName1 = folder.getName();
            final Boolean active = folder.getActive();
            final Set<FolderAccess> accessMap = folder.getAccessMap();
            final Integer version = document.getVersion();

            //Save to other storage
            ExecutorService threadPool = Executors.newFixedThreadPool(2);
            try {

                byte[] copy1 = fileContent.clone();
                byte[] copy2 = fileContent.clone();

                // Submit tasks to the executor
                Future<String> saveToStorageFuture = threadPool.submit(() -> saveToStorage(minioClient,
                        minioBucket,
                        folder.getId(),
                        version,
                        fileName_,
                        copy1,
                        origMimeType,
                        origFileContent
                ));

                Future<String> indexForSearchFuture = threadPool.submit(() -> searchService.indexDocument(
                        workflowId,
                        docId,
                        folderName1,
                        fileName_,
                        folder.getId(),
                        folder.getRoot().getId(),
                        tagSet,
                        accessMap,
                        size.intValue(),
                        version,
                        active,
                        folder.getPathToRootInclusive(),
                        copy2
                ));

                // Wait for both tasks to complete
                String minioVersionId = saveToStorageFuture.get();
                String responseIndex = indexForSearchFuture.get();

            } catch (InterruptedException | ExecutionException ex) {
                LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
            } finally {
                // Shut down the executor service
                threadPool.shutdown();
            }

            //Return
            return Optional.of(document);

        });

    }

//    private boolean accessExist(Folder folder, String authority) {
//        Set<FolderAccess> accessMap = buildReaderAccessMap(Set.of(authority));
//        return isFolderAccessible(folder, accessMap);
//
//    }
    @Transactional
    private DocumentData addDocumentToNewRootFolder(
            final String workflowId,
            final String currentUser,
            final Set<String> tags,
            final String fileName,
            final String mimeType,
            final Long size,
            final String origFileName,
            final String origMimeType,
            final Long origSize,
            final String folderName,
            final Set<FolderAccess> folderAccessMap
    ) {
        //else, create a new Folder with a new Document
        Integer version = 1;

        DocumentData document = new DocumentData();

        //folder.setUpdateDate(LocalDateTime.now());
        FolderAccess fa = new FolderAccess();
        fa.setAuthority(currentUser);
        fa.setRights(FolderRight.FOLDER_OWNER);
        Set<FolderAccess> folderAccessMap2 = new HashSet<>();
        folderAccessMap2.add(fa);
        Map<String, FolderAccess> faMap = folderAccessMap.stream().collect(Collectors.toMap(FolderAccess::getAuthority, v -> v)); //flatten authority, just in case multiple authority in folderAccessMap. We cannot allow multiple authority
        for (var e : faMap.entrySet()) {
            if (!FolderRight.FOLDER_OWNER.equals(e.getValue().getRights())) { //disallow multiple Folder Owner
                folderAccessMap2.add(e.getValue());
            }
        }

        Folder folder = createRootFolder(folderName,
                folderAccessMap2,
                workflowId,
                Set.of(currentUser));

        document.setName(fileName);
        document.setMimeType(mimeType);
        document.setTags(tags);
        document.setFileLocation("-");
        document.setVersion(version);
        document.setParent(folder);
        document.setRoot(folder);
        document.setSize(size);
        document.setOriginalName(origFileName);
        document.setOriginalMimeType(origMimeType);
        document.setSize(origSize);

        final DocumentData documentRes = docRepo.saveAndFlush(document);
        folder.getChildren().add(document);
        final Folder folderRes = folderRepo.save(folder);

        docRepo.updateFileLocation(documentRes.getId(), folderRes.getId() + "/" + fileName + "/" + version);
        if (folderName == null || folderName.isBlank()) {
            folderRes.setName("Folder-" + folderRes.getId());
            folderRepo.updateFolderName(folderRes.getId(), folderRes.getName());

        }
        return documentRes;
    }

    @Transactional
    private DocumentData addDocumentToExistingFolder(
            final String currentUser,
            final Folder folder,
            final Set<String> tags,
            final String fileName,
            final String mimeType,
            final Long size,
            final String origFileName,
            final String origMimeType,
            final Long origSize) throws UnauthorizedException {

        Optional<DocumentData> oDoc = Optional.empty();
        for (var i : folder.getChildren()) {
            if (i instanceof DocumentData d) {
                if (d.getName().equals(fileName)) {
                    oDoc = Optional.of(d);
                }
            }
        }

        return oDoc.map(doc -> {
            //Add a new version of an existing document
            if (tags != null) {
                doc.getTags().addAll(tags);
            }
            doc.setSize(size);
            Integer version = doc.incrementVersion();
            doc.setFileLocation(folder.getId() + "/" + fileName + "/" + version);
            doc = docRepo.save(doc);

            return doc;

        }).orElseGet(() -> {

            //Add new file to existing folder
            Integer version = 1;
            DocumentData document = new DocumentData();
            document.setAccessMap(new HashSet());
            document.setFileLocation(folder.getId() + "/" + fileName + "/" + version);
            document.setName(fileName);
            if (tags != null) {
                document.getTags().addAll(tags);
            }
            document.setMimeType(mimeType);
            document.setSize(size);
            document.setVersion(version);
            document.setParent(folder);
            document.setRoot(folder.getRoot());
            document.setLevel(folder.getLevel() + 1);
            document.setOriginalName(origFileName);
            document.setOriginalMimeType(origMimeType);
            document.setSize(origSize);
            document.setParent(folder);
            document.getPathToRoot().addAll(folder.getPathToRoot());
            document.getPathToRoot().add(folder.getId());

            document = docRepo.save(document);
//            folder.getChildren().add(document);
//            folderRepo.save(folder);
            return document;
        });
    }

    private static String saveToStorage(MinioClient minioClient,
            String bucket,
            Long folderId,
            Integer version,
            String fileName,
            byte[] content,
            String origMimeType,
            byte[] origContent) {

        try {

            var response = minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(folderId + "/" + fileName + "/" + version).stream(
                    new ByteArrayInputStream(content), content.length, -1).build());
            if (!origMimeType.equals("application/pdf")) {
                minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(folderId + "/" + fileName + "/" + version + "/original").stream(
                        new ByteArrayInputStream(origContent), origContent.length, -1).build());
            }

            return response.versionId();

        } catch (ErrorResponseException
                | InsufficientDataException
                | InternalException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException ex) {
            LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
            return "ERROR";
        }

    }

    public void downloadDoc(String fileLocation, OutputStream out) {
        try {
            minioClient.getObject(
                    GetObjectArgs
                            .builder()
                            .bucket(minioBucket)
                            .object(fileLocation)
                            .build()
            ).transferTo(out);

        } catch (ErrorResponseException
                | InsufficientDataException
                | InternalException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException ex) {

            LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
        }
    }

    public byte[] downloadDoc(String fileLocation) {
        try {
            return minioClient.getObject(
                    GetObjectArgs
                            .builder()
                            .bucket(minioBucket)
                            .object(fileLocation)
                            .build()
            ).readAllBytes();

        } catch (ErrorResponseException
                | InsufficientDataException
                | InternalException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException ex) {
            LogFactory.getLog(DocumentService.class.getName()).fatal("Error", ex);
        }
        return new byte[0];
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    private Optional<DocumentData> findDocByName(String name, Set<Item> items) {
        for (var i : items) {
            if (i instanceof DocumentData d) {
                if (d.getName().equals(name)) {
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    public Integer getPageWhereFolderIs(Long folderId, String sortField, Boolean asc, String username, Integer countPerPage, Boolean isActiveOnly, Boolean isAdmin) {

        Integer row = getRowFolderOfId(username, folderId, sortField, asc, isActiveOnly, isAdmin);
        if (row == null) {
            return 1;
        }
        double page = (double) row / (double) countPerPage;
        return (int) Math.ceil(page);
    }

    public Integer getRowFolderOfId(String username, Long id, String orderby, Boolean asc, Boolean isActiveOnly, Boolean isAdmin) {
        //convert camelcase to underscore for orderby
        Matcher m = camelCasePattern.matcher(orderby);
        String order_by = m.replaceAll(match -> "_" + match.group().toLowerCase());
        String activeCondition = isActiveOnly ? " AND ITEM.active=true" : "";

        //query
        String sort = asc ? "ASC" : "DESC";
        String query = "SELECT RowNumber FROM ("
                + "SELECT ITEM.id,ROW_NUMBER() OVER (ORDER BY ITEM." + order_by + " " + sort + ") RowNumber "
                + "FROM public.item ITEM "
                + "WHERE ITEM.item_type='FOLDER' "
                + "AND ITEM.folder_id ISNULL " //we only want root folders
                + " " + activeCondition + " " //if isactiveonly chosen, we only want active folders
                //+ "AND '" + username + "' in ("
                + "AND (:username in ("
                + "SELECT FAM.authority "
                + "FROM public.folder_access_map FAM "
                + "JOIN public.item ITEM2 on FAM.folder_id=ITEM2.id "
                + "WHERE ITEM2.root_folder_id = ITEM.id "
                + ") or :isAdmin=true) " //we only wants readable folders. If user is in folder_access_map, the user should be able to read the folder

                + ") A "
                + "WHERE A.id=:id";
        Integer row = (Integer) entityManager.createNativeQuery(query, Integer.class)
                .setParameter("id", id)
                .setParameter("username", username)
                .setParameter("isAdmin", isAdmin)
                .getSingleResult();

        return row;
    }

    static Specification<DocumentData> whereRootEquals(Folder rootFolder) {
        return (doc, cq, cb) -> {

            return cb.and(
                    cb.equal(doc.get("root"), rootFolder)
            );

        };
    }

    static Specification<DocumentData> whereWorkflowIdEquals(String workflowId, Set<FolderAccess> accessMap, Boolean isActiveOnly) {

        return (doc, cq, cb) -> {

            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, doc.get("parent").get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("parent").get("workflowId"), workflowId),
                        cb.equal(doc.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("paerent").get("workflowId"), workflowId)
                );
            }
        };
    }

    public String[] getSupportedFileExtension() {
        return ArrayUtils.addFirst(conversionService.getConvertibleFileExt(), ".pdf");
    }

    //Find out
    static Specification<Folder> whereFolderContainsAndHasAccess(
            DocumentData doc,
            Set<FolderAccess> accessMap,
            Boolean isActiveOnly
    ) {
        return (folder, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, folder.get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.isMember(doc, folder.get("children")),
                        cb.equal(folder.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.isMember(doc, folder.get("children"))
                );
            }

        };
    }

    static Specification<DocumentData> whereDocsFolderAccessIn(Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (doc, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, doc.get("parent").get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                return cb.or(preds.toArray(Predicate[]::new));
            }
        };
    }

    static Specification<DocumentData> whereDocIdFolderAccessIn(Long docId, Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (doc, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, doc.get("parent").get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.equal(doc.get("id"), docId),
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                return cb.and(
                        cb.equal(doc.get("id"), docId),
                        cb.or(preds.toArray(Predicate[]::new))
                );
            }
        };
    }

    //Find out
    static Specification<Folder> whereFolderAccessIn(Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (folder, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, folder.get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(folder.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                return cb.or(preds.toArray(Predicate[]::new));
            }
        };
    }

    //Find out
    static Specification<Folder> whereFolderAccessInAndIsActive(Set<FolderAccess> accessMap) {
        return (folder, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, folder.get("accessMap")));
            }
            return cb.and(
                    cb.or(preds.toArray(Predicate[]::new)),
                    cb.equal(folder.get("root").get("active"), Boolean.TRUE)
            );
        };
    }

    static Specification<Folder> whereFolderIsRoot(Boolean isActiveOnly) {
        return (folder, cq, cb) -> {
            if (isActiveOnly) {
                return cb.and(
                        cb.equal(folder.get("active"), Boolean.TRUE),
                        cb.isNull(folder.get("parent"))
                );
            } else {
                return cb.and(
                        cb.isNull(folder.get("parent"))
                );
            }
        };

    }

    static Specification<Folder> whereFolderIsRootAndDownstreamHasAccessIn(Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (folder, cq, cb) -> {

            Subquery sub = cq.subquery(Folder.class);
            Root subRoot = sub.from(Folder.class);
            Set<Predicate> subpreds = new HashSet<>();
            for (var access : accessMap) {
                subpreds.add(cb.isMember(access, subRoot.get("accessMap")));
            }
            Subquery sub2 = sub.select(subRoot).where(cb.or(subpreds.toArray(Predicate[]::new)));

            if (isActiveOnly) {
                return cb.and(
                        cb.equal(folder.get("active"), Boolean.TRUE),
                        cb.isNull(folder.get("parent")),
                        cb.in(sub2)
                );
            } else {
                return cb.and(
                        cb.isNull(folder.get("parent")),
                        cb.in(sub2)
                );
            }
        };

    }

    static Specification<Folder> whereFolderIsRootAndAccessIn(Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (folder, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, folder.get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(folder.get("active"), Boolean.TRUE),
                        cb.isNull(folder.get("parent"))
                );
            } else {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.isNull(folder.get("parent"))
                );
            }
        };

    }

    //Find out
    static Specification<Folder> whereFolderIdAccessIn(Long folderId, Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (folder, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, folder.get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(folder.get("root").get("active"), Boolean.TRUE),
                        cb.equal(folder.get("id"), folderId)
                );
            } else {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(folder.get("id"), folderId)
                );
            }
        };
    }

    @Transactional
    private Specification<Folder> whereFolderIdEquals(Long folderId, Boolean isActiveOnly) {
        return (folder, cq, cb) -> {

            if (isActiveOnly) {
                return cb.and(
                        cb.equal(folder.get("root").get("active"), Boolean.TRUE),
                        cb.equal(folder.get("id"), folderId)
                );
            } else {
                return cb.and(
                        cb.equal(folder.get("id"), folderId)
                );
            }
        };
    }

    static Specification<Folder> whereFolderIdIn(List<Long> folderIds) {
        return (folder, cq, cb) -> {
            return cb.isTrue(folder.get("id").in(folderIds));
        };
    }

    static Specification<Folder> whereFolderId(List<Long> folderIds) {
        return (folder, cq, cb) -> {
            return cb.isTrue(folder.get("id").in(folderIds));
        };
    }

    static Specification<DocumentData> docsWhereFolderAccessIn(
            Set<DocumentData> docs,
            Set<FolderAccess> accessMap,
            Boolean isActiveOnly
    ) {
        return (document, cq, cb) -> {
            //evaluate if a document (in DB) has a folder having the right access. If yes, we want that document
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, document.get("parent").get("accessMap")));
            }

            //evaluate if a document (in DB) is part of the set of docs passed in as param
            Set<Predicate> preds2 = new HashSet<>();
            for (var doc : docs) {
                preds2.add(cb.equal(document, doc));
            }

            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds2.toArray(Predicate[]::new)), //docs that confirm to access map AND
                        cb.or(preds.toArray(Predicate[]::new)), //docs that is part of docs input param
                        cb.equal(document.get("root").get("active"), Boolean.TRUE)
                );
            } else {
                //we want the intersection
                return cb.and(
                        cb.or(preds2.toArray(Predicate[]::new)), //docs that confirm to access map AND
                        cb.or(preds.toArray(Predicate[]::new)) //docs that is part of docs input param
                );
            }
        };
    }

    //Find out
    static Specification<Item> whereItemsFolderEquals(Folder folder, Set<FolderAccess> accessMap, Boolean isActiveOnly) {

        return (item, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, item.get("parent").get("accessMap")));
            }
            if (isActiveOnly) {

                return cb.or(
                        cb.and(
                                cb.equal(item.type(), cb.literal(Folder.class)), //if folder, allow access no matter what the access rights
                                cb.equal(item.get("root").get("active"), Boolean.TRUE),
                                cb.equal(item.get("parent"), folder)
                        ),
                        cb.and(
                                cb.equal(item.type(), cb.literal(DocumentData.class)), //if document, allow access oonly iof parent is accessible
                                cb.or(preds.toArray(Predicate[]::new)),
                                cb.equal(item.get("root").get("active"), Boolean.TRUE),
                                cb.equal(item.get("parent"), folder)
                        )
                );
            } else {
                return cb.or(
                        cb.and(
                                cb.equal(item.type(), cb.literal(Folder.class)),
                                cb.equal(item.get("parent"), folder)
                        ),
                        cb.and(
                                cb.equal(item.type(), cb.literal(DocumentData.class)),
                                cb.or(preds.toArray(Predicate[]::new)),
                                cb.equal(item.get("parent"), folder)
                        )
                );
            }

        };
    }

    static Specification<DocumentData> whereDocsFolderEquals(Folder folder, Set<FolderAccess> accessMap, Boolean isActiveOnly) {

        return (doc, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, doc.get("parent").get("accessMap")));
            }
            if (isActiveOnly) {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("root").get("active"), Boolean.TRUE),
                        cb.equal(doc.get("parent"), folder)
                );
            } else {
                return cb.and(
                        cb.or(preds.toArray(Predicate[]::new)),
                        cb.equal(doc.get("parent"), folder)
                );
            }

        };
    }

    static Specification<Item> childrenWhereFolderEquals(Item folder) {
        return (item, cq, cb)
                -> cb.equal(item.get("parent"), folder);
    }

    static Specification<Item> whereItemsFolderIsUpdatableAndEquals(Folder folder, Set<FolderAccess> accessMap, Boolean isActiveOnly) {
        return (item, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            for (var access : accessMap) {
                preds.add(cb.isMember(access, item.get("parent").get("accessMap")));
            }

            if (isActiveOnly) {
                return cb.and(
                        cb.equal(item.get("parent"), folder),
                        cb.equal(item.get("root").get("active"), Boolean.TRUE),
                        cb.or(preds.toArray(Predicate[]::new))
                );
            } else {
                return cb.or(preds.toArray(Predicate[]::new));
            }

        };
    }

//    public Revisions<Long,DocumentData> getRevision(Integer page, Boolean asc){
//        Integer countPerPage = 3;
//        
//        Sort sort = Sort.by(asc == true ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
//        Pageable pageable = PageRequest.of(page, countPerPage, sort);
//   
//        docRepo.
//    }
}
