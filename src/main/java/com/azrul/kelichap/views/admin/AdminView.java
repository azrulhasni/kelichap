/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.admin;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.repository.AuditRepository;
import com.azrul.kelichap.service.DocumentSearchService;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.MapperService;
import com.azrul.kelichap.service.NoteService;
import com.azrul.kelichap.service.NotificationService;
import com.azrul.kelichap.service.UserService;
import com.azrul.kelichap.views.MainLayout;
import com.azrul.kelichap.views.common.FolderPanel;
import com.azrul.kelichap.views.common.SearchPanel;
import com.azrul.kelichap.views.common.UserField;
import com.azrul.kelichap.views.common.UserSearchPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Set;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@PageTitle("Administration")
@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("KELICHAP_ADMIN")
public class AdminView extends VerticalLayout {

    private final DocumentService docService;
    private final DocumentSearchService searchService;
    private final NotificationService notifService;
    private final MapperService mapperService;
    private final UserService userService;
    //Date format
    DatePicker.DatePickerI18n singleFormat = new DatePicker.DatePickerI18n();
    private final AuditRepository auditRepo;
    //  private final FolderPanel folderPanel = new FolderPanel();

    public AdminView(
            @Autowired DocumentService docService,
            @Autowired DocumentSearchService searchService,
            @Autowired NotificationService notifService,
            @Autowired AuditRepository auditRepo,
            @Autowired MapperService mapperService,
            @Autowired UserService userService,
            @Value("${kelichap.documentNameMaxLength:10}") Integer documentNameMaxLength,
            @Value("${kelichap.folderCountPerPage:7}") Integer folderCountPerPage,
            @Value("${kelichap.auditRecordCountPerPage:7}") Integer auditRecordCountPerPage,
            @Value("${kelichap.notesCountPerPage:5}") Integer notesCountPerPage,
            @Value("${kelichap.maxFileCountPerUpload:5}") Integer maxFileCountPerUpload,
            @Value("${kelichap.maxFileSize:12000000}") Integer maxFileSize
    ) {
        this.docService = docService;
        this.searchService = searchService;
        this.auditRepo = auditRepo;
        this.notifService = notifService;
        this.mapperService = mapperService;
        this.singleFormat.setDateFormat("dd-MMM-yyyy");
        this.userService = userService;
        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken oauth2AuthToken) {
            OidcUser oidcUser = (DefaultOidcUser) oauth2AuthToken.getPrincipal();

            this.add(buildReassignPanel(oidcUser));
            this.add(buildFolderPanel(oidcUser, folderCountPerPage, auditRecordCountPerPage, maxFileCountPerUpload, maxFileSize));

            // fromPanel.
        }
    }

    private VerticalLayout buildFolderPanel(
            OidcUser oidcUser,
            Integer folderCountPerPage,
            Integer auditRecordCountPerPage,
            Integer maxFileCountPerUpload,
            Integer maxFileSize) {
        //Current user
        FolderPanel folderPanel = new FolderPanel();
        //Build Folder panel
        folderPanel.buildFolderTree(
                oidcUser,
                docService,
                mapperService,
                userService,
                notifService,
                auditRepo,
                (t, u) -> {},
                folderCountPerPage,
                auditRecordCountPerPage,
                maxFileCountPerUpload,
                maxFileSize);
        SearchPanel searchPanel = new SearchPanel();
        searchPanel.buildSearchPanel(
                folderPanel,
                oidcUser,
                docService,
                searchService,
                (t, u) -> {},
                (t, u) -> {});
        SplitLayout splitPanel = new SplitLayout(folderPanel,searchPanel);
        splitPanel.setWidth("100%");
        splitPanel.setSplitterPosition(40);
        
        VerticalLayout mainPanel = new VerticalLayout();
        mainPanel.add(new H4("Folder management"));
        mainPanel.add(splitPanel);
        mainPanel.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BoxShadow.XSMALL,
                LumoUtility.BorderRadius.LARGE);
        return mainPanel;
    }

    private VerticalLayout buildReassignPanel(OidcUser oidcUser) {
        FormLayout form = new FormLayout();
        VerticalLayout fromPanel = new VerticalLayout();

        UserField fromUserField = new UserField(null, true);
        fromUserField.setWidth("100%");
        UserField toUserField = new UserField(null, true);
        toUserField.setWidth("100%");
        Button btnReassign = new Button("Re-assign user");
        btnReassign.addClickListener(e -> {

            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Re-asign user");
            dialog.setText("Are you sure you want to reassign all folders and documents from "
                    + fromUserField.getUser().getUserDispalyName()
                    + " to "
                    + toUserField.getUser().getUserDispalyName()
            );

            dialog.setCancelable(true);
            dialog.addCancelListener(event -> dialog.close());

            dialog.setConfirmText("Save");
            dialog.addConfirmListener(event -> {
                docService.reassignToNewUser(fromUserField.getUser().getUsername(), toUserField.getUser().getUsername());
                Notification.show("Re-assignment successful");
                toUserField.reset();
                fromUserField.reset();
                btnReassign.setEnabled(false);
            });
            dialog.open();

        });
        btnReassign.setEnabled(false);
        fromUserField.setLabel("From:");
        fromPanel.add(fromUserField);
        Button btnSelectFrom = new Button("Select from");
        btnSelectFrom.addClickListener(e -> {
            showChooseUserDialog(fromUserField, oidcUser, () -> {
                if (fromUserField.getUser() != null && toUserField.getUser() != null) {
                    btnReassign.setEnabled(true);
                } else {
                    btnReassign.setEnabled(false);
                }
            });

        });
        fromPanel.add(btnSelectFrom);
        form.add(fromPanel);

        VerticalLayout toPanel = new VerticalLayout();

        toPanel.add(toUserField);
        toUserField.setLabel("To:");
        Button btnSelectTo = new Button("Select to");
        btnSelectTo.addClickListener(e -> {
            showChooseUserDialog(toUserField, oidcUser, () -> {
                if (fromUserField.getUser() != null && toUserField.getUser() != null) {
                    btnReassign.setEnabled(true);
                } else {
                    btnReassign.setEnabled(false);
                }
            }
            );

        });
        toPanel.add(btnSelectTo);
        form.add(toPanel);
        form.setWidth("50%");
        VerticalLayout panel = new VerticalLayout();
        panel.add(new H4("Re-assign user"));
        panel.add(form);
        panel.add(btnReassign);
        panel.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BoxShadow.XSMALL,
                LumoUtility.BorderRadius.LARGE);

        return panel;
    }

    private void showChooseUserDialog(UserField userField, OidcUser currentUser, Runnable afterClose) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Choose user");

        UserSearchPanel usp = new UserSearchPanel(userService, Set.of(currentUser.getPreferredUsername()), false);
        Button btnClose = new Button("Close");
        btnClose.addClickListener(e -> dialog.close());
        Button btnSelect = new Button("Select");
        btnSelect.addClickListener(e -> {
            if (!usp.getSelectedItems().isEmpty()) {
                userField.setUser(usp.getSelectedItems().iterator().next());
                afterClose.run();
                dialog.close();
            }
        });
        btnClose.addClickListener(e -> {
            afterClose.run();
            dialog.close();
        });
        dialog.add(usp);
        dialog.getFooter().add(btnSelect, btnClose);
        dialog.open();
    }

//     public void showUserProfile(User user){
//         Dialog dialog = new Dialog();
//         UserProfile up = new UserProfile(user);
//         dialog.setHeaderTitle("User profile");
//         dialog.add(up);
//         Button close = new Button("Close");
//         close.addClickListener(e->dialog.close());
//         dialog.getFooter().add(close);
//     }
    public void showDocumentViewerFromNote(
            DocumentData doc,
            Long notifIdToBeHighlighted) {
    }
}
