/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.dto.basic.DocumentDataDTO;
import com.azrul.kelichap.dto.basic.FolderAccessDTO;
import com.azrul.kelichap.dto.basic.FolderDTO;
import com.azrul.kelichap.dto.basic.ItemProxyDTO;
import com.azrul.kelichap.dto.basic.NoteDTO;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.representations.idm.UserRepresentation;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 *
 * @author azrul
 */
@Service
public class MapperService {

    private final ModelMapper folderMapper;
    private final ModelMapper docMapper;
    private final ModelMapper basicMapper;

    public MapperService(
            @Autowired @Qualifier("FolderMapper") ModelMapper folderMapper,
            @Autowired @Qualifier("DocMapper") ModelMapper docMapper,
            @Autowired @Qualifier("BasicMapper") ModelMapper basicMapper) {
        this.folderMapper = folderMapper;
        this.docMapper = docMapper;
        this.basicMapper = basicMapper;
    }

    public FolderDTO map(Folder folder) {
        //return modelMapper.map(folder,FolderDTO.class);
        return folderMapper.map(folder, FolderDTO.class);
    }

    public DocumentDataDTO map(DocumentData doc) {
        return docMapper.map(doc, DocumentDataDTO.class);
    }

    public NoteDTO map(Note note) {
        return basicMapper.map(note, NoteDTO.class);
    }

    public Note map(NoteDTO noteDTO) {
        return basicMapper.map(noteDTO, Note.class);
    }
    
    public User map(UserRepresentation userRep){
        return basicMapper.map(userRep, User.class);
    }
    
    public User map(OidcUser oidcUser){
        User user = new User();
        user.setEmail(oidcUser.getEmail());
        user.setFirstName(oidcUser.getGivenName());
        user.setLastName(oidcUser.getFamilyName());
        user.setUsername(oidcUser.getPreferredUsername());
        return user;
    }
    
    public Set<User> mapUsers(List<UserRepresentation> userReps){
        return userReps.stream()
                .map(ur -> basicMapper.map(ur, User.class))
                .collect(Collectors.toSet());
    }

    public Set<FolderAccessDTO> mapAccess(Set<FolderAccess> folderAccess) {
        return folderAccess.stream()
                .map(fa -> basicMapper.map(fa, FolderAccessDTO.class))
                .collect(Collectors.toSet());
    }

    public Set<FolderAccess> reverseMap(Set<FolderAccessDTO> folderAccess) {
        return folderAccess.stream()
                .map(fa -> basicMapper.map(fa, FolderAccess.class))
                .collect(Collectors.toSet());
    }

}
