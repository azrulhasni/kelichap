/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.resources.Node;

/**
 *
 * @author azrul
 */
@org.springframework.context.annotation.Configuration
public class TypesenseConfig {

    @Value("${typesense.protocol}")
    private String[] protocol;
    @Value("${typesense.host}")
    private String[] host;
    @Value("${typesense.port}")
    private String[] port;
    @Value("${typesense.apikey}")
    private String apiKey;
    

    @Bean
    public Client getClient() {
        List<Node> nodes = new ArrayList<>();
        for (int i=0;i<host.length;i++){
            nodes.add(
                    new Node(
                            protocol[i], // For Typesense Cloud use https
                            host[i], // For Typesense Cloud use xxx.a1.typesense.net
                            port[i] // For Typesense Cloud use 443
                    )
            );
        }

        Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2000), apiKey);

        return new Client(configuration);
    }
}
