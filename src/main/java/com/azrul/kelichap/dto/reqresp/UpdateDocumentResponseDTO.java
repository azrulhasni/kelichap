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
public class UpdateDocumentResponseDTO {
    private DocumentDataDTO document;

    /**
     * @return the document
     */
    public DocumentDataDTO getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DocumentDataDTO document) {
        this.document = document;
    }
}
