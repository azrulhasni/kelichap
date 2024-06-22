/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author azrul
 */
@Configuration
@ConfigurationProperties(prefix = "jwt.auth.api.converter")
public class JwtAuthForAPIConverterProperties {

    private String resourceId;
    private String principalAttribute;

    /**
     * @return the resourceId
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * @param resourceId the resourceId to set
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * @return the principalAttribute
     */
    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    /**
     * @param principalAttribute the principalAttribute to set
     */
    public void setPrincipalAttribute(String principalAttribute) {
        this.principalAttribute = principalAttribute;
    }
}