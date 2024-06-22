/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.controller;

import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.dto.reqresp.AddNoteRequestDTO;
import com.azrul.kelichap.dto.reqresp.AddNoteResponseDTO;
import com.azrul.kelichap.dto.reqresp.UpdateNoteRequestDTO;
import com.azrul.kelichap.dto.reqresp.UpdateNoteResponseDTO;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.MapperService;
import com.azrul.kelichap.service.NoteService;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author azrul
 */
@RestController
public class NoteController {

    private final NoteService noteService;
    private final DocumentService docService;
    private final MapperService mapperService;

    public NoteController(
            @Autowired NoteService noteService,
            @Autowired MapperService mapperService,
            @Autowired DocumentService docService
    ) {
        this.noteService = noteService;
        this.mapperService = mapperService;
        this.docService = docService;
    }

    @PutMapping(path = "document/note/{id}")
    public ResponseEntity<UpdateNoteResponseDTO> updateNote(
            final @PathVariable(required = true, name = "id") Long id,
            final @RequestBody UpdateNoteRequestDTO reqDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            Note rnote = noteService.updateNoteMessage(id, reqDTO.getMessage(), token.getName());
            UpdateNoteResponseDTO respDTO = new UpdateNoteResponseDTO();
            respDTO.setNote(mapperService.map(rnote));
            return new ResponseEntity<>(respDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PostMapping(path = "document/{docId}/note")
    public ResponseEntity<AddNoteResponseDTO> addNote(
            final @PathVariable(required = true, name = "docId") Long docId,
            final @RequestBody AddNoteRequestDTO reqDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return docService.getDocById(docId, Set.of(token.getName()), true).map(doc -> {
                AddNoteResponseDTO respDTO = new AddNoteResponseDTO();
                Note note = new Note();
                note.setWriterUserName(token.getName());
                note.setWrittenDate(LocalDateTime.now());
                note.setMessage(reqDTO.getMessage());
                Note rnote = noteService.addNote(note, doc);
                respDTO.setNote(mapperService.map(rnote));
                return new ResponseEntity<>(respDTO, HttpStatus.OK);
            }).orElse(new ResponseEntity<>(HttpStatus.FORBIDDEN));
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    
    @DeleteMapping(path = "document/note/{id}")
    public ResponseEntity deleteNote(final @PathVariable(required = true, name = "id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            var response = noteService.getNote(id).map(note->{
                noteService.deleteNote(note, token.getName());
                return new ResponseEntity<>(HttpStatus.OK);
            }).orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            return response;
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
}
