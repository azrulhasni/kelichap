/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.common;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.dto.basic.SearchDataDTO;
import com.azrul.kelichap.dto.reqresp.SearchDocumentsResponseDTO;
import com.azrul.kelichap.service.DocumentSearchService;
import com.azrul.kelichap.service.DocumentService;
import com.azrul.kelichap.service.UserService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 *
 * @author azrul
 */
public class SearchPanel extends VerticalLayout{
    private final String SEARCH_ORDER = "docId:desc,page:asc";
    
    public SearchPanel(){
        
    }
    
    public void buildSearchPanel(
            FolderPanel folderPanel,
            OidcUser oidcUser,
            DocumentService docService,
            DocumentSearchService searchService,
            BiConsumer<DocumentData,Integer> showDocumentViewer,
            BiConsumer<DocumentData,Long> showDocumentViewerFromNote) {
        //final VerticalLayout mainPanel = new VerticalLayout();
        final VerticalLayout searchResultPanel = new VerticalLayout();

        final HorizontalLayout searchPanel = new HorizontalLayout();
        final ComboBox<String> ddPages = new ComboBox<>();
        final TextField searchField = new TextField();
        final VerticalLayout txSearchScope = new VerticalLayout();
        searchResultPanel.setClassName(".searchResultPanel");
        ddPages.setPlaceholder("Pages");
        ddPages.setAllowCustomValue(false);
        ddPages.addValueChangeListener(e -> {
            searchDocumentsAndShow(ddPages,
                    txSearchScope,
                    searchField,
                    searchResultPanel,
                    folderPanel, 
                    oidcUser, 
                    docService, 
                    searchService,
                    showDocumentViewer,
                    showDocumentViewerFromNote);
        });

        searchField.addKeyPressListener(Key.ENTER, e -> {
            SearchDocumentsResponseDTO searchDTO = searchDocumentsAndShow(ddPages,
                    txSearchScope,
                    searchField,
                    searchResultPanel,
                    folderPanel, 
                    oidcUser, 
                    docService, 
                    searchService,
                    showDocumentViewer,
                    showDocumentViewerFromNote);
            setSearchPages(searchDTO, ddPages);
        });
        searchField.setPrefixComponent(LumoIcon.SEARCH.create());
        searchField.setPlaceholder("Search documents");
        searchField.setWidthFull();

        searchPanel.setWidthFull();
        searchPanel.add(searchField);
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        searchField.addValueChangeListener(e -> {
            if (!e.getOldValue().equals(e.getValue())) {
                if (!ddPages.isEmpty()) {
                    ddPages.setValue("1");
                }
            }
        });
        searchPanel.add(ddPages);
        ddPages.setEnabled(false);
        Button btnSearch = new Button("Search", (ClickEvent<Button> t) -> {
            SearchDocumentsResponseDTO searchDTO = searchDocumentsAndShow(
                    ddPages,
                    txSearchScope,
                    searchField,
                    searchResultPanel,
                    folderPanel, 
                    oidcUser, 
                    docService, 
                    searchService,
                    showDocumentViewer,
                    showDocumentViewerFromNote);
            setSearchPages(searchDTO, ddPages);
        });

        btnSearch.setId("btnSearch");
        searchPanel.add(btnSearch);
        this.add(txSearchScope);
        this.add(searchPanel);
        this.add(searchResultPanel);
    }

    private void setSearchPages(SearchDocumentsResponseDTO searchDTO, ComboBox<String> ddPages) {
        List<String> pageList = new ArrayList<>();
        for (Integer i = 1; i <= searchDTO.getTotalPage(); i++) {
            pageList.add(i.toString());
        }
        ddPages.setItems(pageList);
    }
    
    private SearchDocumentsResponseDTO searchDocumentsAndShow(
            ComboBox<String> ddPages,
            VerticalLayout txSearchScope,
            TextField searchField,
            VerticalLayout searchResultPanel,
            FolderPanel folderPanel,
            OidcUser oidcUser,
            DocumentService docService,
            DocumentSearchService searchService,
            BiConsumer<DocumentData,Integer> showDocumentViewer,
            BiConsumer<DocumentData,Long> showDocumentViewerFromNote) {
        Integer page = null;
        Boolean isAdmin = oidcUser.getAuthorities().stream().anyMatch(
                sga -> StringUtils.equals(sga.getAuthority(), "ROLE_KELICHAP_ADMIN"));

        
        if (StringUtils.isNumeric(ddPages.getValue())) {
            page = Integer.valueOf(ddPages.getValue());
        } else {
            page = 1;
        }

        txSearchScope.removeAll();
        if (!folderPanel.getSelectedItems().isEmpty()) {
            txSearchScope.add(new Text("Searching in:"));

            for (var item : folderPanel.getSelectedItems()) {
                txSearchScope.add(new Text("- " + item.getName()));
            }
        } else {
            txSearchScope.add(new Text("Searching in all accessible documents"));
        }

        ddPages.setEnabled(true);
        String query = searchField.getValue();
        SearchDocumentsResponseDTO searchDTO = searchService.search(
                query,
                folderPanel.getSelectedItems(),
                docService.getFolders(Set.of(oidcUser.getPreferredUsername()), folderPanel.isActiveOnly()),
                page,
                3,
                SEARCH_ORDER,
                folderPanel.isActiveOnly(),
                Set.of(oidcUser.getPreferredUsername()),
                true, //search also in downstream folders
                isAdmin
        );

        List<HorizontalLayout> cards = searchDTO
                .getSearchResults()
                .stream()
                .map(searchData -> buildSearchResultCard(searchData, folderPanel, oidcUser,docService,showDocumentViewer,showDocumentViewerFromNote ))
                .collect(Collectors.toList());

        searchResultPanel.removeAll();
        for (var card : cards) {
            searchResultPanel.add(card);
        }

        return searchDTO;
        //ddPages.setValue(page.toString());

    }
    
    private HorizontalLayout buildSearchResultCard(
            SearchDataDTO searchData,
            FolderPanel folderPanel,
            OidcUser oidcUser,
            DocumentService docService,
            BiConsumer<DocumentData,Integer> showDocumentViewer,
            BiConsumer<DocumentData,Long> showDocumentViewerFromNote
    ) {
        HorizontalLayout card = new HorizontalLayout();
        card.addClassName("card");
        card.setSpacing(false);
        card.getThemeList().add("spacing-s");

        VerticalLayout description = new VerticalLayout();
        description.addClassName("description");
        description.setSpacing(false);
        description.setPadding(false);

        List<Component> descriptionComponents = new ArrayList<>();
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setSpacing(false);
        header.getThemeList().add("spacing-s");

        Span name = new Span(searchData.getFileName());
        name.addClassName("name");
        Span folderName = new Span(searchData.getFolderName());
        folderName.addClassName("name");

        Span inactive = new Span("Inactive");
        inactive.getElement().getThemeList().add("badge contrast small primary");

        if (StringUtils.equals(searchData.getType(), "DOCUMENT")) {
            Span workflowId = new Span("[" + searchData.getWorkflowId() != null ? searchData.getWorkflowId() : "" + "]");
            workflowId.addClassName("name");

            Span docBadge = new Span("Document");
            docBadge.getElement().getThemeList().add("badge small primary");
            if (searchData.getActive()) {
                header.add(name, folderName, workflowId, docBadge);
            } else {
                header.add(name, folderName, workflowId, docBadge, inactive);
            }
            descriptionComponents.add(header);
        } else if (StringUtils.equals(searchData.getType(), "NOTE")) {

            Span noteBadge = new Span("Note");
            noteBadge.getElement().getThemeList().add("badge small primary");
            if (searchData.getActive()) {
                header.add(name, folderName, noteBadge);
            } else {
                header.add(name, folderName, noteBadge, inactive);
            }
            descriptionComponents.add(header);
        } else {
            Span folderBadge = new Span("Folder");
            folderBadge.getElement().getThemeList().add("badge small primary");
            if (searchData.getActive()) {
                header.add(name, folderName, folderBadge);
            } else {
                header.add(name, folderName, folderBadge, inactive);
            }
            descriptionComponents.add(header);
        }

        searchData.getSnippets().stream().forEach(snippet -> {
            Span post = new Span();
            //post.setText(snippet);
            post.addClassName("post");
            post.add(new Html("<label>" + snippet + "</label>"));
            descriptionComponents.add(post);
        });

        if (!StringUtils.equals(searchData.getType(), "FOLDER")) {
            HorizontalLayout pagePanel = new HorizontalLayout();
            Icon pageIcon = VaadinIcon.BOOKMARK_O.create();
            pageIcon.setSize("14px");
            pagePanel.addClassName("actions");
            pagePanel.setSpacing(false);
            pagePanel.getThemeList().add("spacing-s");
            pagePanel.add(pageIcon, new Span("Page:" + searchData.getPage()));
            descriptionComponents.add(pagePanel);

            Icon tagsIcon = VaadinIcon.TAGS.create();
            tagsIcon.setSize("14px");
            tagsIcon.addClassName("icon");
            HorizontalLayout tagsPanel = new HorizontalLayout();
            tagsPanel.addClassName("actions");
            tagsPanel.setSpacing(false);
            tagsPanel.getThemeList().add("spacing-s");
            Span tags = new Span(StringUtils.join(searchData.getTags(), ","));
            tagsPanel.add(tagsIcon, tags);
            descriptionComponents.add(tagsPanel);
        }

        HorizontalLayout buttonPanel = new HorizontalLayout();
        Button btnView = new Button("View document");
        btnView.addClickListener(e -> {
            docService.getDocById(
                    searchData.getDocId(),
                    Set.of(oidcUser.getPreferredUsername()),
                    folderPanel.isActiveOnly()
            ).ifPresentOrElse(doc -> {
                if (StringUtils.equals(searchData.getType(), "DOCUMENT")) {
                    showDocumentViewer.accept(
                            doc,
                            searchData.getPage()
                    );
                } else {
                    showDocumentViewerFromNote.accept(
                            doc,
                            searchData.getNoteId()
                    );
                }

            }, () -> {
                Notification notification = Notification.show("File does not exist (it could have been deleted)");
                notification.setDuration(2000);
                notification.open();
            });
        });
        btnView.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button btnOpenFolder = new Button("Open folder");
        btnOpenFolder.addClickListener(e -> {
            if (StringUtils.equals("FOLDER", searchData.getType())) {
                folderPanel.openFromFolderToRootFolder(searchData.getFolderId(), oidcUser, docService);
            } else { //Note and DocumentData
                folderPanel.openFromDocToRootFolder(searchData.getDocId(), oidcUser, docService);
            }
        });
        btnOpenFolder.addThemeVariants(ButtonVariant.LUMO_SMALL);
        if (!StringUtils.equals("FOLDER", searchData.getType())) {
            buttonPanel.add(btnView);
        }
        buttonPanel.add(btnOpenFolder);
        descriptionComponents.add(buttonPanel);

        description.add(descriptionComponents);
        card.add(/*image, */description);
        return card;
    }
    
}
