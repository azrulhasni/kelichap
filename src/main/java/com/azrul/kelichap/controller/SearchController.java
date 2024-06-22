/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.controller;

import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.dto.reqresp.QuestionResponseDTO;
import com.azrul.kelichap.dto.reqresp.SearchDocumentsResponseDTO;
import com.azrul.kelichap.dto.reqresp.SearchUsersResponseDTO;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.DocumentSearchService;
import com.azrul.kelichap.service.UserService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 *
 * @author azrul
 */
@RestController
public class SearchController {
    
    private final ChatClient aiClient;
    private final DocumentService docService;
    private final DocumentSearchService searchService;
    private final UserService userService;

    public SearchController(
            @Autowired DocumentService docService,
            @Autowired DocumentSearchService searchService,
            @Autowired ChatClient aiClient,
            @Autowired UserService userService
    ) {
        this.docService = docService;
        this.searchService = searchService;
        this.aiClient=aiClient;  
        this.userService=userService;
    }
    
    
    
    
//    @GetMapping(path = "/ask")
//    @ResponseBody
//    public QuestionResponseDTO askQuestion(
//            @RequestParam("question") String question,
//            @RequestParam("authority") String authority) {
        
//        int MAX_PAGE = 10;
        
//        var respDTO = new QuestionResponseDTO();
//        try {
//           
//            //Find the folders that this authority has access to
//            var accessMap = Set.of(
//                    new FolderAccess(authority, FolderRight.CREATE_UPDATE_FOLDER),
//                    new FolderAccess(authority, FolderRight.READ_FOLDER)
//            );
//            List<Folder> folder = folderRepo.findAll(hasFolderAccess(accessMap));
//            Set<String> workflowIds = folder.stream().map(Folder::getWorkflowId).collect(Collectors.toSet());
//            SearchParameters searchParameters = new SearchParameters()
//                    .q(question)
//                    .queryBy("content,tags,embedding")
//                    .filterBy("workflowId:["+String.join(", ",workflowIds)+"]")
//                    .page(1)
//                    .perPage(MAX_PAGE);
//                    //.sortBy("ratings_count:desc")
//                    //.sortBy("page:desc")
//                    //.prefix("true,true,true");
//
//            SearchResult searchResult = typesenseClient.collections(typesenseCollection).documents().search(searchParameters);
//            
//            
//            StringBuilder sb = new StringBuilder();
//            for (var hit : searchResult.getHits()) {
//                sb.append(hit.getDocument().get("content")).append("\n");
//            }
//            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.qaSystemPromptResource);
//            var msg = systemPromptTemplate.createMessage(Map.of("documents", sb.toString()));
//            UserMessage userMessage = new UserMessage(msg.getContent());
//            Prompt prompt = new Prompt(List.of(userMessage));
//
//            Logger.getLogger(SearchController.class.getName()).log(Level.INFO, "Start prompting");
//            ChatResponse aiResponse = aiClient.generate(prompt);
//            Logger.getLogger(SearchController.class.getName()).log(Level.INFO, "Done prompting");
//            respDTO.setAnswer(aiResponse.getGeneration().getContent());
//            
//            
//            respDTO.setStatus("SUCCESS");
//        } catch (Exception ex) {
//            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
//            respDTO.setStatus("ERROR");
//        }
        
//        return respDTO;
//    }
    
    

    @GetMapping(path = "/searchdocuments")
    @ResponseBody
    public ResponseEntity<SearchDocumentsResponseDTO> searchDocuments(
            @RequestParam("query") String query,
            @RequestParam("sortField") String sortField,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("page") Integer page,
            @RequestParam("countPerPage") Integer countPerPage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            Set<Folder> folders = docService
                .getAllAccessibleFolders(Set.of(token.getName()), Boolean.FALSE)
                .stream()
                .collect(Collectors.toSet());
            SearchDocumentsResponseDTO  dto = searchService.search(
                        query, 
                        Set.of(),
                        folders,
                        page,
                        countPerPage,
                        sortField+":"+sortDir,
                        true,
                        Set.of(token.getName()),
                        false, //since we are searching eveerywhere, this doesn't really matter
                        false
            );

            return new ResponseEntity<>(dto,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        
//        searchService.doSearch(
//                query, 
//                folders,
//                page,
//                countPerPage,
//                sortField+":"+ascending,
//                Boolean.FALSE);
    }

    @GetMapping(path = "/searchusers")
    @ResponseBody
    public ResponseEntity<SearchUsersResponseDTO> searchUsers(
            @RequestParam("query") String query) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
             Set<User> users = userService.searchUsers(query,Set.of(token.getName()));
             SearchUsersResponseDTO dto = new SearchUsersResponseDTO();
             dto.setSearchResults(users);
            return new ResponseEntity<>(dto,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
}
