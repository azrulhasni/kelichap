/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.SearchDataDTO;
import com.azrul.kelichap.domain.DocumentData;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author azrul
 */
public class SearchDocumentsResponseDTO {

    private Integer totalPage;
    private Integer page;
    private Integer countPerPage;
    private Integer count;
    private Set<SearchDataDTO> searchResults = new HashSet<>();
    private String status;

    /**
     * @return the searchResults
     */
    public Set<SearchDataDTO> getSearchResults() {
        return searchResults;
    }

    /**
     * @param searchResults the searchResults to set
     */
    public void setSearchResults(Set<SearchDataDTO> searchResults) {
        this.searchResults = searchResults;
    }

    /**
     * @return the totalPage
     */
    public Integer getTotalPage() {
        return totalPage;
    }

    /**
     * @param totalPage the totalPage to set
     */
    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    /**
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * @return the countPerPage
     */
    public Integer getCountPerPage() {
        return countPerPage;
    }

    /**
     * @param countPerPage the countPerPage to set
     */
    public void setCountPerPage(Integer countPerPage) {
        this.countPerPage = countPerPage;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the count
     */
    public Integer getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(Integer count) {
        this.count = count;
    }
}
