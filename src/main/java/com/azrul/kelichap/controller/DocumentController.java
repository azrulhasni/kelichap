package com.azrul.kelichap.controller;

import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.dto.basic.DocumentDataDTO;
import com.azrul.kelichap.dto.basic.FolderDTO;
import com.azrul.kelichap.dto.reqresp.AddDocumentRequestDTO;
import com.azrul.kelichap.dto.reqresp.AddDocumentResponseDTO;
import com.azrul.kelichap.dto.reqresp.AddFolderRequestDTO;
import com.azrul.kelichap.dto.reqresp.AddFolderResponseDTO;
import com.azrul.kelichap.dto.reqresp.UpdateDocumentRequestDTO;
import com.azrul.kelichap.dto.reqresp.UpdateDocumentResponseDTO;
import com.azrul.kelichap.dto.reqresp.UpdateFolderRequestDTO;
import com.azrul.kelichap.dto.reqresp.UpdateFolderResponseDTO;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.MapperService;
import com.azrul.kelichap.service.DocumentSearchService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 *
 * @author azrul
 */
@RestController
public class DocumentController {

    private final DocumentService docService;
    private final MapperService mapperService;
    private static final Pattern ALLOWED_FOLDER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_:\\.\\-\\s]+$");
    private static final Pattern ALLOWED_TAGS_PATTERN = Pattern.compile("^[a-zA-Z0-9 _#]+$");


    public DocumentController(
            @Autowired DocumentService docService,
            @Autowired MapperService mapperService
    ) {
        this.docService = docService;
        this.mapperService = mapperService;
    }

    @GetMapping(value = "/original-file/{id}")
    public ResponseEntity<StreamingResponseBody> downloadOriFile(
            @PathVariable Long id,
            final HttpServletResponse response
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            var stream = docService.getDocById(id, Set.of(token.getName()), true).map(document -> {
                StreamingResponseBody srb = out -> {
                    response.setContentType(document.getOriginalMimeType());
                    response.setHeader("Content-Disposition", "attachment;filename=\"" + document.getOriginalName() + "\"");
                    docService.downloadDoc(document.getFileLocation() + "/original", out);

                };
                return srb;
            }).orElse(null);

            if (stream != null) {
                return new ResponseEntity(stream, HttpStatus.OK);
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @GetMapping(value = "/file/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable Long id,
            final HttpServletResponse response
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            var stream = docService.getDocById(id, Set.of(token.getName()), true).map(document -> {
                StreamingResponseBody srb = out -> {
                    response.setContentType(document.getMimeType());
                    response.setHeader("Content-Disposition", "attachment;filename=\"" + document.getName() + "\"");
                    docService.downloadDoc(document.getFileLocation(), out);

                };
                return srb;
            }).orElse(null);

            if (stream != null) {
                return new ResponseEntity(stream, HttpStatus.OK);
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @GetMapping(path = {"/folder/{id}", "/folder/"})
    public ResponseEntity<FolderDTO> getFolder(
            @PathVariable(required = false, name = "id") Long id,
            @RequestParam(required = false) Map<String, String> qparams //supports: workflowId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            if (id != null) {
                return docService.getFolderById(id, Set.of(token.getName())).map(folder -> {
                    FolderDTO folderDTO = mapperService.map(folder);
                    return new ResponseEntity<>(folderDTO, HttpStatus.OK);
                }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @GetMapping(path = "/document/{id}")
    public ResponseEntity<DocumentDataDTO> getDocument(
            final @PathVariable(required = true, name = "id") Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {

            return docService.getDocById(id, Set.of(token.getName()), true).map(doc -> {
                DocumentDataDTO docDTO = mapperService.map(doc);
                return new ResponseEntity<>(docDTO, HttpStatus.OK);
            }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PostMapping(path = "/folder/{parentFolderId}/folder")
    public ResponseEntity<AddFolderResponseDTO> addNonRootFolder(
            final @PathVariable(required = true, name = "parentFolderId") Long parentFolderId,
            final @RequestBody AddFolderRequestDTO reqDTO) throws Exception {
        Matcher matcher = ALLOWED_FOLDER_NAME_PATTERN.matcher(reqDTO.getFolderName());
        if (!matcher.matches()) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            //Parameters
            Set<FolderAccess> fa = reqDTO.getAccessMap();
            AddFolderResponseDTO respDTO = new AddFolderResponseDTO();
            return docService.getFolderById(parentFolderId, Set.of(token.getName())).map(parent -> {

                var folder = docService.createFolder(
                        reqDTO.getFolderName(),
                        parent,
                        fa,
                        reqDTO.getWorkflowId(),
                        Set.of(token.getName())
                );
                if (folder != null) {
                    FolderDTO folderDTO = mapperService.map(folder);
                    respDTO.setFolder(folderDTO);
                    return new ResponseEntity<>(respDTO, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(respDTO, HttpStatus.FORBIDDEN);
                }
            }).orElseGet(() -> {
                return new ResponseEntity<>(respDTO, HttpStatus.INTERNAL_SERVER_ERROR);
            });
        }
        return new ResponseEntity<>(new AddFolderResponseDTO(), HttpStatus.FORBIDDEN);
    }

    @PostMapping(path = "/folder")
    public ResponseEntity<AddFolderResponseDTO> addRootFolder(
            final @RequestBody AddFolderRequestDTO reqDTO) throws Exception {
        Matcher matcher = ALLOWED_FOLDER_NAME_PATTERN.matcher(reqDTO.getFolderName());
        if (!matcher.matches()) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            //Parameters
            Set<FolderAccess> fa = reqDTO.getAccessMap();
            AddFolderResponseDTO respDTO = new AddFolderResponseDTO();

            var folder = docService.createRootFolder(
                    reqDTO.getFolderName(),
                    fa,
                    reqDTO.getWorkflowId(),
                    Set.of(token.getName())
            );
            if (folder != null) {
                FolderDTO folderDTO = mapperService.map(folder);
                respDTO.setFolder(folderDTO);
                return new ResponseEntity<>(respDTO, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(respDTO, HttpStatus.FORBIDDEN);
            }

        }
        return new ResponseEntity<>(new AddFolderResponseDTO(), HttpStatus.FORBIDDEN);
    }

    @PutMapping(path = "/document/{id}")
    public ResponseEntity<UpdateDocumentResponseDTO> updateDocument(
            final @PathVariable(required = true, name = "id") Long id,
            final @RequestBody UpdateDocumentRequestDTO reqDTO) {
        for (String tag:reqDTO.getTags()){
            Matcher matcher = ALLOWED_TAGS_PATTERN.matcher(tag);
            if (!matcher.matches()) {
                return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            UpdateDocumentResponseDTO respDTO = new UpdateDocumentResponseDTO();
            return docService.updateDocTags(id, reqDTO.getTags(), Set.of(token.getName()), true).map(doc -> {
                DocumentDataDTO docDTO = mapperService.map(doc);
                respDTO.setDocument(docDTO);
                return new ResponseEntity<>(respDTO, HttpStatus.OK);
            }).orElseGet(() -> {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            });
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PutMapping(path = "/folder/{id}")
    public ResponseEntity<UpdateFolderResponseDTO> updateFolder(
            final @PathVariable(required = true, name = "id") Long id,
            final @RequestBody UpdateFolderRequestDTO reqDTO) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            UpdateFolderResponseDTO respDTO = new UpdateFolderResponseDTO();
            Set<String> currentAuths = Set.of(token.getName());

            return docService.getFolderById(id, currentAuths).map(folder -> {
                Set<FolderAccess> fa = reqDTO.getAccessMap() != null
                        ? mapperService.reverseMap(reqDTO.getAccessMap())
                        : folder.getAccessMap();

                Boolean active = reqDTO.getActive() != null
                        ? reqDTO.getActive()
                        : folder.getActive();

                String folderName = reqDTO.getFolderName() != null
                        ? reqDTO.getFolderName()
                        : folder.getName();
                
                Matcher matcher = ALLOWED_FOLDER_NAME_PATTERN.matcher(folderName);
                if (!matcher.matches()) {
                    return new ResponseEntity<UpdateFolderResponseDTO>(HttpStatus.PRECONDITION_FAILED);
                }

                return docService.updateFolder(id, folderName, active, fa, currentAuths, false).map(folderRes -> {
                    FolderDTO folderDTO = mapperService.map(folder);
                    respDTO.setFolder(folderDTO);
                    return new ResponseEntity<>(respDTO, HttpStatus.OK);
                }).orElseGet(() -> {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
            }).orElseGet(() -> {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            });
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }


    @PostMapping(path = "folder/{folderId}/document",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}) // consumes = {"*/*"}
    public ResponseEntity<AddDocumentResponseDTO> addDocument(
            final @PathVariable(required = true, name = "folderId") Long folderId,
            final AddDocumentRequestDTO reqDTO,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            final @RequestParam("file") MultipartFile file
    ) throws Exception {
        for (String tag:reqDTO.getTags()){
            Matcher matcher = ALLOWED_TAGS_PATTERN.matcher(tag);
            if (!matcher.matches()) {
                return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            //Parameters
            final String mimeType = file.getContentType();
            final Long size = file.getSize();
            Path p = Paths.get(file.getOriginalFilename());
            final String originalFileName = p.getFileName().toString();
            byte[] fileContent = file.getBytes();
            final Set<FolderAccess> folderAccess = reqDTO.getFolderAccess() == null ? Set.of() : mapperService.reverseMap(reqDTO.getFolderAccess());
            //return new ResponseEntity<>( HttpStatus.OK);
            var opDoc = docService.addDocument(
                    reqDTO.getWorkflowId(),
                    token.getName(),
                    reqDTO.getTags(),
                    folderId,//reqDTO.getFolderId(),
                    originalFileName,
                    mimeType,
                    size,
                    fileContent,
                    reqDTO.getFolderName(),
                    folderAccess
            );
            AddDocumentResponseDTO respDTO = new AddDocumentResponseDTO();
            return opDoc.map(doc -> {
                DocumentDataDTO docDTO = mapperService.map(doc);
                respDTO.setDocument(docDTO);
                return new ResponseEntity<>(respDTO, HttpStatus.OK);
            }).orElseGet(() -> {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            });
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    
    @DeleteMapping(path = "/folder/{id}")
    public ResponseEntity deleteFolder(Long folderid){
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            Set<String> currentAuths = Set.of(token.getName());
            return docService.getFolderById(folderid, currentAuths).map(folder->{
                docService.deleteFolder(folder, currentAuths, Boolean.TRUE);
                return new ResponseEntity(HttpStatus.OK);
            }).orElseGet(()->{
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            });
            
        }
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }
    
    @DeleteMapping(path = "/document/{id}")
    public ResponseEntity deleteDocument(Long docid){
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            Set<String> currentAuths = Set.of(token.getName());
            return docService.getDocById(docid, currentAuths, Boolean.TRUE).map(doc->{
                docService.deleteDoc(doc, currentAuths, Boolean.TRUE);
                return new ResponseEntity(HttpStatus.OK);
            }).orElseGet(()->{
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            });
            
        }
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

}
