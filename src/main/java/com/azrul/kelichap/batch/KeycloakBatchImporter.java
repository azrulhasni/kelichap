/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.batch;

import com.azrul.kelichap.service.SearchEngine;
import com.azrul.kelichap.service.UserToSearchEngineSyncService;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author azrul
 */
@Component
public class KeycloakBatchImporter {
    //kelichap.keycloak.typesense.usersync.immediateafterrun
    private final boolean  immediateAfterRun;
    private final boolean copyOnlyOnEmpty;
    private final SearchEngine searchEngine;
    private final UserToSearchEngineSyncService userToSearchEngineSyncService;
    
    public KeycloakBatchImporter(
            @Autowired UserToSearchEngineSyncService userToSearchEngineSyncService,
            @Autowired SearchEngine searchEngine,
            @Value("${kelichap.keycloak.typesense.usersync.immediateafterrun}") boolean immediateAfterRun, 
            @Value("${kelichap.keycloak.typesense.usersync.immediateruncopyoonlyonempty}") boolean copyOnlyOnEmpty)
    {
        this.immediateAfterRun = immediateAfterRun;
        this.searchEngine=searchEngine;
        this.userToSearchEngineSyncService = userToSearchEngineSyncService;
        this.copyOnlyOnEmpty=copyOnlyOnEmpty;
    }
    
    @PostConstruct
    public void runImmediate(){
        if (immediateAfterRun){
            if (copyOnlyOnEmpty){
                if (searchEngine.getUserCollectionInfo().getNumDocuments()==0){
                    userToSearchEngineSyncService.copyFromKeycloakToUserAlias();
                }
            }else{
                userToSearchEngineSyncService.copyFromKeycloakToUserAlias();
            }
        }
    }
    
    @Scheduled(cron = "${kelichap.keycloak.typesense.usersync.cron}")
    @SchedulerLock(name = "UserSyncTask", 
      lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    public void scheduled(){
        userToSearchEngineSyncService.copyFromKeycloakToUserAlias();
    }
}
