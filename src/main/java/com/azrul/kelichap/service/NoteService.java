/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.repository.DocumentDataRepository;
import com.azrul.kelichap.repository.NoteRepository;
import com.azrul.kelichap.repository.FolderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author azrul
 */
@Service
public class NoteService {

    @PersistenceContext
    private EntityManager entityManager;

    private final NoteRepository noteRepo;
    private final DocumentDataRepository docRepo;
    private final FolderRepository folderRepo;
    private final DocumentService docService;
    private final DocumentSearchService searchService;

    public NoteService(
            @Autowired NoteRepository noteRepo,
            @Autowired DocumentDataRepository docRepo,
            @Autowired FolderRepository folderRepo,
            @Autowired DocumentService docService,
            @Autowired DocumentSearchService searchService
    ) {
        this.noteRepo = noteRepo;
        this.docRepo = docRepo;
        this.folderRepo = folderRepo;
        this.docService = docService;
        this.searchService = searchService;
    }

    @Transactional
    public List<Note> getNotesOfDocument(DocumentData doc, String username) {
        return noteRepo.findAll(whereDocsIsReadableAndEquals(doc, username));
    }

    @Transactional
    public List<Note> getNotesOfDocument(DocumentData doc, String username, Integer countPerPage, Integer page) {
        Sort sort = Sort.by(Direction.DESC, "writtenDate");
        Pageable pageable = PageRequest.of(0, page * countPerPage, sort);
        return noteRepo.findAll(whereDocsIsReadableAndEquals(doc, username), pageable).toList();
    }

    @Transactional
    public Integer pageCountNotesOfDocument(DocumentData doc, String username, Integer countPerPage) {
        Long notesCount = noteRepo.count(whereDocsIsReadableAndEquals(doc, username));
        return (int) Math.ceil((double) notesCount / countPerPage);
    }

    @Transactional
    public Note addNote(Note note, DocumentData doc) {
        if (docService.isActive(doc)) {
            doc = docRepo.getReferenceById(doc.getId());
            doc.getNotes().add(note);
            note.setDocument(doc);
            Note rnote = noteRepo.save(note);
            docRepo.save(doc);
            noteRepo.flush();
            searchService.indexNote(note);
            return rnote;
        } else {
            return null;
        }
    }

    @Transactional
    public Optional<Note> getNote(Long id) {
        return noteRepo.findById(id);
    }

    @Transactional
    public Note updateNoteMessage(Long noteId, String message, String username) {

        return noteRepo.findById(noteId).map(note -> {
            if (!note.getWriterUserName().equals(username)) { //only the writer can change his note
                return null;
            }
            if (docService.isActive(note.getDocument())) {
                note.setMessage(message);
                Note rnote = noteRepo.save(note);
                searchService.updateNoteMessage(rnote);
                return rnote;
            } else {
                return null;
            }

        }).orElse(null);

    }

    @Transactional
    public void deleteNote(Note note, String username) {
        if (!note.getWriterUserName().equals(username)) { //only the writer can deleete his note
            return;
        }
        if (docService.isActive(note.getDocument())) {
            noteRepo.delete(note);
            searchService.deleteNote(note);
        }
    }

    @Transactional
    public Integer pageOfNote(Long noteId, String orderby, Boolean asc, String username, Integer countPerPage/*, Boolean isActiveOnly*/) {
        Integer noteRow = getRowNoteOfId(noteId, orderby, asc, username);
        if (noteRow == null) {
            return 1;
        }
        double page = (double) noteRow / (double) countPerPage;
        return (int) Math.ceil(page);
    }

    @Transactional
    public Integer getRowNoteOfId(Long noteId, String orderby, Boolean asc, String username) {

        return noteRepo.findById(noteId).map(note -> {
            Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(orderby);
            String order_by = m.replaceAll(match -> "_" + match.group().toLowerCase());
            DocumentData doc = note.getDocument();

            //query
            String sort = asc ? "ASC" : "DESC";
            Integer row = (Integer) entityManager.createNativeQuery(
                    "SELECT RowNumber FROM ("
                    + "SELECT NOTE.id,ROW_NUMBER() OVER (ORDER BY " + order_by + " " + sort + ") RowNumber "
                    + "FROM public.note NOTE "
                    + "WHERE NOTE.doc_id=" + doc.getId() + " "
                    + "AND '" + username + "' in ("
                    + "SELECT FAM.authority "
                    + "FROM public.folder_access_map FAM "
                    + "WHERE FAM.folder_id=" + doc.getParent().getId() + ")"
                    + ") A "
                    + "WHERE A.id=:id", Integer.class).setParameter("id", note.getId()).getSingleResult();
            return row;
        }).orElse(null);
    }

    static Specification<Note> whereDocsIsReadableAndEquals(DocumentData doc, String username) {

        return (note, cq, cb) -> {
            Set<Predicate> preds = new HashSet<>();
            preds.add(cb.isMember(new FolderAccess(username, FolderRight.CREATE_UPDATE_FOLDER), note.get("document").get("parent").get("accessMap")));
            preds.add(cb.isMember(new FolderAccess(username, FolderRight.FOLDER_OWNER), note.get("document").get("parent").get("accessMap")));
            preds.add(cb.isMember(new FolderAccess(username, FolderRight.READ_FOLDER), note.get("document").get("parent").get("accessMap")));

            return cb.and(
                    cb.or(preds.toArray(Predicate[]::new)),
                    cb.equal(note.get("document").get("root").get("active"), Boolean.TRUE),
                    cb.equal(note.get("document"), doc)
            );

        };
    }
}
