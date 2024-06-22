/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.DocumentDataDTO;

/**
 *
 * @author azrul
 */
public class AddDocumentResponseDTO {
    private DocumentDataDTO document;
    
    /**
     * @return the documentData
     */
    public DocumentDataDTO getDocument() {
        return document;
    }

    /**
     * @param documentData the documentData to set
     */
    public void setDocument(DocumentDataDTO documentData) {
        this.document = documentData;
    }
    
}
