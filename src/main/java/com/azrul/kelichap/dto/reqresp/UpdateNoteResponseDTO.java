/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.reqresp;

import com.azrul.kelichap.dto.basic.NoteDTO;

/**
 *
 * @author azrul
 */
public class UpdateNoteResponseDTO {
    private NoteDTO note;

    /**
     * @return the noteDTO
     */
    public NoteDTO getNote() {
        return note;
    }

    /**
     * @param noteDTO the noteDTO to set
     */
    public void setNote(NoteDTO note) {
        this.note = note;
    }
}
