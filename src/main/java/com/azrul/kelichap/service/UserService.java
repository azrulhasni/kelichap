/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.api.Document;
import org.typesense.api.Documents;
import org.typesense.model.CollectionSchema;
import org.typesense.model.ImportDocumentsParameters;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.model.SearchResultHit;

/**
 *
 * @author azrul
 */
@Service
public class UserService {
    private final MapperService mapperService;
    private final ObjectMapper objectToKeyValueMaper = new ObjectMapper();
    private final SearchEngine searchEngine;
    private final Integer allUsersMaxCount;
    

    public UserService(
            @Autowired MapperService mapperService,
            @Autowired SearchEngine searchEngine,
            @Value("${typesense.users.allUsersMaxCount}") Integer allUsersMaxCount
           
    ) {
        this.mapperService = mapperService;
        this.allUsersMaxCount=allUsersMaxCount;
        this.searchEngine=searchEngine;
    }
    
    public void reloadUsers(List<UserRepresentation> userReps){
        try {
            objectToKeyValueMaper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            Set<User> users = mapperService.mapUsers(userReps);
            List<Map<String,Object>> usersAsMaps = new ArrayList<>();
            for (User u:users){
                Map<String, Object> map = objectToKeyValueMaper.convertValue(u, new TypeReference<Map<String, Object>>() {});
                map.put("id",u.getUsername());
                usersAsMaps.add(map);
            }
           
            
            ImportDocumentsParameters importDocumentsParameters = new ImportDocumentsParameters();
            importDocumentsParameters.action("create");
            searchEngine.get4Users().import_(usersAsMaps, importDocumentsParameters);
        } catch (Exception ex) {
            Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    
    public User getUser(String username){
        try {
            objectToKeyValueMaper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            var userMap = searchEngine.get4Users(username).retrieve();
            return objectToKeyValueMaper.convertValue(userMap, User.class);
        } catch (Exception ex) {
            Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String getFullName(String username){
        User user = getUser(username);
        return user.getFirstName()+" "+user.getLastName();
                
    }
    
    public Set<User> getUsers(Set<String> usernames){
        try {
            objectToKeyValueMaper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            Set<User> users = new HashSet<>();
            String usernameFilter = "id:[" + String.join(", ",usernames) +  "]";
            SearchParameters searchParameters = new SearchParameters()
                                        .q("*")
                                        .filterBy(usernameFilter);
            
            SearchResult searchResult = searchEngine.get4Users().search(searchParameters);
            for (SearchResultHit hit:searchResult.getHits()){
                users.add(objectToKeyValueMaper.convertValue(hit.getDocument(), User.class));
            }
            return users;
        } catch (Exception ex) {
            Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public Set<User> searchUsers(String queryString, Set<String> filteredOutUsernames){
        try {
            objectToKeyValueMaper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            Set<User> users = new HashSet<>();
            SearchParameters searchParameters = new SearchParameters()
                                        .q(queryString)
                                        .filterBy("id:!=["+ String.join(", ",filteredOutUsernames)+"]")
                                        .queryBy("username,firstName,lastName,email")
                                        .sortBy("username:asc")
                                        .limit(allUsersMaxCount);
            
            SearchResult searchResult = searchEngine.get4Users().search(searchParameters);
            for (SearchResultHit hit:searchResult.getHits()){
                users.add(objectToKeyValueMaper.convertValue(hit.getDocument(), User.class));
            }
            return users;
        } catch (Exception ex) {
            Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    

}
