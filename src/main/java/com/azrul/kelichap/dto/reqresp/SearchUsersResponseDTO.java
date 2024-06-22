/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.domain.User;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class SearchUsersResponseDTO {
    private Set<User> searchResults = new HashSet<>();

    /**
     * @return the searchResults
     */
    public Set<User> getSearchResults() {
        return searchResults;
    }

    /**
     * @param searchResults the searchResults to set
     */
    public void setSearchResults(Set<User> searchResults) {
        this.searchResults = searchResults;
    }
}
