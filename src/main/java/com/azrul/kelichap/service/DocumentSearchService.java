/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.dto.basic.SearchDataDTO;
import com.azrul.kelichap.repository.DocumentDataRepository;
import com.azrul.kelichap.repository.FolderRepository;
import com.azrul.kelichap.dto.reqresp.SearchDocumentsResponseDTO;
import static com.azrul.kelichap.service.DocumentService.whereRootEquals;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.typesense.api.Client;
import org.typesense.api.Document;
import org.typesense.api.Documents;
import org.typesense.model.DeleteDocumentsParameters;
import org.typesense.model.ImportDocumentsParameters;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.model.UpdateDocumentsParameters;

/**
 *
 * @author azrul
 */
@Service
public class DocumentSearchService {

  
    private final DocumentDataRepository docRepo;
    private final FolderRepository folderRepo;
    private final SearchEngine searchEngine;

    public DocumentSearchService(
            @Autowired DocumentDataRepository docRepo,
            @Autowired FolderRepository folderRepo,
            @Autowired SearchEngine searchEngine
           
    ) {
        //  this.typesenseNotesCollection = typesenseNotesCollection;
        this.docRepo = docRepo;
        this.folderRepo = folderRepo;
        this.searchEngine = searchEngine;
    }

    public SearchDocumentsResponseDTO search(String query,
            Set<Item> selectedItems,
            Set<Folder> accessibleFolders,
            Integer page,
            Integer countPerPage,
            String order,
            Boolean isActiveOnly,
            Set<String> currentAuthorities,
            Boolean searchInAllDownStream, 
            Boolean isAdmin) {
        if (page == null) {
            page = 1;
        }
        if (countPerPage == null) {
            countPerPage = 3;
        }

        var respDTO = new SearchDocumentsResponseDTO();
        try {

            Set<String> folderIds = new HashSet<>();

            folderIds.addAll(selectedItems.stream()
                    .filter(item -> item instanceof Folder)
                    .map(Folder.class::cast)
                    .flatMap(folder -> {
                        return Stream.of(folder.getId().toString());

                    })
                    .collect(Collectors.toSet()));

            if (selectedItems.isEmpty()) {
                folderIds.addAll(accessibleFolders
                        .stream()
                        .map(a -> a.getId().toString())
                        .collect(Collectors.toList()));
            }

            Set<String> docIDs = selectedItems.stream()
                    .filter(item -> item instanceof DocumentData)
                    .map(DocumentData.class::cast)
                    .map(DocumentData::getId)
                    .map(id -> id.toString())
                    .collect(Collectors.toSet());

            StringBuilder docsFilterBuilder = buildSearchFilter(
                    true,
                    currentAuthorities,
                    isActiveOnly,
                    folderIds,
                    docIDs, 
                    isAdmin);

            SearchParameters searchDocumentParameters = new SearchParameters()
                    .q(query)
                    .queryBy("workflowId,folderName,content,tags")
                    .page(page)
                    .perPage(countPerPage)
                    .filterBy(docsFilterBuilder.toString())
                    //.sortBy("ratings_count:desc")
                    .sortBy(order)
                    .prefix("true,true,true,true");

            SearchResult searchDocumentResult = searchEngine.get4Docs().search(searchDocumentParameters);
            Set<SearchDataDTO> allSearchData = new LinkedHashSet<>();//to preserve insertion order
            for (var hit : searchDocumentResult.getHits()) {
                SearchDataDTO search = new SearchDataDTO();
                search.setDocId(((Integer) hit.getDocument().get("docId")).longValue());
                var wfId = hit.getDocument().get("workflowId");
                if (wfId != null) {
                    search.setWorkflowId(hit.getDocument().get("workflowId").toString());
                }
                search.setFileName(hit.getDocument().get("fileName").toString());
                search.setPage((Integer) hit.getDocument().get("page"));
                search.setFolderId(((Integer) hit.getDocument().get("folderId")).longValue());
                search.setRootFolderId(((Integer) hit.getDocument().get("rootFolderId")).longValue());
                search.setFolderName(hit.getDocument().get("folderName").toString());
                search.setVersion((Integer) hit.getDocument().get("version"));
                search.setActive((Boolean) hit.getDocument().get("active"));
                search.setType(hit.getDocument().get("type_").toString());
                if (StringUtils.equals(search.getType(),"NOTE")){
                    search.setNoteId(Long.valueOf(hit.getDocument().get("id").toString()));
                }

                search.setFileLocation(search.getFolderId() + "/" + search.getFileName());

                Set<String> snippets = new HashSet<>();
                for (var highlight : hit.getHighlights()) {
                    if (highlight.getSnippet() == null) {
                        for (var snippet : highlight.getSnippets()) {
                            snippets.add(snippet);
                        }
                    } else {
                        snippets.add(highlight.getSnippet());
                    }
                }
                search.setSnippets(snippets);
                Object tagsObj = hit.getDocument().get("tags");
                if (tagsObj != null) {
                    var tags = new HashSet<String>((List) hit.getDocument().get("tags"));
                    search.setTags(tags);
                }else{
                    search.setTags(Set.of()); 
                }
                allSearchData.add(search);
            }

            respDTO.setCountPerPage(countPerPage);
            respDTO.setCount(searchDocumentResult.getFound());
            respDTO.setPage(searchDocumentResult.getPage());
            respDTO.setTotalPage((int) Math.ceil((double) searchDocumentResult.getFound() / (double) countPerPage));
            respDTO.setSearchResults(allSearchData);
            respDTO.setStatus("SUCCESS");
        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
            respDTO.setStatus("ERROR");
        }

        return respDTO;

    }

    private StringBuilder buildSearchFilter(
            Boolean searchLatest,
            Set<String> currentAuthorities,
            Boolean isActiveOnly,
            Set<String> folderIds,
            Set<String> docIDs, 
            Boolean isAdmin) {
        StringBuilder filterBuilder = new StringBuilder();
        if (searchLatest == true) {
            filterBuilder.append("latest:true && ");
        }
        if (!isAdmin){
            filterBuilder.append("readers:[" + String.join(", ", currentAuthorities) + "] && ");
        }else{
            filterBuilder.append("type_:'FOLDER' && ");
        }
        //filterBuilder.append("readers:[] ");
        if (isActiveOnly) {
            filterBuilder.append("active:true ");
        }
        if (!folderIds.isEmpty() || !docIDs.isEmpty()) {
            filterBuilder.append("&& (");
        }
        if (!folderIds.isEmpty()) {
            filterBuilder.append("pathToRoot:[" + String.join(", ", folderIds) + "] ");
            if (!docIDs.isEmpty()) {
                filterBuilder.append("|| ");
            }
        }

        if (!docIDs.isEmpty()) {
            filterBuilder.append("docId:[" + String.join(", ", docIDs) + "]");
        }
        if (!folderIds.isEmpty() || !docIDs.isEmpty()) {
            filterBuilder.append(")");
        }
        System.out.println(filterBuilder);
        return filterBuilder;
    }

    public void updateTags(Set<String> tags, DocumentData doc) throws Exception {
        //if (isActive(doc)) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tags", tags);
        UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
        updateDocumentsParameters.filterBy("docId:" + doc.getId());

        searchEngine.get4Docs()
                .update(updateData, updateDocumentsParameters);
        //}
    }

    public void deleteDoc(DocumentData doc) {

        try {
            //if (isActive(doc)) {
            Long docId = doc.getId();

            DeleteDocumentsParameters deleteDocumentsParameters = new DeleteDocumentsParameters();
            deleteDocumentsParameters.filterBy("docId:=" + docId);

            searchEngine.get4Docs().delete(deleteDocumentsParameters);
            //}
        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    public void deleteFolder(Folder folder) {

        try {
            DeleteDocumentsParameters deleteDocumentsParameters = new DeleteDocumentsParameters();
            deleteDocumentsParameters.filterBy("folderId:=" + folder.getId());

            searchEngine.get4Docs().delete(deleteDocumentsParameters);
            
        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }
    
    public void updateFolder(Folder folder, String newFolderName, Boolean newActiveState, Boolean oldActiveState) {

        try { 
            //final Folder folder = folderRepo.getFolderWithCompleteAccessMap(folder_);
            Set<String> readers = folder.getAccessMap().stream().map(fa -> fa.getAuthority()).collect(Collectors.toSet());

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("folderName", newFolderName);
            updateData.put("readers", readers);

            UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
            updateDocumentsParameters.filterBy("folderId:" + folder.getId());

            searchEngine.get4Docs()
                    .update(updateData, updateDocumentsParameters);

            if (!newActiveState.equals(oldActiveState)) { //if activity change...
                if (folder.isRoot()) {
                    List<DocumentData> docs = docRepo.findAll(whereRootEquals(folder));

                    for (var doc : docs) {
                        Map<String, Object> updateActivity = new HashMap<>();
                        updateActivity.put("active", newActiveState);
                        UpdateDocumentsParameters up = new UpdateDocumentsParameters();
                        up.filterBy("docId:" + doc.getId());
                        searchEngine.get4Docs()
                                .update(updateActivity, up);

                    }
                }
            }
            //}
        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    public void updateDocReadersList(Folder folder_) {
        try {
            final Folder folder = folderRepo.getFolderWithCompleteAccessMap(folder_);
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("readers",folder.getAccessMap().stream().map(fa -> fa.getAuthority()).collect(Collectors.toSet()));
            UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
            updateDocumentsParameters.filterBy("folderId:" + folder.getId());

            searchEngine.get4Docs()
                    .update(updateData, updateDocumentsParameters);
            
            

        } catch (Exception ex) {
             LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }
    
     public void emptyDocReadersList(Folder folder_) {
        try {
            final Folder folder = folderRepo.getFolderWithCompleteAccessMap(folder_);
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("readers",new String[]{} );//folder.getAccessMap().stream().map(fa -> fa.getAuthority()).collect(Collectors.toSet()));
            UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
            updateDocumentsParameters.filterBy("folderId:" + folder.getId());

            searchEngine.get4Docs()
                    .update(updateData, updateDocumentsParameters);
            
            

        } catch (Exception ex) {
             LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    public void updateNoteMessage(Note noteWithNewMessage) {

        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("content", noteWithNewMessage.getMessage());
            //UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
            //updateDocumentsParameters.filterBy("id:" + noteWithNewMessage.getId());
            Map<String, Object> result = searchEngine.get4Docs(noteWithNewMessage.getId().toString())
                    .update(updateData);

            //System.out.println(result);

        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    public void deleteNote(Note note) {

        try {

            Map<String, Object> result = searchEngine.get4Docs(note.getId().toString())
                    .delete();

            //System.out.println(result);

        } catch (Exception ex) {
             LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }
    
    
    
    @Transactional
    public void indexFolder(Folder folder) {
        

        //build readers
        Set<String> readers = folderRepo.getReaders(folder.getId());

        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("id", folder.getId().toString());
            updateData.put("type_", "FOLDER");
            updateData.put("docId", folder.getId());

            //--tags
            updateData.put("readers", readers);
            //--workflowid
            updateData.put("folderName", folder.getName());
            updateData.put("folderId", folder.getId());
            updateData.put("rootFolderId", folder.getRoot().getId());
            updateData.put("page", 1);
            updateData.put("fileName", "");
            //--size
            updateData.put("latest", true);
            updateData.put("version", 1);
            updateData.put("active", true);
            updateData.put("pathToRoot", folder.getPathToRootInclusive());


            updateData.put("content","");
            Map<String, Object> result = searchEngine.get4Docs().upsert(updateData);

            //System.out.println(result);

        } catch (Exception ex) {
             LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    

    @Transactional
    public void indexNote(Note note) {
        DocumentData doc = note
                .getDocument();

        //build readers
        Set<String> readers = folderRepo.getReaders(doc.getParent().getId());

        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("id", note.getId().toString());
            updateData.put("type_", "NOTE");
            updateData.put("docId", doc.getId());

            //--tags
            updateData.put("readers", readers);
            //--workflowid
            updateData.put("folderName", doc.getParent().getName());
            updateData.put("folderId", doc.getParent().getId());
            updateData.put("rootFolderId", doc.getRoot().getId());
            updateData.put("page", 1);
            updateData.put("fileName", doc.getName());
            //--size
            updateData.put("latest", true);
            updateData.put("version", 1);
            updateData.put("active", true);
            updateData.put("pathToRoot", doc.getParent().getPathToRootInclusive());

            updateData.put("writerUserName", note.getWriterUserName());
            updateData.put("writerFullName", note.getWriterFullName());
            updateData.put("writtenDate", note.getWrittenDate().toEpochSecond(ZoneOffset.UTC));

            updateData.put("content", note.getMessage());
            Map<String, Object> result = searchEngine.get4Docs().upsert(updateData);

            //System.out.println(result);

        } catch (Exception ex) {
             LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
        }
    }

    public String indexDocument(
            String workflowId,
            Long docId,
            String folderName,
            String fileName,
            Long folderId,
            Long rootFolderId,
            Set<String> tags,
            Set<FolderAccess> accessMap,
            Integer size,
            Integer version,
            Boolean active,
            List<Long> pathToRoot,
            byte[] content) {

        PDDocument pddoc = null;

        try {
            pddoc = Loader.loadPDF(content);

            PDFTextStripper reader = new PDFTextStripper();
            List<Map<String, Object>> document = new ArrayList<>();
            ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
            String result = null;

            //build readers
            Set<String> readers = folderRepo.getReaders(Set.of(folderId));

            //if we are updating, put all earlier docs as latest=false
            if (version != 1) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("latest", false);
                UpdateDocumentsParameters updateDocumentsParameters = new UpdateDocumentsParameters();
                updateDocumentsParameters.filterBy("docId:" + docId);

                searchEngine.get4Docs()
                        .update(updateData, updateDocumentsParameters);
            }

            for (int i = 1; i <= pddoc.getNumberOfPages(); i++) {
                //System.out.println("Page:" + i);
//                
//                {"name": "docId", "type": "int64" },
//           {"name": "tags", "type": "string[]" },
//           {"name": "workflowId", "type": "string", "optional": true},
//           {"name": "folderName", "type": "string" },
//           {"name": "folderId", "type": "int64" },
//           {"name": "page", "type": "int32" },
//           {"name": "fileName", "type": "string" },
//           {"name": "size", "type": "int32" },
//           {"name": "content", "type": "string" }

                reader.setStartPage(i);
                reader.setEndPage(i);
                Map<String, Object> map = new HashMap<>();
                map.put("id", docId + "_" + version + "_" + i);
                map.put("type_", "DOCUMENT");
                map.put("docId", docId);
                if (tags != null) {
                    map.put("tags", tags);
                }
                map.put("readers", readers);
                map.put("workflowId", workflowId);
                map.put("folderName", folderName);
                map.put("folderId", folderId);
                map.put("rootFolderId", rootFolderId);
                map.put("page", i);
                map.put("fileName", fileName);
                map.put("size", size);
                map.put("latest", true);
                map.put("version", version);
                map.put("active", active);
                map.put("pathToRoot", pathToRoot);

                map.put("content", reader.getText(pddoc));

                document.add(map);

                if (i % 10 == 0) { //every 10 pages
                    //import in bulk
                    result = searchEngine.get4Docs().import_(document, queryParameters);
                    document = new ArrayList<>();
                } else if (i == pddoc.getNumberOfPages()) { //last page
                    //import whatever is left
                    result = searchEngine.get4Docs().import_(document, queryParameters);

                }

            }
            if (result.contains("\"success\": false")) {
                LogFactory.getLog(DocumentSearchService.class.getName()).error("Error");
                return "ERROR";
            } else {
                return "SUCCESS";

            }
        } catch (Exception ex) {
            LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
            return "ERROR";
        } finally {
            if (pddoc != null) {
                try {
                    pddoc.close();

                } catch (IOException ex) {
                     LogFactory.getLog(DocumentSearchService.class.getName()).fatal("Error", ex);
                }
            }
        }

    }

//    @Transactional
//    public Boolean isActive(Item item) {
//        if (item == null) {
//            return false;
//        }
//        if (item instanceof Folder folder) {
//
//            if (folder.isRoot()) {
//                return folder.getActive();
//            } else {
//                return folderRepo.findById(folder.getId()).map(f -> f.getRoot().getActive()).orElse(false);
//            }
//        } else {
//            DocumentData doc = (DocumentData) item;
//            Folder folder = doc.getParent();
//            if (folder.isRoot()) {
//                return folder.getActive();
//            } else {
//                return folderRepo.findById(folder.getId()).map(f -> f.getRoot().getActive()).orElse(false);
//            }
//        }
//    }
}
