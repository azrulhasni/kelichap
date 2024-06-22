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
public class AddNoteResponseDTO {
    private NoteDTO note;

    /**
     * @return the note
     */
    public NoteDTO getNote() {
        return note;
    }

    /**
     * @param note the note to set
     */
    public void setNote(NoteDTO note) {
        this.note = note;
    }
}
