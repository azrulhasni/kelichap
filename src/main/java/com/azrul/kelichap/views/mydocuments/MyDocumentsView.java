package com.azrul.kelichap.views.mydocuments;

import com.azrul.kelichap.autocomplete.Autocomplete;
import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.domain.NotificationStatus;
import com.azrul.kelichap.domain.NotificationType;
import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.dto.basic.SearchDataDTO;
import com.azrul.kelichap.dto.reqresp.SearchDocumentsResponseDTO;
import com.azrul.kelichap.repository.AuditRepository;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.MapperService;
import com.azrul.kelichap.service.NoteService;
import com.azrul.kelichap.service.NotificationService;
import com.azrul.kelichap.service.DocumentSearchService;
import com.azrul.kelichap.service.UserService;
import com.azrul.kelichap.views.MainLayout;
import com.azrul.kelichap.views.common.FolderPanel;
import com.azrul.kelichap.views.common.SearchPanel;
import com.azrul.kelichap.views.notification.NotificationPanel;
import com.vaadin.componentfactory.pdfviewer.PdfViewer;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoIcon;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.vaadin.olli.FileDownloadWrapper;

@PageTitle("My Documents")
@Route(value = "mydocuments", layout = MainLayout.class)
@RolesAllowed("KELICHAP_USER")
public class MyDocumentsView extends Div implements AfterNavigationObserver, HasUrlParameter<Long> {

    private final Keycloak keycloak;
    private final String keycloakRealm;
    private final String keycloakUsername;
    private final DocumentService docService;
    private final DocumentSearchService searchService;
    private final NoteService noteService;
    private final NotificationService notifService;
    private final MapperService mapperService;
    private final UserService userService;

    private final Pattern EXTRACT_USER = Pattern.compile("\\@\\[([^]]*)]");
    private final Pattern EXTRACT_USERNAME = Pattern.compile("\\(([^]]*)\\)");

    

    private final Integer DOCUMENT_NAME_MAX_LENGTH;
    private final Integer NOTES_COUNT_PER_PAGE;

    private final AuditRepository auditRepo;

    //UI Components
    private final TabSheet tabPanel = new TabSheet();
    private final FolderPanel folderPanel = new FolderPanel();
//    private final VerticalLayout mainPanel = new VerticalLayout();
//    private final VerticalLayout searchResultPanel = new VerticalLayout();

//    private final HorizontalLayout searchPanel = new HorizontalLayout();
//    private final ComboBox<String> ddPages = new ComboBox<>();
//    private final TextField searchField = new TextField();
//    private final VerticalLayout txSearchScope = new VerticalLayout();
    private final Map<Long, Integer> notesPage = new HashMap<>();

    //Date format
    DatePicker.DatePickerI18n singleFormat = new DatePicker.DatePickerI18n();

    //Path preferred by workflow system
    private Long fromFolderId;

    //Current user
    private final DefaultOidcUser oidcUser;

    //All users
    private final Map<NotificationType, String> notifCardIntroText = Map.<NotificationType, String>of(
            NotificationType.MENTION, " mentined you in",
            NotificationType.FOLDER_ASSIGNMENT, " shared a folder",
            NotificationType.UPLOAD_NEW, " uploaded a new document",
            NotificationType.UPLOAD_NEW_VERSION, " uploaded a new version",
            NotificationType.ACCESS_REQUEST, " requested access"
    );

    private final Map<NotificationType, String> notifCardActionButtonText = Map.<NotificationType, String>of(
            NotificationType.MENTION, "Go there",
            NotificationType.FOLDER_ASSIGNMENT, "Go there",
            NotificationType.UPLOAD_NEW, "Go there",
            NotificationType.UPLOAD_NEW_VERSION, "Go there",
            NotificationType.ACCESS_REQUEST, "Open request"
    );

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Long parameter) {
        if (parameter == null) {
            fromFolderId = null;
        } else {
            fromFolderId = parameter;
            folderPanel.openFromFolderToRootFolder(fromFolderId, oidcUser, docService);
        }
    }

    public MyDocumentsView(
            @Autowired DocumentService docService,
            @Autowired DocumentSearchService searchService,
            @Autowired NoteService noteService,
            @Autowired NotificationService notifService,
            @Autowired Keycloak keycloak,
            @Autowired AuditRepository auditRepo,
            @Autowired MapperService mapperService,
            @Autowired UserService userService,
            @Value("${kelichap.keycloak.realm}") String keycloakRealm,
            @Value("${kelichap.keycloak.username}") String keycloakUsername,
            @Value("${kelichap.documentNameMaxLength:10}") Integer documentNameMaxLength,
            @Value("${kelichap.folderCountPerPage:7}") Integer folderCountPerPage,
            @Value("${kelichap.auditRecordCountPerPage:7}") Integer auditRecordCountPerPage,
            @Value("${kelichap.notesCountPerPage:5}") Integer notesCountPerPage,
            @Value("${kelichap.maxFileCountPerUpload:5}") Integer maxFileCountPerUpload,
            @Value("${kelichap.maxFileSize:12000000}") Integer maxFileSize
    ) {
        this.docService = docService;
        this.searchService = searchService;
        this.keycloak = keycloak;
        this.keycloakRealm = keycloakRealm;
        this.keycloakUsername = keycloakUsername;
        this.auditRepo = auditRepo;
        this.noteService = noteService;
        this.notifService = notifService;
        this.mapperService = mapperService;
        this.singleFormat.setDateFormat("dd-MMM-yyyy");
        this.userService = userService;

        this.DOCUMENT_NAME_MAX_LENGTH = documentNameMaxLength;
        this.NOTES_COUNT_PER_PAGE = notesCountPerPage;

        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken oauth2AuthToken) {

            //UI Components
            //Current user
            oidcUser = (DefaultOidcUser) oauth2AuthToken.getPrincipal();

            //Build Folder panel
            folderPanel.buildFolderTree(
                    oidcUser,
                    docService,
                    mapperService,
                    userService,
                    notifService,
                    auditRepo,
                    this::showDocumentViewer,
                    folderCountPerPage,
                    auditRecordCountPerPage,
                    maxFileCountPerUpload,
                    maxFileSize);

            //L&F
            addClassName("folders-view");
            setSizeFull();

            SplitLayout splitLayout = new SplitLayout();

            VerticalLayout primaryPanel = new VerticalLayout();
            Button btnNotif = buildNotifButton();
            primaryPanel.add(btnNotif, folderPanel);
            splitLayout.addToPrimary(primaryPanel);

            //
            SearchPanel mainPanel = new SearchPanel();
            mainPanel.buildSearchPanel(folderPanel, 
                    oidcUser, 
                    docService, 
                    searchService, 
                    this::showDocumentViewerAtPage, 
                    this::showDocumentViewerFromNote);

            tabPanel.add("Search", mainPanel);
            splitLayout.addToSecondary(tabPanel);
            splitLayout.setSplitterPosition(40);
            add(splitLayout);

        } else {
            oidcUser = null;
            folderPanel.resetPageNav();
        }

        //Finally, navigate to selected folder
    }

    

    public void showRequestApprovalDialog(Folder folder, FolderRight requestedFolderRight, String requestorUserName) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Approve access");
        User requestor = userService.getUser(requestorUserName);//allUsers.get(requestorUserName);

        VerticalLayout panel = new VerticalLayout();
        panel.add(new Text("Request from " + requestor.getFirstName() + " " + requestor.getLastName() + "(" + requestor.getUsername() + ") to access '" + folder.getName() + "'"));

        ComboBox<FolderRight> cbAccess = new ComboBox<>("Access to be granted");
        cbAccess.setWidthFull();
        List rightsOptions = Arrays.asList(FolderRight.values())
                .stream()
                .filter(fr -> fr != FolderRight.FOLDER_OWNER)
                .collect(Collectors.toList());

        cbAccess.setItems(rightsOptions);
        cbAccess.setValue(requestedFolderRight);
        panel.add(cbAccess);
        dialog.add(panel);

        Button btnApproveRequest = new Button("Approve request");
        btnApproveRequest.setId("btnApproveRequest");
        btnApproveRequest.addClickListener(c -> {
            FolderRight selectedRight = cbAccess.getValue();
            //if (allUsers.containsKey(requestorUserName)) {
            FolderAccess fa = new FolderAccess(requestorUserName, selectedRight);
            docService.addForlderAccessMap(folder, fa);
            //}
            dialog.close();
        });

        Button btnGoToFolder = new Button("Go to folder");
        btnGoToFolder.setId("btnGoToFolder");
        btnGoToFolder.addClickListener(c -> {
            folderPanel.openFromFolderToRootFolder(folder.getId(), oidcUser, docService);
            dialog.close();
        });
        dialog.getFooter().add(btnGoToFolder);

        dialog.getFooter().add(btnApproveRequest);
        Button btnCancel = new Button("Close", ed -> dialog.close());
        btnCancel.setId("btnCancel");
        dialog.getFooter().add(btnCancel);
        dialog.open();
    }

    public void showDocumentViewerFromNote(
            DocumentData doc,
            Long notifIdToBeHighlighted) {
        showDocumentViewer(doc, true, 1, notifIdToBeHighlighted);
    }

    public void showDocumentViewer(
            DocumentData doc,
            Boolean isLatestVersion
    ) {
        showDocumentViewer(doc, isLatestVersion, 1, null);
    }

//    private void showDocumentViewer(
//            DocumentData doc,
//            Boolean isLatestVersion,
//            Integer currentPage) {
//        showDocumentViewer(doc, isLatestVersion, currentPage, null);
//    }
    
    private void showDocumentViewerAtPage(
            DocumentData doc,
            Integer currentPage) {
        showDocumentViewer(doc, true, currentPage, null);
    }
    
    

    private void showDocumentViewer(
            DocumentData doc,
            Boolean isLatestVersion,
            Integer currentPage,
            Long notifIdToBeHighlighted) {

        PdfViewer pv = new PdfViewer();
        StreamResource pdfResource = new StreamResource(doc.getName(), (out, session) -> {
            docService.downloadDoc(doc.getFileLocation(), out);
        });
        StreamResource origResource = new StreamResource(doc.getOriginalName(), (out, session) -> {
            docService.downloadDoc(doc.getFileLocation() + "/original", out);
        });

        pv.setSrc(pdfResource);

        pv.setPage(currentPage);
        Div pdfViewer = new Div(pv);

        pdfViewer.setWidth("100%");
        HorizontalLayout btnPanel = new HorizontalLayout();
        Button btnDocProp = new Button("Properties",
                e -> folderPanel.showFolderOrDocUpdateDialog(doc, oidcUser, docService, userService, mapperService, notifService, auditRepo, this::showDocumentViewer)
        );
        btnDocProp.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnPanel.add(btnDocProp);

        Button btnDownloadOrig = new Button("Download original");
        btnDownloadOrig.addThemeVariants(ButtonVariant.LUMO_SMALL);
        FileDownloadWrapper btnwDownloadOrig = new FileDownloadWrapper(origResource);
        btnwDownloadOrig.wrapComponent(btnDownloadOrig);
        btnPanel.add(btnwDownloadOrig);

        Button btnDownloadPdf = new Button("Download pdf");
        btnDownloadPdf.addThemeVariants(ButtonVariant.LUMO_SMALL);
        FileDownloadWrapper btnwDownloadPdf = new FileDownloadWrapper(pdfResource);
        btnwDownloadPdf.wrapComponent(btnDownloadPdf);
        btnPanel.add(btnwDownloadPdf);

        btnPanel.setPadding(false);
        VerticalLayout docContent = new VerticalLayout();
        docContent.setPadding(false);
        docContent.setSpacing(false);
        docContent.add(btnPanel, pdfViewer);
        SplitLayout tabContent = new SplitLayout();
        tabContent.setSplitterPosition(60);
        var msgContent = buildNotesPanel(
                doc,
                notifIdToBeHighlighted,
                docService.isActive(doc.getParent())
        );
        tabContent.addToPrimary(docContent);
        tabContent.addToSecondary(msgContent);

        String name = null;
        if (doc.getName().length() <= DOCUMENT_NAME_MAX_LENGTH) {
            name = doc.getName();
        } else {
            name = doc.getName().substring(0, DOCUMENT_NAME_MAX_LENGTH - 1);
        }

        if (!isLatestVersion) {
            name = "[OLD v" + doc.getVersion() + "] " + name;
        }
        if (docService.isActive(doc.getParent()) == false) {
            name = "[INACTIVE] " + name;
        }
        Tab tab = tabPanel.add(name + "...", tabContent);
        if (!isLatestVersion) {
            tab.getStyle().set("color", "darkred");
        }
        tab.setTooltipText(doc.getName());
        Button close = new Button(VaadinIcon.CLOSE.create(), click -> tabPanel.remove(tab));
        tab.addComponentAsFirst(close);
        tabPanel.setSelectedTab(tab);
    }

    private VerticalLayout buildNotesPanel(
            DocumentData doc,
            Long noteToBeHighlighted,
            Boolean isActive
    ) {
        List<String> readers = docService.getReadersOfDocumentWithoutCurrentUser(
                doc,
                oidcUser.getPreferredUsername()
        );

        VerticalLayout msgContent = new VerticalLayout();
        VirtualList<Note> vlNotesList = new VirtualList();
        vlNotesList.setRenderer(new NoteRenderer(
                oidcUser,
                noteService,
                docService,
                readers,
                this::getNotesList,
                doc,
                noteToBeHighlighted,
                vlNotesList
        ));
        vlNotesList.getStyle().set("height", "340px");
        Autocomplete acNewChat = new Autocomplete("@", true);
        acNewChat.setWidthFull();

        acNewChat.addTokenListener(event -> {
            String text = event.getToken();
            if (text != null) {
                List<String> selectedUsers = readers.stream().filter(u -> {
                    return StringUtils.containsIgnoreCase(u, text.trim().replaceAll("[^\\p{L}\\p{Nd}]+", ""));
                }).collect(Collectors.toList());
                acNewChat.setOptions(selectedUsers);
            }
        });

        Button btnSubmit = new Button("Submit", e -> {
            Note note = new Note();
            note.setMessage(acNewChat.getValue());
            note.setWriterFullName(oidcUser.getGivenName() + " " + oidcUser.getFamilyName());
            note.setWriterUserName(oidcUser.getPreferredUsername());
            note.setWrittenDate(LocalDateTime.now());

            note = noteService.addNote(note, doc);

            acNewChat.setValue("");

            List<Note> msgs = getNotesList(doc, noteToBeHighlighted/*, isActiveOnly*/);
            vlNotesList.setItems(msgs);

            Map<String, com.azrul.kelichap.domain.Notification> notifs = new HashMap<>();
            Matcher fullNameMatcher = EXTRACT_USER.matcher(note.getMessage());
            while (fullNameMatcher.find()) {
                String toFullName = fullNameMatcher.group(1);
                Matcher userNameMatcher = EXTRACT_USERNAME.matcher(toFullName);
                if (userNameMatcher.find(1)) {
                    String toUserName = userNameMatcher.group(1);
                    com.azrul.kelichap.domain.Notification notif = new com.azrul.kelichap.domain.Notification();
                    notif.setDateTime(note.getCreationDate());
                    notif.setToFullName(toFullName);
                    notif.setToUserName(toUserName);
                    notif.setFromFullName(note.getWriterFullName());
                    notif.setFromUserName(note.getWriterUserName());
                    notif.setItem(doc);
                    notif.setMessage(note.getMessage());
                    notif.setMessageId(note.getId());

                    notif.setStatus(NotificationStatus.NEW);
                    notif.setType(NotificationType.MENTION);
                    notifs.put(toUserName, notif);
                }
            }
            notifService.saveAll(new ArrayList<>(notifs.values()));

        });
        btnSubmit.setId("btnSubmitNote");
        btnSubmit.setWidthFull();
        Button btnShowMore = new Button("Show more", e -> {
            moveNoteListToNextPage(doc, /*isActiveOnly,*/ noteToBeHighlighted, vlNotesList);
        });

        List<Note> msgs = getNotesList(doc, noteToBeHighlighted/*, isActiveOnly*/);
        vlNotesList.setItems(msgs);

        if (noteToBeHighlighted != null) { //if we are showing
            Integer pageOfNoteToBeHighlighted = this.noteService.pageOfNote(
                    noteToBeHighlighted,
                    "id",
                    false,
                    oidcUser.getPreferredUsername(),
                    NOTES_COUNT_PER_PAGE);

            moveNoteListToPage(doc, pageOfNoteToBeHighlighted, noteToBeHighlighted, /*isActiveOnly,*/ vlNotesList);
        }

        Footer secShowMore = new Footer(btnShowMore);

        msgContent.add(acNewChat, btnSubmit, vlNotesList, secShowMore);

        if (!isActive) {
            acNewChat.setEnabled(false);
            btnSubmit.setEnabled(false);
        }
        return msgContent;
    }

    private void moveNoteListToNextPage(DocumentData doc,/* Boolean isActiveOnly, */ Long noteToBeHighlighted, VirtualList list) {
        Integer page = notesPage.containsKey(doc.getId()) ? notesPage.get(doc.getId()) : 1;
        if (page < noteService.pageCountNotesOfDocument(
                doc,
                oidcUser.getPreferredUsername(),
                NOTES_COUNT_PER_PAGE)) { // if there are still more poages
            page++; //move to the next page

            moveNoteListToPage(doc, page, noteToBeHighlighted,/* isActiveOnly,*/ list);
        }
    }

    private void moveNoteListToPage(DocumentData doc, Integer page, Long noteToBeHighlighted, /*Boolean isActiveOnly,*/ VirtualList list) {
        notesPage.put(doc.getId(), page);
        List<Note> msgs = getNotesList(doc, noteToBeHighlighted/*, isActiveOnly*/);
        list.setItems(msgs);
        list.scrollToEnd();
    }

    private List<Note> getNotesList(
            DocumentData doc,
            Long noteToBeHighlighted
    //Boolean isActiveOnly
    ) {
        Integer page = notesPage.containsKey(doc.getId()) ? notesPage.get(doc.getId()) : 1;
        return noteService.getNotesOfDocument(doc,
                oidcUser.getPreferredUsername(),
                NOTES_COUNT_PER_PAGE,
                page).stream()
                .map(n -> {
                    if (n.getId().equals(noteToBeHighlighted)) {
                        n.setHighlighted(true);
                    } else {
                        n.setHighlighted(false);
                    }
                    return n;

                }).collect(Collectors.toList());
    }

    

    

    @Override
    public void afterNavigation(AfterNavigationEvent event) {

    }

    private Button buildNotifButton() {
        Integer unreadNotifCount = notifService.countForUserAndStatus(
                oidcUser.getPreferredUsername(),
                NotificationStatus.NEW
        );

        Span counter = buildNotifCounter(unreadNotifCount);

        String counterLabel = String.format("%d unread messages", unreadNotifCount);

        Icon bell = LumoIcon.BELL.create();
        Button btnNotif = new Button("");
        btnNotif.setId("btnNotif");

        ContextMenu notifMenu = new ContextMenu();

        notifMenu.add(new NotificationPanel(
                oidcUser.getPreferredUsername(),
                this.notifService,
                notifMenu,
                notifCardIntroText,
                notifCardActionButtonText,
                notif -> {

                    if (notif.getType().equals(NotificationType.MENTION)) {
                        folderPanel.openFromDocToRootFolder(notif.getItem().getId(), oidcUser, docService);
                        showDocumentViewerFromNote((DocumentData) notif.getItem(), notif.getMessageId());
                    } else if (notif.getType().equals(NotificationType.FOLDER_ASSIGNMENT)) {
                        folderPanel.openFromFolderToRootFolder((Folder) notif.getItem(), true, oidcUser, docService);
                    } else if (notif.getType().equals(NotificationType.UPLOAD_NEW)
                    || notif.getType().equals(NotificationType.UPLOAD_NEW_VERSION)) {
                        folderPanel.openFromDocToRootFolder(notif.getItem().getId(), oidcUser, docService);
                        showDocumentViewer((DocumentData) notif.getItem(), true);
                    } else if (notif.getType().equals(NotificationType.ACCESS_REQUEST)) {
                        showRequestApprovalDialog((Folder) notif.getItem(), notif.getRequestedRight(), notif.getFromUserName());
                    }
                }
        ));
        notifMenu.setTarget(btnNotif);
        notifMenu.setOpenOnClick(true);
        notifMenu.addDetachListener(e -> {
            //if (e. == false) {
            Integer unreadNotifCount2 = notifService.countForUserAndStatus(
                    oidcUser.getPreferredUsername(),
                    NotificationStatus.NEW
            );

            Span counter2 = buildNotifCounter(unreadNotifCount2);
            btnNotif.setSuffixComponent(counter2);
            String counterLabel2 = String.format("%d unread messages", unreadNotifCount2);
            btnNotif.setTooltipText(counterLabel2);
            //}
        });

        btnNotif.setTooltipText(counterLabel);
        btnNotif.setSuffixComponent(counter);
        btnNotif.setIcon(bell);
        btnNotif.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return btnNotif;
    }

    private Span buildNotifCounter(Integer unreadNotifCount) {
        Span counter = new Span(String.valueOf(unreadNotifCount));
        counter.getElement().getThemeList().add("badge pill small contrast");
        counter.getStyle().set("margin-inline-start", "var(--lumo-space-s)");
        if (unreadNotifCount > 0) {
            counter.getStyle().set("background-color", "crimson");
            counter.getStyle().set("color", "white");
        }
        return counter;
    }

}
