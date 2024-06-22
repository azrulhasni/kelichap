/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.api.Document;
import org.typesense.api.Documents;
import org.typesense.model.CollectionAlias;
import org.typesense.model.CollectionAliasSchema;
import org.typesense.model.CollectionResponse;

/**
 *
 * @author azrul
 */
@Service
public class SearchEngine {

    private final Client typesenseClient;
    private final String typesenseDocsCollection;
    private final String typesenseUsersCollection;

    private final String[] protocol;
    private final String[] host;
    private final String[] port;
    private final String apiKey;
//    private final String typesenseDocsAlias;
//    private final String typesenseUsersAlias;

    public SearchEngine(
            @Autowired Client typesenseClient,
            @Value("${typesense.documents.collection}") String typesenseDocsCollection,
            @Value("${typesense.users.alias}") String typesenseUsersAlias,
            @Value("${typesense.protocol}") String[] protocol,
            @Value("${typesense.host}") String[] host,
            @Value("${typesense.port}") String[] port,
            @Value("${typesense.apikey}") String apiKey) {
        this.typesenseClient = typesenseClient;
        this.typesenseDocsCollection = typesenseDocsCollection;//.isBlank() ? typesenseDocsAlias : typesenseDocsCollection;
        this.typesenseUsersCollection = typesenseUsersAlias;//.isBlank() ? typesenseUsersAlias : typesenseUsersCollection;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.apiKey = apiKey;
    }

    public Documents get4Docs() {
        return typesenseClient.collections(typesenseDocsCollection).documents();
    }

    public Document get4Docs(String document) {
        return typesenseClient.collections(typesenseDocsCollection).documents(document);
    }

    public Documents get4Users() {

        return typesenseClient.collections(typesenseUsersCollection).documents();
    }

    public Document get4Users(String document) {
        return typesenseClient.collections(typesenseUsersCollection).documents(document);
    }

    public CollectionResponse getUserCollectionInfo() {
        try {
            return typesenseClient.collections(typesenseUsersCollection).retrieve();
        } catch (Exception ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public CollectionResponse getDocsCollectionInfo() {
        try {
            return typesenseClient.collections(typesenseDocsCollection).retrieve();
        } catch (Exception ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String cloneCollection(String origCollection, String prefix) throws IOException {

        //Do cloning
        //----------
        String newCollection = prefix + "_" + ZonedDateTime.now().toString();
        // Create JSON payload
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("name", newCollection);

        // Create URL object
        String param = URLEncoder.encode(origCollection, StandardCharsets.UTF_8.toString());
        URL url = new URL(protocol[0], host[0], Integer.valueOf(port[0]), "/collections?src_name=" + param);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request method to POST
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // Set headers
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-TYPESENSE-API-KEY", apiKey);

        // Write payload to output stream
        try ( OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Check response code
        int responseCode = conn.getResponseCode();

        conn.disconnect();

        //client.aliases().upsert("companies", collectionAlias);
        return newCollection;

    }

    public Optional<String> cloneSchemaAndAttachToAlias(String alias) {

        Optional<String> oCollectionName = getCollectionNameFromAlias(alias);
        return oCollectionName.flatMap(collectionName -> {
            try {
                String newCollectionName = cloneCollection(collectionName, alias);
                CollectionAliasSchema collectionAlias = new CollectionAliasSchema();
                collectionAlias.collectionName(newCollectionName);

                typesenseClient.aliases().upsert(alias, collectionAlias);
                String param = URLEncoder.encode(collectionName, StandardCharsets.UTF_8.toString());
                typesenseClient.collections(param).delete();
                return Optional.ofNullable(newCollectionName);
            } catch (IOException ex) {
                Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
            return Optional.empty();
        });
    }

    private Optional<String> getCollectionNameFromAlias(String alias) {
        try {
            //see if collection is an alias or not
            CollectionAlias collectionAlias = typesenseClient.aliases(alias).retrieve();
            return Optional.of(collectionAlias.getCollectionName());
        } catch (Exception ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.empty();
    }

}
