/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.common;

import com.azrul.kelichap.autocomplete.Autocomplete;
import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.NotificationStatus;
import com.azrul.kelichap.domain.NotificationType;
import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.repository.AuditRepository;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.MapperService;
import com.azrul.kelichap.service.NotificationService;
import com.azrul.kelichap.service.UserService;
import com.azrul.kelichap.views.mydocuments.MyDocumentsView;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 *
 * @author azrul
 */
public class FolderPanel extends VerticalLayout {

    private final TreeGrid<Item> treeGrid = new TreeGrid<>();
    //private final VerticalLayout folderPanel = new VerticalLayout();
    private final MenuBar folderControl = new MenuBar();
    private PageNav pageNav;
    private final Checkbox cbActiveOnly = new Checkbox("Active folders only");

    private Integer FOLDER_COUNT_PER_PAGE;

    private final Pattern EXTRACT_USER = Pattern.compile("\\@\\[([^]]*)]");
    private final Pattern EXTRACT_USERNAME = Pattern.compile("\\(([^]]*)\\)");

    private final String BETWEEN_BRACKETS = "\\((.*?)\\)";

    private final String ALLOWED_FOLDER_NAME_PATTERN = "[A-Za-z0-9:\\.\\-\\s]";
    private final String ALLOWED_TAGS_PATTERN = "[a-zA-Z0-9 _#]";

    private Integer AUDIT_RECORD_COUNT_PER_PAGE;
    private Integer MAX_FILE_COUNT_PER_UPLOAD;
    private Integer MAX_FILE_SIZE;
    //Date format
    DatePicker.DatePickerI18n singleFormat = new DatePicker.DatePickerI18n();

    public FolderPanel() {
    }

    public void resetPageNav() {
        this.pageNav = null;
    }

    public void buildFolderTree(
            OidcUser oidcUser,
            DocumentService docService,
            MapperService mapperService,
            UserService userService,
            NotificationService notifService,
            AuditRepository auditRepo,
            BiConsumer<DocumentData, Boolean> showDocumentViewer,
            Integer folderCountPerPage,
            Integer auditRecordCountPerPage,
            Integer maxFileCountPerUpload,
            Integer maxFileSize
    ) {
        this.FOLDER_COUNT_PER_PAGE = folderCountPerPage;
        this.AUDIT_RECORD_COUNT_PER_PAGE = auditRecordCountPerPage;
        this.MAX_FILE_COUNT_PER_UPLOAD = maxFileCountPerUpload;
        this.MAX_FILE_SIZE = maxFileSize;

        Boolean isAdmin = oidcUser.getAuthorities().stream().anyMatch(
                sga -> StringUtils.equals(sga.getAuthority(), "ROLE_KELICHAP_ADMIN"));

        var dataProvider = buildItemDataProvider(oidcUser, docService);
        cbActiveOnly.setValue(true); //by default, search only active

        //need to put these two so that folderControl could shrink
        folderControl.setWidthFull();
        folderControl.setMinWidth("var(--lumo-size-m)");

        //Must initiate page nav before assigining data provider to treeGrid
        pageNav = new PageNav();

        pageNav.init(
                dataProvider,
                docService.countRootFolders(
                        Set.of(oidcUser.getPreferredUsername()),
                        cbActiveOnly.getValue(),
                        isAdmin
                ),
                FOLDER_COUNT_PER_PAGE,
                "id",
                false
        );
        //Assign data provider to treeGrid
        treeGrid.setDataProvider(dataProvider);
        treeGrid.addExpandListener(e -> {
            if (!e.getItems().isEmpty()) {
                treeGrid.select(e.getItems().iterator().next());
            }
        });
        treeGrid.addCollapseListener(e -> {
            if (!e.getItems().isEmpty()) {
                treeGrid.select(e.getItems().iterator().next());
            }
        });
        //set active-only checkbox behaviour
        cbActiveOnly.addValueChangeListener(e -> {
            pageNav.refreshPageNav(
                    docService.countRootFolders(
                            Set.of(oidcUser.getPreferredUsername()),
                            cbActiveOnly.getValue(),
                            isAdmin));
            treeGrid.getDataProvider().refreshAll();
        });
        MenuItem btnRefresh = isAdmin ? null : folderControl.addItem("Refresh", e -> {
            treeGrid.getDataProvider().refreshAll();
            pageNav.refreshPageNav(
                    docService.countRootFolders(
                            Set.of(oidcUser.getPreferredUsername()),
                            cbActiveOnly.getValue(),
                            isAdmin));
        });
        if (btnRefresh != null) {
            btnRefresh.setId("btnRefresh");
        }

        MenuItem btnUpload = isAdmin ? null : folderControl.addItem("Upload", e -> {
            showUploadDocumentDialog(oidcUser, docService, notifService);
        });
        if (btnUpload != null) {
            btnUpload.setId("btnUpload");
        }

        MenuItem btnDelete = isAdmin ? null : folderControl.addItem("Delete", e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Delete selected documents");
            dialog.setText(
                    "Are you sure you want to permanently delete these documents/folders (Deleting folders will delete ALL of its content)?");
            dialog.setCancelable(true);
            dialog.addCancelListener(event -> dialog.close());
            dialog.setConfirmText("Delete");
            dialog.setConfirmButtonTheme("error primary");
            dialog.addConfirmListener(event -> {
                Set<Item> items = treeGrid.getSelectedItems();
                Optional<Folder> ofolder = items
                        .stream()
                        .filter(Folder.class::isInstance)
                        .map(Folder.class::cast)
                        .findFirst();
                ofolder.ifPresent(folder -> {
                    Boolean success = docService.deleteFolder(folder, Set.of(oidcUser.getPreferredUsername()), cbActiveOnly.getValue());
                    if (success == false) {
                        Notification notification = new Notification();
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

                        Div text = new Div(new Text("Fail to delete"));

                        Button closeButton = new Button(new Icon("lumo", "cross"));
                        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                        closeButton.setAriaLabel("Close");
                        closeButton.addClickListener(e2 -> {
                            notification.close();
                        });

                        HorizontalLayout layout = new HorizontalLayout(text, closeButton);
                        layout.setAlignItems(Alignment.CENTER);

                        notification.add(layout);
                        notification.open();
                    }
                });
                Optional<DocumentData> odocs = items
                        .stream()
                        .filter(DocumentData.class::isInstance)
                        .map(DocumentData.class::cast)
                        .findFirst();
                odocs.ifPresent(doc -> {
                    docService.deleteDoc(doc, Set.of(oidcUser.getPreferredUsername()), cbActiveOnly.getValue());
                });
                //Must update pageNav before data provider is updated
                pageNav.refreshPageNav(
                        docService.countRootFolders(
                                Set.of(oidcUser.getPreferredUsername()),
                                cbActiveOnly.getValue(),
                                isAdmin));
                treeGrid.getDataProvider().refreshAll();
                treeGrid.deselectAll();
                dialog.close();
            });
            dialog.open();
        });
        if (btnDelete != null) {
            btnDelete.setId("btnDelete");
        }

        MenuItem btnCreateFolder = isAdmin ? null : folderControl.addItem("Create folder", (var e) -> {
            showCreateFolderDialog(oidcUser, docService, userService, notifService);
        });
        if (btnCreateFolder != null) {
            btnCreateFolder.setId("btnCreateFolder");
        }

        MenuItem btnClearSelection = folderControl.addItem("Clear selection", (var e) -> {
            treeGrid.deselectAll();
            btnCreateFolder.setEnabled(true);
            btnDelete.setEnabled(false);
            btnUpload.setEnabled(true);
        });
        btnClearSelection.setId("btnClearSelection");
        //Notification button
        //Button btnNotif = buildNotifButton();

        this.add(folderControl);

        HorizontalLayout folderControlSecondLine = new HorizontalLayout();
        folderControlSecondLine.add(cbActiveOnly/*, btnNotif*/);
        this.add(folderControlSecondLine);
        //Build folder/doc properties button ( ... )
        treeGrid.addComponentColumn(item -> {
            if (item instanceof DocumentData && isAdmin) {
                return new HorizontalLayout();
            } else {
                Button btnProperties = new Button("...");
                btnProperties.addClickListener(e -> {
                    showFolderOrDocUpdateDialog(item,
                            oidcUser,
                            docService,
                            userService,
                            mapperService,
                            notifService,
                            auditRepo,
                            showDocumentViewer);
                });
                btnProperties.getStyle().set("--lumo-button-size", "var(--lumo-size-xs)");
                btnProperties.setTooltipText("Properties");
                btnProperties.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

                return new HorizontalLayout(btnProperties);
            }
        }).setWidth("3rem").setResizable(true);
        treeGrid.addComponentHierarchyColumn(item -> {
            HorizontalLayout namePanel = new HorizontalLayout();
            if (item instanceof Folder folder) {
                // folder = docService.refresh(folder, Set.of(oidcUser.getPreferredUsername()));
                Boolean readable = (folder.getAccessMap().stream()
                        .filter(fa -> StringUtils.equals(fa.getAuthority(), oidcUser.getPreferredUsername())
                        && fa.getRights() == FolderRight.READ_FOLDER)
                        .count() > 0);
                Boolean updatable = (folder.getAccessMap().stream()
                        .filter(fa -> StringUtils.equals(fa.getAuthority(), oidcUser.getPreferredUsername())
                        && (fa.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                        || fa.getRights() == FolderRight.FOLDER_OWNER))
                        .count() > 0);
                if (!docService.isActive(folder)) {
                    Icon iconActive = VaadinIcon.CLOSE_CIRCLE.create();
                    namePanel.setFlexShrink(0, iconActive);
                    if (readable) {
                        Icon iconRead = VaadinIcon.FOLDER_SEARCH.create();
                        namePanel.setFlexShrink(0, iconRead);
                        namePanel.add(iconActive, iconRead, new Text(item.getName()));
                    } else if (updatable) {
                        Icon iconUpdate = VaadinIcon.FOLDER_ADD.create();
                        namePanel.setFlexShrink(0, iconUpdate);
                        namePanel.add(iconActive, iconUpdate, new Text(item.getName()));
                    } else {
                        Icon iconFolder = VaadinIcon.FOLDER_O.create();
                        namePanel.setFlexShrink(0, iconFolder);
                        namePanel.add(iconActive, iconFolder, new Text(item.getName()));
                    }
                } else {
                    if (readable) {
                        Icon iconRead = VaadinIcon.FOLDER_SEARCH.create();
                        namePanel.setFlexShrink(0, iconRead);
                        namePanel.add(iconRead, new Text(item.getName()));
                    } else if (updatable) {
                        Icon iconUpdate = VaadinIcon.FOLDER_ADD.create();
                        namePanel.setFlexShrink(0, iconUpdate);
                        namePanel.add(iconUpdate, new Text(item.getName()));
                    } else {
                        Icon iconFolder = VaadinIcon.FOLDER_O.create();
                        namePanel.setFlexShrink(0, iconFolder);
                        namePanel.add(iconFolder, new Text(item.getName()));
                    }
                }
            } else {
                Icon icon = VaadinIcon.FILE_TEXT_O.create();
                namePanel.setFlexShrink(0, icon);
                namePanel.add(icon, new Text(item.getName()));
            }
            return namePanel;
        }).setHeader("Name").setResizable(true).setAutoWidth(true).setSortProperty("name");
        treeGrid.addHierarchyColumn(item -> {
            return item.getLastModifiedDate() != null
                    ? item.getLastModifiedDate().format(DateTimeFormatter.ofPattern("dd-MMM-yy hh:mm:ss")) : "";
        }).setHeader("Update").setResizable(true).setAutoWidth(true).setSortProperty("lastModifiedDate");
        treeGrid.addItemDoubleClickListener(e -> {
            if (e.getItem() instanceof DocumentData document) {
                showDocumentViewer.accept(document, true);
            }
        });
        treeGrid.addSelectionListener(e -> {
            e.getFirstSelectedItem().ifPresent(item -> {
                //Calculate button enability for Create Upload and Delete
                Integer state = 0;
                if (item instanceof Folder folder) {
                    state = calculateToShowCreateUpdateDeleteButtons(folder, oidcUser, docService);
                } else {
                    state = calculateToShowCreateUpdateDeleteButtons(item.getParent(), oidcUser, docService);
                }
                if (state == 2) {
                    if (btnCreateFolder != null) {
                        btnCreateFolder.setEnabled(true);
                    }
                    if (btnDelete != null) {
                        btnDelete.setEnabled(true);
                    }
                    if (btnUpload != null) {
                        btnUpload.setEnabled(true);
                    }
                } else {
                    if (btnCreateFolder != null) {
                        btnCreateFolder.setEnabled(false);
                    }
                    if (btnDelete != null) {
                        btnDelete.setEnabled(false);
                    }
                    if (btnUpload != null) {
                        btnUpload.setEnabled(false);
                    }
                }

            });
        });
        treeGrid.setMultiSort(false);
        pageNav.setWidthFull();
        //HeaderRow headerRow =treeGrid.appendHeaderRow();
        this.add(pageNav);
        this.add(treeGrid);
    }

    private HierarchicalDataProvider buildItemDataProvider(OidcUser oidcUser, DocumentService docService) {
        Boolean isAdmin = oidcUser.getAuthorities().stream().anyMatch(
                sga -> StringUtils.equals(sga.getAuthority(), "ROLE_KELICHAP_ADMIN"));
        //build data provider
        HierarchicalDataProvider dp = new AbstractBackEndHierarchicalDataProvider<Item, Void>() {
            @Override
            protected Stream<Item> fetchChildrenFromBackEnd(HierarchicalQuery<Item, Void> hq) {
                QuerySortOrder so = hq.getSortOrders().isEmpty() ? null : hq.getSortOrders().get(0);
                return //When we are getting documents/folders under a folder, get all documents/folders
                        hq.getParentOptional().map(item -> {
                            if (item instanceof Folder folder) {
                                //When we are getting documents/folders under a folder, get all documents/folders
                                List<Item> items = docService.getItemsInFolder(
                                        folder,
                                        so == null ? "id" : so.getSorted(),
                                        so == null ? false : so.getDirection().equals(SortDirection.DESCENDING),
                                        Set.of(oidcUser.getPreferredUsername()),
                                        cbActiveOnly.getValue(),
                                        isAdmin
                                );
                                return items.stream();
                            } else {
                                return Stream.<Item>empty();
                            }
                        }).orElseGet(() -> {
                            List<Folder> items = docService.getRootFolders(
                                    so == null ? "id" : so.getSorted(),
                                    so == null ? false : so.getDirection().equals(SortDirection.DESCENDING),
                                    pageNav.getPage() - 1,
                                    pageNav.getMaxCountPerPage(),
                                    Set.of(oidcUser.getPreferredUsername()),
                                    cbActiveOnly.getValue(),
                                    isAdmin);

                            //save current sorting order in pagenav. Do it only for root to save time
                            pageNav.setSortField(so == null ? "id" : so.getSorted());
                            pageNav.setAsc(so == null ? false : so.getDirection().equals(SortDirection.DESCENDING));
                            return items.stream().map(Item.class::cast);
                        });
            }

            @Override
            public int getChildCount(HierarchicalQuery<Item, Void> hq) {
                return hq.getParentOptional().map(item -> {
                    if (item instanceof Folder folder) {
                        int c = docService.countItemsInFolder(
                                folder,
                                Set.of(oidcUser.getPreferredUsername()),
                                cbActiveOnly.getValue(),
                                isAdmin);
                        return c;
                    } else {
                        return 0;
                    }
                }).orElseGet(() -> {
                    int c = pageNav.getDataCountPerPage();
                    return c;
                });
            }

            @Override
            public boolean hasChildren(Item t) {
                if (t instanceof Folder folder) {
                    if (docService.countItemsInFolder(
                            folder,
                            Set.of(oidcUser.getPreferredUsername()),
                            cbActiveOnly.getValue(),
                            isAdmin
                    ) == 0) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        };
        return dp;
    }

    private void showCreateFolderDialog(
            OidcUser oidcUser,
            DocumentService docService,
            UserService userService,
            NotificationService notifService
    ) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setHeaderTitle("Create folder");
        dialog.setModal(true);
        dialog.setWidth("50%");
        dialog.setDraggable(true);
        dialog.setResizable(true);

        ComboBox<Folder> ddParentFolder = new ComboBox<>("Choose parent folder");
        ddParentFolder.setAllowCustomValue(false);

        Set<Folder> folders = docService.getUpdatableFolders(
                Set.of(oidcUser.getPreferredUsername()));
        ddParentFolder.setItems(folders);

        ddParentFolder.setClearButtonVisible(true);
        ddParentFolder.setPlaceholder("Create as root folder");
        ddParentFolder.setTooltipText("Choose existing folder as parent. Click x for root folder");
        ddParentFolder.setItemLabelGenerator(folder -> folder.getName() + "(" + StringUtils.defaultString(folder.getWorkflowId()) + ")");

        TextField tfFolderName = new TextField("Folder name");
        tfFolderName.setAllowedCharPattern(ALLOWED_FOLDER_NAME_PATTERN);
        tfFolderName.setId("fieldFolderName");

        Binder<Folder> binder = new Binder<>(Folder.class);
        binder.setBean(new Folder());
        binder.forField(tfFolderName)
                .withNullRepresentation("")
                .withValidator(v -> StringUtils.isNotBlank(v), "Name is compulsory")
                .bind("name");

        binder.forField(ddParentFolder)
                .bind("parent");

        NativeLabel inactive = new NativeLabel("Parent folder inactive");
        inactive.setVisible(false);

        HorizontalLayout userPanels = new HorizontalLayout();
        userPanels.setWidth("100%");
        Set<FolderAccess> sharedWithUserNames = new HashSet<>();
        buildUserAccessPanel(userPanels,
                Optional.empty(),
                sharedWithUserNames,
                oidcUser,
                docService,
                userService
        );
        VerticalLayout dialogPanel = new VerticalLayout();
        FormLayout formPanel = new FormLayout();
        formPanel.add(ddParentFolder);
        formPanel.add(tfFolderName);
        formPanel.add(inactive);
        dialogPanel.add(formPanel);
        dialogPanel.add(userPanels);
        dialog.add(dialogPanel);

        Boolean isAdmin = oidcUser.getAuthorities().stream().anyMatch(
                sga -> StringUtils.equals(sga.getAuthority(), "ROLE_KELICHAP_ADMIN"));

        Button btnCreateFolder = new Button("Create", ed -> {

            if (binder.validate().isOk()) {

                Folder folderData = binder.getBean();
                Folder newFolder = docService.createFolder(folderData.getName(), //lbSelectedUsers.getSelectedItems().stream().map(u -> u.getUsername()).collect(Collectors.toSet()),
                        folderData.getParent(),
                        sharedWithUserNames, null, Set.of(oidcUser.getPreferredUsername()));
                pageNav.refreshPageNav(
                        docService.countRootFolders(
                                Set.of(oidcUser.getPreferredUsername()),
                                cbActiveOnly.getValue(),
                                isAdmin));

                treeGrid.getDataProvider().refreshAll();
                treeGrid.expand(folderData.getParent());
                treeGrid.select(newFolder);

                sendFolderAssignmentNotif(
                        newFolder,
                        sharedWithUserNames.stream().map(FolderAccess::getAuthority).collect(Collectors.toList()),
                        oidcUser,
                        userService,
                        notifService
                );
                dialog.close();
            }
        });

        getSingleSelectedFolder().ifPresent(parentFolder -> {
            ddParentFolder.setValue(parentFolder);
            if (!docService.isActive(parentFolder)) {
                tfFolderName.setReadOnly(true);
                userPanels.setEnabled(false);
                inactive.setVisible(true);
            }
        });
        ddParentFolder.addValueChangeListener(e -> {
            Folder folder = e.getValue();
            if (folder != null) { //we choose non-root
                if (!docService.isActive(folder)) {
                    tfFolderName.setReadOnly(true);
                    userPanels.setEnabled(false);
                    inactive.setVisible(true);
                } else {
                    tfFolderName.setReadOnly(false);
                    userPanels.setEnabled(true);
                    inactive.setVisible(false);
                }
            }
        });
        btnCreateFolder.setId("btnCreateFolder");
        dialog.getFooter().add(btnCreateFolder);
        Button btnCancel = new Button("Cancel", ed -> dialog.close());
        btnCancel.setId("btnCancel");
        dialog.getFooter().add(btnCancel);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                treeGrid.deselectAll(); //prevent selecting inactive
            }
        });
        dialog.open();
    }

    private void showUploadDocumentDialog(OidcUser oidcUser, DocumentService docService, NotificationService notifService) {
        if (docService.countUpdatableFolders(
                Set.of(oidcUser.getPreferredUsername()),
                cbActiveOnly.getValue()) > 0) {
            //get all accessible folders
            Set<Folder> folders = docService.getUpdatableFolders(
                    Set.of(oidcUser.getPreferredUsername()));
            final MultiFileBuffer buffer = new MultiFileBuffer();
            //Build dialog
            Dialog dialog = new Dialog();
            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);
            dialog.setHeaderTitle("Upload");
            dialog.setModal(true);
            dialog.setWidth("30%");
            dialog.setResizable(true);
            dialog.setDraggable(true);
            FormLayout dialogPanel = new FormLayout();
            dialogPanel.setWidth("100%");

            //List<String> folderNames = folders.stream().map(item -> item.getName()).collect(Collectors.toList());
            ComboBox<Folder> ddFolderNames = new ComboBox<>("Choose existing folder");
            ddFolderNames.setAllowCustomValue(false);
            ddFolderNames.setItems(folders);
            ddFolderNames.setItemLabelGenerator(folder -> folder.getName() + "(" + StringUtils.defaultString(folder.getWorkflowId()) + ")");

            getSingleSelectedFolder()
                    .ifPresentOrElse(
                            item -> ddFolderNames.setValue(item),
                            () -> ddFolderNames.setValue(folders.iterator().next()));
            Autocomplete acTags1 = buildTagField(Set.of(), oidcUser, docService);

            Upload upload = new Upload(buffer);
            upload.setId("upload");
            upload.setMaxFiles(MAX_FILE_COUNT_PER_UPLOAD);
            upload.setMaxFileSize(MAX_FILE_SIZE); //12MB
            String[] ext = docService.getSupportedFileExtension();
            upload.setAcceptedFileTypes(ext);

            NativeLabel inactive = new NativeLabel("Parent folder inactive");
            inactive.setVisible(false);

            dialogPanel.add(ddFolderNames, 2);
            dialogPanel.add(inactive, 2);

            dialogPanel.add(acTags1, 2);
            Span span = new Span();
            span.getElement().setProperty("innerHTML", "<br>");
            dialogPanel.add(span);

            NativeLabel uploadLabel = new NativeLabel("Upload multiple files");
            uploadLabel.setFor(upload);
            dialogPanel.add(upload, 2);

            //Inactive
            if (docService.isActive((Folder) ddFolderNames.getValue()) == false) {
                acTags1.setReadOnly(true);
                upload.setVisible(false);
                inactive.setVisible(true);
            }

            ddFolderNames.addValueChangeListener(e -> {
                Folder folder = e.getValue();
                if (folder != null) {
                    if (!docService.isActive(folder)) {
                        acTags1.setReadOnly(true);
                        upload.setVisible(false);
                        inactive.setVisible(true);
                    } else {
                        acTags1.setReadOnly(false);
                        upload.setVisible(true);
                        inactive.setVisible(false);
                    }
                }
            });

            Button btnDone = new Button("Save", e3 -> {

                try {
                    for (var file : buffer.getFiles()) {
                        InputStream inputStream = buffer.getInputStream(file);
                        final String mimeType = buffer.getFileData(file).getMimeType();
                        byte[] fileContent = inputStream.readAllBytes();
                        final Long size = Integer.valueOf(fileContent.length).longValue();
                        final String originalFileName = buffer.getFileData(file).getFileName();

                        var opDoc = docService.addDocument(
                                ddFolderNames.getValue().getWorkflowId(),
                                oidcUser.getPreferredUsername(),
                                Set.of(acTags1.getValue().split("\\s+")),
                                ddFolderNames.getValue().getId(),
                                originalFileName,
                                mimeType,
                                size,
                                fileContent,
                                ddFolderNames.getValue().getName(),
                                Set.of()
                        );
                        opDoc.ifPresent(doc -> {
                            if (treeGrid.isExpanded(doc.getParent())) {
                                treeGrid.getDataProvider().refreshItem(doc.getParent(), true);
                            } else {
                                treeGrid.expand(doc.getParent());
                                treeGrid.getDataProvider().refreshAll();
                            }
                            List<String> readersExCurrentUser = docService.getReadersUserName(doc.getParent())
                                    .stream()
                                    .filter(auth -> !StringUtils.equals(oidcUser.getPreferredUsername(), auth))
                                    .collect(Collectors.toList());

                            sendFileUploadNotif(doc, readersExCurrentUser, oidcUser, notifService);

                            treeGrid.select(doc);
                        });
                    }
                } catch (IOException ex) {
                    LogFactory.getLog(MyDocumentsView.class.getName()).fatal("Error", ex);
                }
                dialog.close();
            });
            Button btnCancel = new Button("Cancel", e3 -> {
                dialog.close();
            });

            btnDone.setEnabled(false);
            upload.addAllFinishedListener(e2 -> {
                btnDone.setEnabled(true);
            });
            dialog.add(dialogPanel);
            dialog.getFooter().add(btnDone, btnCancel);
            dialog.addOpenedChangeListener(event -> {
                if (!event.isOpened()) {
                    treeGrid.deselectAll(); //prevent selecting inactive
                }
            });
            dialog.open();
        }
    }

    public void openFromDocToRootFolder(
            Long docId,
            OidcUser oidcUser,
            DocumentService docService) {
        //go to page where the root folder is

        //expand all the way to the root folder
        docService.getDocById(
                docId,
                Set.of(oidcUser.getPreferredUsername()),
                cbActiveOnly.getValue()
        ).ifPresent(d -> {
            openFromFolderToRootFolder(d, true, oidcUser, docService);
        });
    }

    public void openFromFolderToRootFolder(
            Long folderId,
            OidcUser oidcUser,
            DocumentService docService) {
        //go to page where the root folder is

        Boolean isAdmin = isCurrentUserAdmin(oidcUser);

        //expand all the way to the root folder
        docService.getFolderById(
                folderId,
                Set.of(oidcUser.getPreferredUsername()),
                cbActiveOnly.getValue(),
                isAdmin
        ).ifPresentOrElse(d -> {
            openFromFolderToRootFolder(d, true, oidcUser, docService);
        }, () -> {
            Notification notification = new Notification();
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setDuration(5000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.add(new Text("Could not find the specified folder"));
            notification.open();
        });
    }

    public void openFromFolderToRootFolder(Item startItem, Boolean highlighResult, OidcUser oidcUser, DocumentService docService) {
        Boolean isAdmin = isCurrentUserAdmin(oidcUser);

        Folder startFolder = Stream.of(startItem).map(it -> {
            if (it instanceof DocumentData) {
                return it.getParent();
            } else {
                return it;
            }
        }).map(Folder.class::cast).findFirst().orElse(null);

        // Long folderId = startFolder.getId();
        //Firstly let's find which page would the root folder reside in. Go to that page
        Integer page = docService.getPageWhereFolderIs(
                startFolder.getRoot().getId(),
                pageNav.getSortField(),
                pageNav.getAsc(),
                oidcUser.getPreferredUsername(),
                pageNav.getMaxCountPerPage(),
                cbActiveOnly.getValue(),
                isAdmin
        );
        pageNav.jumpToPage(page);

        //Then, for all folder leading to the the root, expand it
        List<Long> pathToRoot = startFolder.getPathToRoot();
        List<Folder> foldersInPath = docService.getAllFoldersWithIds(pathToRoot);
        for (var folder : foldersInPath) {
            treeGrid.expand(folder);
        }

        //lastly also eexpand the start folder
        treeGrid.expand(startFolder);

        if (highlighResult) {
            treeGrid.select(startItem);
        }

    }

    private Boolean isCurrentUserAdmin(OidcUser oidcUser) {
        Boolean isAdmin = oidcUser.getAuthorities().stream().anyMatch(
                sga -> StringUtils.equals(sga.getAuthority(), "ROLE_KELICHAP_ADMIN"));
        return isAdmin;
    }

    private Optional<Folder> getSingleSelectedFolder() {
        Set<Item> items = treeGrid.getSelectedItems();
        if (items.isEmpty()) {
            return Optional.empty();
        }

        Item one = items.iterator().next();
        if (one instanceof DocumentData dd) {
            return Optional.of(dd.getParent());
        } else {
            return Optional.of((Folder) one);
        }
    }

    public Set<Item> getSelectedItems() {
        return treeGrid.getSelectedItems();
    }

    public Boolean isActiveOnly() {
        return cbActiveOnly.getValue();
    }

    public void showFolderOrDocUpdateDialog(
            Item item,
            OidcUser oidcUser,
            DocumentService docService,
            UserService userService,
            MapperService mapperService,
            NotificationService notifService,
            AuditRepository auditRepo,
            BiConsumer<DocumentData, Boolean> showDocumentViewer
    ) {

        if (item instanceof Folder folder) {
            //final Folder folder = docService.refresh(_folder, Set.of(oidcUser.getPreferredUsername()));
            String ownerUserName = docService.getOwner(folder);
            //Main form
            FormLayout mainForm = new FormLayout();
            Binder<Folder> binder = new Binder<>(Folder.class);
            binder.setBean(folder);

            TextField nameField = new TextField("Name:");

            nameField.setAllowedCharPattern(ALLOWED_FOLDER_NAME_PATTERN);
            nameField.setReadOnly(true);
            nameField.setId("fieldFolderName");
            binder.forField(nameField)
                    .withNullRepresentation("")
                    .withValidator(v -> StringUtils.isNotBlank(v), "Name is compulsory")
                    .bind("name");

            mainForm.add(nameField);

            TextField idField = new TextField("Id:");
            idField.setId("fieldFolderId");
            binder.forField(idField)
                    .withNullRepresentation("")
                    .withConverter(new Converter<String, Long>() {
                        @Override
                        public Result<Long> convertToModel(String prsntn, ValueContext vc) {
                            if (prsntn != null) {
                                return Result.ok(Long.valueOf(prsntn));
                            } else {
                                return Result.error("Conversion error for:" + prsntn);
                            }
                        }

                        @Override
                        public String convertToPresentation(Long model, ValueContext vc) {
                            if (model != null) {
                                return model.toString();
                            } else {
                                return "";
                            }
                        }

                    })
                    .bindReadOnly("id");
            mainForm.add(idField);

            TextField wfField = new TextField("Workflow:");
            binder.forField(wfField)
                    .withNullRepresentation("")
                    .bindReadOnly("workflowId");
            mainForm.add(wfField);

            DateTimePicker updateDateField = new DateTimePicker("Last update:");
            updateDateField.setDatePickerI18n(this.singleFormat);
            binder.forField(updateDateField)
                    .bindReadOnly("lastModifiedDate");
            mainForm.add(updateDateField);

            TextField myAccessField = new TextField("My access:");
            FolderRight myRight = folder
                    .getAccessMap()
                    .stream()
                    .filter(fa -> oidcUser.getPreferredUsername().equals(fa.getAuthority()))
                    .findFirst()
                    .map(fa -> fa.getRights())
                    .orElse(null);

            myAccessField.setValue(myRight == null ? "Restricted" : myRight.getHumanReadableValue());
            myAccessField.setReadOnly(true);

            mainForm.add(myAccessField);

            User owner = StringUtils.equals(oidcUser.getPreferredUsername(), ownerUserName)
                    //if owner of folder is the current user...
                    ? mapperService.map(oidcUser)
                    //else ...
                    : userService.getUser(ownerUserName);

            UserField ownerField = new UserField(owner, true); //buildUserField(ownerUserName);

            mainForm.add(ownerField);

            Checkbox cbIsActive = new Checkbox("Is active:");
            cbIsActive.setValue(docService.isActive(folder));
            cbIsActive.setEnabled(false);
            mainForm.add(cbIsActive);

            Dialog configDialog = new Dialog();
            configDialog.setCloseOnEsc(false);
            configDialog.setCloseOnOutsideClick(false);

            configDialog.setHeaderTitle("Folder");
            configDialog.setResizable(true);
            configDialog.setDraggable(true);
            configDialog.setWidth("60%");

            Tab tab = new Tab();
            configDialog.add(tab);

            TabSheet mainSheet = new TabSheet();

            mainSheet.add("Properties", mainForm);
            tab.add(mainSheet);

            HorizontalLayout userPanel = new HorizontalLayout();
            Set<FolderAccess> resultantSelectedUserNames = new HashSet<>();

            nameField.setReadOnly(false);
            buildUserAccessPanel(userPanel,
                    Optional.of(folder),
                    resultantSelectedUserNames,
                    oidcUser,
                    docService,
                    userService
            );
            configDialog.add(userPanel);

            Boolean isAdmin = isCurrentUserAdmin(oidcUser);

            Button btnSubmit = new Button("Submit", ed -> {

                if (binder.validate().isOk()) {
                    Folder newFolder = binder.getBean();
                    docService.updateFolder(folder.getId(),
                            newFolder.getName(),
                            cbIsActive.getValue(),
                            resultantSelectedUserNames,
                            Set.of(oidcUser.getPreferredUsername()),
                            isAdmin);
                    pageNav.refreshPageNav(
                            docService.countRootFolders(
                                    Set.of(oidcUser.getPreferredUsername()),
                                    cbActiveOnly.getValue(),
                                    isAdmin
                            ));
                    treeGrid.getDataProvider().refreshAll();

                    List<FolderAccess> newSharedUsers = resultantSelectedUserNames
                            .stream()
                            .filter(rfa -> !folder.getAccessMap().contains(rfa))
                            .collect(Collectors.toList());

                    sendFolderAssignmentNotif(
                            folder,
                            newSharedUsers.stream().map(FolderAccess::getAuthority).collect(Collectors.toList()),
                            oidcUser,
                            userService,
                            notifService);

                    configDialog.close();
                }
            });
            btnSubmit.setId("btnSubmit");

            cbIsActive.addValueChangeListener(e -> {
                calculateEnabilityOfFolderConfigDialog(
                        folder,
                        e.getValue(),
                        nameField,
                        userPanel,
                        cbIsActive,
                        btnSubmit,
                        oidcUser
                );
            });

            calculateEnabilityOfFolderConfigDialog(
                    folder,
                    docService.isActive(folder),
                    nameField,
                    userPanel,
                    cbIsActive,
                    btnSubmit,
                    oidcUser
            );

            if (isAdmin == false && (myRight == null || myRight == FolderRight.READ_FOLDER)) {
                Button btnAskAccess = new Button("Ask for access", ea -> showRequestAccessDialog(folder, oidcUser, docService, userService, notifService));
                configDialog.getFooter().add(btnAskAccess);
            }
            Button btnClose = new Button("Close", ed -> configDialog.close());
            btnClose.setId("btnClose");

            configDialog.getFooter().add(btnSubmit);
            configDialog.getFooter().add(btnClose);
            configDialog.addOpenedChangeListener(event -> {
                if (!event.isOpened()) {
                    treeGrid.deselectAll(); //prevent selecting inactive
                }
            });
            configDialog.setCloseOnOutsideClick(false);
            configDialog.open();

        } else {

            docService.getDocById(
                    item.getId(),
                    Set.of(oidcUser.getPreferredUsername()),
                    cbActiveOnly.getValue()
            ).ifPresent(doc -> {
                //Main form
                Dialog configDialog = new Dialog();
                configDialog.setCloseOnEsc(false);
                configDialog.setCloseOnOutsideClick(false);
                configDialog.setWidth("50%");
                configDialog.setHeight("75%");
                configDialog.setHeaderTitle("Document");

                FormLayout mainForm = new FormLayout();
                mainForm.setWidth("100%");

                TextField tfFolderName = new TextField("Folder Name:");
                tfFolderName.setId("fieldFolderName");
                tfFolderName.setValue(doc.getName());
                tfFolderName.setReadOnly(true);
                mainForm.add(tfFolderName);

                TextField idField = new TextField("Id:");
                idField.setValue(doc.getId().toString());
                //nameField.setAllowedCharPattern("[A-Za-z0-9 _]");
                idField.setReadOnly(true);
                idField.setId("fieldDocId");
                mainForm.add(idField);

                Autocomplete acTags2 = buildTagField(doc.getTags(), oidcUser, docService);
                acTags2.setReadOnly(true);
                mainForm.add(acTags2);

                PageNav auditPageNav = new PageNav();
                DataProvider docAuditDataProvider = buildDocAuditDataProvider(doc, auditRepo, auditPageNav);
                auditPageNav.init(
                        docAuditDataProvider,
                        auditRepo.getRevisionCount(DocumentData.class, "id", doc.getId()),
                        AUDIT_RECORD_COUNT_PER_PAGE,
                        "id",
                        false
                );

                TabSheet mainSheet = new TabSheet();
                mainSheet.setWidthFull();
                mainSheet.add("Properties", mainForm);
                configDialog.add(mainSheet);
                //Version form
                Grid<DocumentData> docGrid = new Grid<>(DocumentData.class, false);

                docGrid.setHeight("20em");
                docGrid.addComponentColumn(d -> new Text(Integer.toString(d.getVersion())))
                        .setHeader("Version")
                        .setResizable(true)
                        .setSortable(true)
                        .setSortProperty("version");
                docGrid.addComponentColumn(d -> {
                    String name = d.getLastModifiedBy();
                    Avatar avUser = new Avatar(name.replaceAll(BETWEEN_BRACKETS, ""));
                    avUser.addThemeVariants(AvatarVariant.LUMO_XSMALL);
                    int index = Math.abs(name.hashCode() % 7 + 1);
                    avUser.setColorIndex(index);
                    Div divName = new Div(name);
                    divName.getStyle().set("display", "flex");
                    divName.getStyle().set("align-items", "center");
                    HorizontalLayout divUser = new HorizontalLayout();
                    divUser.add(avUser, divName);
                    return divUser;
                }).setHeader("Last modified by")
                        .setResizable(true)
                        .setSortable(true)
                        .setSortProperty("lastModifiedBy");
                docGrid.addComponentColumn(d -> {
                    return new Text(d.getLastModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
                })
                        .setHeader("Last modified date")
                        .setResizable(true)
                        .setSortable(true)
                        .setSortProperty("lastModifiedDate");

                docGrid.addComponentColumn(d -> {
                    if (d.getSize() != null) {
                        Button b = new Button("Open", be -> {
                            showDocumentViewer.accept(d, doc.getVersion().equals(d.getVersion()));
                            configDialog.close();
                        });
                        return b;
                    } else {
                        return new Text("");
                    }

                }).setHeader("Open");
                docGrid.setItems(docAuditDataProvider);
                VerticalLayout versionPanel = new VerticalLayout();
                versionPanel.add(auditPageNav, docGrid);

                mainSheet.add("Version", versionPanel);

                configDialog.getFooter().add(new Button("Open document", ed -> {
                    configDialog.close();
                    showDocumentViewer.accept(doc, true);

                }));
                docService.getFolderOfDocIfUpdatable(
                        doc,
                        Set.of(oidcUser.getPreferredUsername()),
                        cbActiveOnly.getValue()).ifPresentOrElse(folder -> {
                    //active or not. IInactive folder should not be updatable
                    if (docService.isActive(folder)) {
                        acTags2.setReadOnly(false);
                    }
                    Button btnSubmit = new Button("Submit", ed -> {
                        if (acTags2.getValue() != null) {
                            docService.updateDocTags(doc.getId(),
                                    Set.of(acTags2.getValue().split("\\s+")),
                                    Set.of(oidcUser.getPreferredUsername()),
                                    cbActiveOnly.getValue());
                        } else {
                            docService.updateDocTags(doc.getId(),
                                    Set.of(),
                                    Set.of(oidcUser.getPreferredUsername()),
                                    cbActiveOnly.getValue());
                        }
                        treeGrid.getDataProvider().refreshAll();
                        configDialog.close();
                    });
                    btnSubmit.setId("btnSubmit");
                    configDialog.getFooter().add(btnSubmit);
                    Button btnCancel = new Button("Cancel", ed -> configDialog.close());
                    btnCancel.setId("btnCancel");
                    configDialog.getFooter().add(btnCancel);
                }, () -> {
                    Button btnClose = new Button("Close", ed -> configDialog.close());
                    btnClose.setId("btnClose");
                    configDialog.getFooter().add(btnClose);
                });
                configDialog.addDialogCloseActionListener(e -> {
                    treeGrid.deselectAll(); //prevent selecting inactive
                });
                //configDialog.setCloseOnOutsideClick(false);
                configDialog.open();
            });
        }
    }

    private DataProvider buildDocAuditDataProvider(DocumentData doc, AuditRepository auditRepo, PageNav auditPageNav) {
        //build data provider
        var dp = new AbstractBackEndDataProvider<DocumentData, Void>() {
            @Override
            protected Stream<DocumentData> fetchFromBackEnd(Query<DocumentData, Void> query) {
                QuerySortOrder so = query.getSortOrders().isEmpty() ? null : query.getSortOrders().get(0);
                query.getPage();
                List<DocumentData> auditedDocs = auditRepo.getRevisions(
                        DocumentData.class, "id", doc.getId(),
                        (auditPageNav.getPage() - 1),
                        auditPageNav.getMaxCountPerPage(),
                        so == null ? "id" : so.getSorted(),
                        so == null ? false : so.getDirection().equals(SortDirection.ASCENDING));

                return auditedDocs.stream();
            }

            @Override
            protected int sizeInBackEnd(Query<DocumentData, Void> query) {

                return auditPageNav.getDataCountPerPage();
            }

            @Override
            public String getId(DocumentData item) {
                return item.getId().toString() + "--" + item.getVersion();//item.getLastModifiedDate().format(DateTimeFormatter.ISO_DATE);
            }

        };
        return dp;
    }

    private MultiSelectListBox<User> buildUserAccessPanel(
            HorizontalLayout userPanels,
            Optional<Folder> ofolder,
            Set<FolderAccess> resultantSelectedUserNames,
            OidcUser oidcUser,
            DocumentService docService,
            UserService userService
    ) {
        Set<FolderAccess> currentSelectedUserNames = ofolder.map(folder
                -> docService.getAccessMap(folder)
                        .stream()
                        .filter(fa -> !oidcUser.getPreferredUsername().equals(fa.getAuthority()))
                        .collect(Collectors.toSet())).orElse(Set.of());

        //Identiify owner
        String owner = currentSelectedUserNames.stream()
                .filter(u -> u.getRights().equals(FolderRight.FOLDER_OWNER))
                .map(u -> u.getAuthority())
                .findFirst()
                .orElse(null);

        //Build panel
        final Map<String, FolderAccess> selectedUserFA = new HashMap<>();
        final Map<String, User> selectedUserRep = new HashMap<>();
        final Set<String> origSelectedUserNames = new HashSet<>();

        for (var e : currentSelectedUserNames) {
            if (!StringUtils.equals(e.getAuthority(), owner)) {//filter owner
                selectedUserFA.put(e.getAuthority(), e);
                origSelectedUserNames.add(e.getAuthority());
            }
        }

        userService.getUsers(selectedUserFA.keySet()).stream().forEach(user -> {
            if (!StringUtils.equals(user.getUsername(), owner)) { //filter out owner
                selectedUserRep.put(user.getUsername(), user);
            }
        });

        VerticalLayout buttonPanel = new VerticalLayout();
        buttonPanel.setAlignItems(FlexComponent.Alignment.START);
        Button btnTransfer = new Button("Transfer >>");
        btnTransfer.setEnabled(false);
        btnTransfer.setWidth("100%");
        btnTransfer.setId("btnTransfer");
        Button btnRetract = new Button("<< Retract");
        btnRetract.setWidth("100%");
        btnRetract.setEnabled(true);
        btnRetract.setId("btnRetract");

        ComboBox<FolderRight> cbRights = new ComboBox<>();
        cbRights.setItems(
                EnumSet.allOf(
                        FolderRight.class).stream().filter(
                                r -> r != FolderRight.FOLDER_OWNER).collect(Collectors.toSet())
        );
        cbRights.setAllowCustomValue(false);
        cbRights.setWidth("100%");
        cbRights.setId("comboBoxRights");

        cbRights.addValueChangeListener(e -> {

            if (e.getValue() != null) {
                btnTransfer.setEnabled(true);
            } else {
                btnTransfer.setEnabled(false);
            }
        });

        cbRights.setItemLabelGenerator(r -> r.getHumanReadableValue());

        buttonPanel.add(cbRights, btnTransfer, btnRetract);

        VerticalLayout selectedPanel = new VerticalLayout();
        selectedPanel.add(new Text("Selected users"));
        //HorizontalLayout searchUserPanel = new HorizontalLayout();

        MultiSelectListBox<User> lbSelectedUsers = new MultiSelectListBox<>();
        lbSelectedUsers.getStyle().set("border", "2px solid lightgray");
        lbSelectedUsers.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        lbSelectedUsers.setWidth("100%");
        lbSelectedUsers.setHeight("100%");
        List<User> selectedUserRepValues = new ArrayList<>();
        selectedUserRepValues.addAll(selectedUserRep.values());
        Collections.sort(selectedUserRepValues, getUserComparator());
        lbSelectedUsers.setItems(selectedUserRepValues);

        lbSelectedUsers.setId("listBoxSelectedUsers");
        lbSelectedUsers.setRenderer(new ComponentRenderer<>(
                user -> {
                    if (selectedUserFA.containsKey(user.getUsername())) {

                        String name = user.getFirstName()
                        + " " + user.getLastName() + " (" + user.getUsername() + ")";
                        Avatar avUser = new Avatar(name.replaceAll(BETWEEN_BRACKETS, ""));
                        avUser.addThemeVariants(AvatarVariant.LUMO_XSMALL);
                        int index = Math.abs(name.hashCode() % 7 + 1);
                        avUser.setColorIndex(index);

                        Boolean newUser = !origSelectedUserNames.contains(user.getUsername());
                        Span newUserBadge = new Span("new");
                        newUserBadge.getElement().getThemeList().add("badge");
                        newUserBadge.getStyle().set("font-size", "xx-small");
                        newUserBadge.getStyle().set("background", "gold");
                        newUserBadge.getStyle().set("color", "black");

                        Icon addIcon = VaadinIcon.FOLDER_ADD.create();
                        Icon searchIcon = VaadinIcon.FOLDER_SEARCH.create();

                        if (null == selectedUserFA.get(user.getUsername()).getRights()) {
                            return new HorizontalLayout();
                        } else {
                            switch (selectedUserFA.get(user.getUsername()).getRights()) {
                                case CREATE_UPDATE_FOLDER:
                                    if (newUser) {
                                        var namePanel = new HorizontalLayout(addIcon, avUser, new Text(name), newUserBadge);
                                        namePanel.setFlexShrink(0, addIcon);
                                        return namePanel;
                                    } else {
                                        var namePanel = new HorizontalLayout(addIcon, avUser, new Text(name));
                                        namePanel.setFlexShrink(0, addIcon);
                                        return namePanel;
                                    }
                                case READ_FOLDER:
                                    if (newUser) {
                                        var namePanel = new HorizontalLayout(searchIcon, avUser, new Text(name), newUserBadge);
                                        namePanel.setFlexShrink(0, searchIcon);
                                        return namePanel;
                                    } else {
                                        var namePanel = new HorizontalLayout(searchIcon, avUser, new Text(name));
                                        namePanel.setFlexShrink(0, searchIcon);
                                        return namePanel;
                                    }
                                default:
                                    return new HorizontalLayout();
                            }
                        }
                    } else {
                        return new HorizontalLayout();
                    }
                }));
        selectedPanel.add(lbSelectedUsers);

        Set<String> filteredOut = new HashSet<>();
        filteredOut.addAll(selectedUserFA.keySet());
        filteredOut.add(oidcUser.getPreferredUsername());
        UserSearchPanel userSearchPanel = new UserSearchPanel(userService, filteredOut, true);
        // MultiSelectListBox<User> lbNotSelectedUsers = userSearchPanel(userPanels, filteredOut);
        userPanels.add(userSearchPanel, buttonPanel, selectedPanel);

        btnTransfer.addClickListener(e -> {

            userSearchPanel.getSelectedItems()
                    .stream()
                    .forEach(u -> {
                        selectedUserRep.put(u.getUsername(), u);
                        var fa = new FolderAccess(u.getUsername(), cbRights.getValue());
                        selectedUserFA.put(fa.getAuthority(), fa);
                    });

            //Must add dp resultantSelectedUserNames first before setting items to lbSelectedUsers
            resultantSelectedUserNames.clear();
            resultantSelectedUserNames.addAll(selectedUserFA.values());

            userSearchPanel.filterOutSelectedItems();
            var filteredUsers2 = new ArrayList(selectedUserRep.values());
            Collections.sort(filteredUsers2, getUserComparator());
            lbSelectedUsers.setItems(filteredUsers2);
        });
        btnRetract.addClickListener(e -> {
            var removedUsers = new HashSet<User>();
            lbSelectedUsers.getSelectedItems().stream().forEach(u -> {
                selectedUserFA.remove(u.getUsername());
                selectedUserRep.remove(u.getUsername());
                removedUsers.add(u);
            });
            userSearchPanel.addItems(removedUsers);

            //Must add dp resultantSelectedUserNames first before setting items to lbSelectedUsers
            resultantSelectedUserNames.clear();
            resultantSelectedUserNames.addAll(selectedUserFA.values());

            var filteredUsers3 = new ArrayList(selectedUserRep.values());
            Collections.sort(filteredUsers3, getUserComparator());
            lbSelectedUsers.setItems(filteredUsers3);
        });

        return lbSelectedUsers;
    }

    private Comparator getUserComparator() {
        return new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return StringUtils.compare(o1.getUsername(), o2.getUsername());
            }
        };
    }

    private Autocomplete buildTagField(Set<String> currentTags, OidcUser oidcUser, DocumentService docService) {
        Set<String> allTags = docService.getAllAccessibleTags(Set.of(oidcUser.getPreferredUsername()));
        Autocomplete acTags = new Autocomplete("#", false);
        acTags.setLabel("Tags");
        acTags.setValue(StringUtils.join(currentTags, " "));
        acTags.setWidthFull();
        acTags.setAllowedCharPattern(ALLOWED_TAGS_PATTERN);

        acTags.addTokenListener(event -> {
            String text = event.getToken();
            if (text != null) {
                List<String> selectedTags = allTags.stream().filter(u -> {
                    return StringUtils.containsIgnoreCase(u, text.trim().replaceAll("[^\\p{L}\\p{Nd}]+", ""));
                }).collect(Collectors.toList());
                acTags.setOptions(selectedTags);
            }
        });
        return acTags;
    }

    private Integer calculateToShowCreateUpdateDeleteButtons(Folder folder, OidcUser oidcUser, DocumentService docService) {
        folder = docService.refresh(folder, Set.of(oidcUser.getPreferredUsername()));

        Integer state;
        if (docService.isActive(folder)) {
            state = 1;
        } else {
            state = 0;
        }
        if (state == 1) {
            Optional<FolderAccess> ofolderAccess = folder
                    .getAccessMap()
                    .stream()
                    .filter(fa
                            -> fa.getAuthority().equals(oidcUser.getPreferredUsername())
                    && (fa.getRights() == FolderRight.CREATE_UPDATE_FOLDER
                    || fa.getRights() == FolderRight.FOLDER_OWNER))
                    .findFirst();
            state = ofolderAccess.map(fa -> 2).orElse(0);

        }
        return state;
    }

    public void showRequestAccessDialog(Folder folder, OidcUser oidcUser, DocumentService docService, UserService userService, NotificationService notifService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request access");

        String ownerUserName = docService.getOwner(folder);
        Optional<FolderRight> oCurrentUserRight = folder.getAccessMap().stream().filter(fa -> StringUtils.equals(fa.getAuthority(), oidcUser.getPreferredUsername())).map(FolderAccess::getRights).findFirst();
        User ownerUR = userService.getUser(ownerUserName);//allUsers.get(ownerUserName);

        Button btnSendRequest = new Button("Send request");
        VerticalLayout panel = new VerticalLayout();
        panel.add(new Text("Request from " + ownerUR.getFirstName() + " " + ownerUR.getLastName() + "(" + ownerUR.getUsername() + ") to access '" + folder.getName() + "'"));

        ComboBox<FolderRight> cbAccess = new ComboBox<>("Choose the rights requested");
        cbAccess.setWidthFull();
        List rightsOptions = oCurrentUserRight.map(currentUserRight -> {
            if (currentUserRight == FolderRight.CREATE_UPDATE_FOLDER) { //if you alreeady have the write access (the biggest access under OWNER) - no need to request for more
                return List.of();
            } else {
                return Arrays.asList(FolderRight.values())
                        .stream()
                        .filter(fr -> fr != FolderRight.FOLDER_OWNER && fr != currentUserRight)
                        .collect(Collectors.toList());

            }

        }).orElseGet(() -> {

            return Arrays.asList(FolderRight.values())
                    .stream()
                    .filter(fr -> fr != FolderRight.FOLDER_OWNER)
                    .collect(Collectors.toList());

        });
        cbAccess.setItems(rightsOptions);
        panel.add(cbAccess);
        dialog.add(panel);

        btnSendRequest.setId("btnSendRequest");

        btnSendRequest.addClickListener(c -> {
            //System.out.println(cbAccess.getValue().toString()+":"+cbAccess.getValue().getClass());
            Boolean newNotif = sendNotifRequestAccess(folder, ownerUserName, cbAccess.getValue(), oidcUser, notifService);
            if (newNotif == false) {
                var notification = com.vaadin.flow.component.notification.Notification.show("This request was sent recently. Please give time to the recipient to respond");
                notification.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING);
                notification.setDuration(3000);
            }
            dialog.close();
        });
        dialog.getFooter().add(btnSendRequest);
        Button btnCancel = new Button("Cancel", ed -> dialog.close());
        btnCancel.setId("btnCancel");
        dialog.getFooter().add(btnCancel);
        dialog.open();
    }

    public void sendFolderAssignmentNotif(Folder folder, List<String> sharedWith, OidcUser oidcUser, UserService userService, NotificationService notifService) {
        List<com.azrul.kelichap.domain.Notification> notifs = new ArrayList<>();

        for (String username : sharedWith) {
            var user = userService.getUser(username);//allUsers.get(username);
            com.azrul.kelichap.domain.Notification notif = new com.azrul.kelichap.domain.Notification();
            notif.setDateTime(LocalDateTime.now());
            notif.setFromFullName(oidcUser.getGivenName() + " " + oidcUser.getFamilyName());
            notif.setFromUserName(oidcUser.getPreferredUsername());
            notif.setItem(folder);
            notif.setStatus(NotificationStatus.NEW);
            notif.setToFullName(user.getFirstName() + " " + user.getLastName());
            notif.setToUserName(username);
            notif.setMessage("Folder name:" + folder.getName());
            notif.setType(NotificationType.FOLDER_ASSIGNMENT);
            notifs.add(notif);
        }
        notifService.saveAll(notifs);
    }

    public Boolean sendNotifRequestAccess(Folder folder, String toUserName, FolderRight requestedRight, OidcUser oidcUser, NotificationService notifService) {

        long notifCount = notifService.countByItemToUserFromUserStatusAndRequestedRight(
                folder,
                toUserName,
                oidcUser.getPreferredUsername(),
                NotificationStatus.NEW,
                requestedRight);

        if (notifCount > 0) {
            return false; //access request to this folder, asking for this right from the same user , to the same user and is still unread iis consiidered duplicate
        } else {
            //UserRepresentation user = allUsers.get(toUserName);
            com.azrul.kelichap.domain.Notification notif = new com.azrul.kelichap.domain.Notification();
            notif.setDateTime(LocalDateTime.now());
            notif.setFromFullName(oidcUser.getGivenName() + " " + oidcUser.getFamilyName());
            notif.setFromUserName(oidcUser.getPreferredUsername());
            notif.setItem(folder);
            notif.setRequestedRight(requestedRight);
            notif.setStatus(NotificationStatus.NEW);
            //notif.setToFullName(user.getFirstName() + " " + user.getLastName());
            notif.setToUserName(toUserName);
            notif.setMessage("Folder name:" + folder.getName());
            notif.setType(NotificationType.ACCESS_REQUEST);
            notifService.save(notif);
            return true;
        }
    }

    public void sendFileUploadNotif(DocumentData doc, List<String> sharedWith, OidcUser oidcUser, NotificationService notifService) {
        List<com.azrul.kelichap.domain.Notification> notifs = new ArrayList<>();

        for (String username : sharedWith) {
            //var user = allUsers.get(username);
            com.azrul.kelichap.domain.Notification notif = new com.azrul.kelichap.domain.Notification();
            notif.setDateTime(LocalDateTime.now());
            notif.setFromFullName(oidcUser.getGivenName() + " " + oidcUser.getFamilyName());
            notif.setFromUserName(oidcUser.getPreferredUsername());
            notif.setItem(doc);
            notif.setStatus(NotificationStatus.NEW);
            //notif.setToFullName(user.getFirstName() + " " + user.getLastName());
            notif.setToUserName(username);
            notif.setMessage("File name: " + doc.getName());
            if (doc.getVersion() > 1) {
                notif.setType(NotificationType.UPLOAD_NEW_VERSION);
            } else {
                notif.setType(NotificationType.UPLOAD_NEW);
            }
            notifs.add(notif);
        }
        notifService.saveAll(notifs);
    }

    private void calculateEnabilityOfFolderConfigDialog(
            Folder folder,
            Boolean folderIsActive,
            TextField nameField,
            HorizontalLayout userPanel,
            Checkbox cbIsActive,
            Button btnSubmit,
            OidcUser oidcUser) {
        Boolean isAdmin = isCurrentUserAdmin(oidcUser);

        if (folder.getAccessMap().contains(new FolderAccess(oidcUser.getPreferredUsername(), FolderRight.CREATE_UPDATE_FOLDER))) {
            if (folderIsActive == true) {
                nameField.setReadOnly(false);
                userPanel.setEnabled(true);
                cbIsActive.setEnabled(false);
                btnSubmit.setEnabled(true);
            } else {
                nameField.setReadOnly(true);
                userPanel.setEnabled(false);
                cbIsActive.setEnabled(false);
                btnSubmit.setEnabled(false);
            }
        } else if (folder.getAccessMap().contains(new FolderAccess(
                oidcUser.getPreferredUsername(),
                FolderRight.READ_FOLDER))) {
            nameField.setReadOnly(true);
            userPanel.setEnabled(false);
            cbIsActive.setEnabled(false);
            btnSubmit.setEnabled(false);

        } else if (folder.getAccessMap().contains(new FolderAccess(
                oidcUser.getPreferredUsername(),
                FolderRight.FOLDER_OWNER))) {
            if (folderIsActive == true) {
                nameField.setReadOnly(false);
                userPanel.setEnabled(true);
                if (folder.isRoot()) {
                    cbIsActive.setEnabled(true);
                } else {
                    cbIsActive.setEnabled(false);
                }
                btnSubmit.setEnabled(true);
            } else {
                nameField.setReadOnly(true);
                userPanel.setEnabled(false);
                if (folder.isRoot()) {
                    cbIsActive.setEnabled(true);
                } else {
                    cbIsActive.setEnabled(false);
                }
                btnSubmit.setEnabled(true);
            }
        } else {
            nameField.setReadOnly(true);
            userPanel.setEnabled(false);
            cbIsActive.setEnabled(false);
            btnSubmit.setEnabled(false);
        }
        if (isAdmin) {
            if (folderIsActive == true) {
                nameField.setReadOnly(true);
                userPanel.setEnabled(true);
                if (folder.isRoot()) {
                    cbIsActive.setEnabled(true);
                } else {
                    cbIsActive.setEnabled(false);
                }
                btnSubmit.setEnabled(true);
            } else {
                nameField.setReadOnly(true);
                userPanel.setEnabled(false);
                if (folder.isRoot()) {
                    cbIsActive.setEnabled(true);
                } else {
                    cbIsActive.setEnabled(false);
                }
                btnSubmit.setEnabled(true);
            }
        }
    }

}
