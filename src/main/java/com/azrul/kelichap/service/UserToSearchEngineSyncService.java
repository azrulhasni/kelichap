/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import java.util.List;
import java.util.Optional;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author azrul
 */
@Service
public class UserToSearchEngineSyncService {

    private final Keycloak keycloak;
    private final UserService userService;
    private final SearchEngine searchEngine;
    private final String keycloakRealm;
    private final Integer syncBatchSize;
    private final String typesenseUsersAlias;

    public UserToSearchEngineSyncService(
            @Autowired Keycloak keycloak,
            @Autowired UserService userService,
            @Autowired SearchEngine searchEngine,
            @Value("${kelichap.keycloak.realm}") String keycloakRealm,
            @Value("${typesense.users.alias}") String typesenseUsersAlias,
            @Value("${kelichap.keycloak.typesense.usersync.batchsize}") Integer syncBatchSize
    ) {
        this.keycloak = keycloak;
        this.userService = userService;
        this.keycloakRealm = keycloakRealm;
        this.syncBatchSize = syncBatchSize;
        this.searchEngine = searchEngine;
        this.typesenseUsersAlias = typesenseUsersAlias;
    }

    public void copyFromKeycloakToUserAlias() {
        Optional<String> newCollectioName = searchEngine.cloneSchemaAndAttachToAlias(typesenseUsersAlias);
        newCollectioName.ifPresent(collectionName -> {
            int p = 0;
            while (true) {
                List<UserRepresentation> pagedUsers = keycloak.realm(keycloakRealm).users().list(p * syncBatchSize, syncBatchSize);
                userService.reloadUsers(pagedUsers);
                p++;
                if (pagedUsers.size() < syncBatchSize) {
                    break;
                }
            }
        });
        
        
//        int allUsersCount = keycloak.realm(keycloakRealm).users().count();
//        Integer pages = (int)Math.ceil((double)allUsersCount/syncBatchSize);
//        
//        for (int p=0;p<pages;p++){
//            List<UserRepresentation> pagedUsers = keycloak.realm(keycloakRealm).users().list(p*syncBatchSize,syncBatchSize-1);
//            userService.reloadUsers(pagedUsers);
//        }

//            List<UserRepresentation> allUsers = keycloak
//                    .realm(keycloakRealm)
//                    .users()
//                    .list();
//                    .stream()
//                    .filter(u -> !this.keycloakUsername.equals(u.getUsername())
//                    ) // filter system user
//                    .collect(Collectors.toMap(UserRepresentation::getUsername, u -> mapperService.map(u)));
    }

}
