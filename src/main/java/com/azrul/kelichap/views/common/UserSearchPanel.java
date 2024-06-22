/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.common;

import com.azrul.kelichap.domain.User;
import com.azrul.kelichap.service.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 * @author azrul
 */
public class UserSearchPanel extends VerticalLayout{
    private final String BETWEEN_BRACKETS = "\\((.*?)\\)";
    private MultiSelectListBox<User> lbSearchResults;
    private Set<String> filteredOutUsers = new HashSet<>();
    
    public UserSearchPanel(UserService userService, Set<String> filteredOutUsers_, boolean selectMultiple){
        //VerticalLayout allNotSelectedUsersPanel = new VerticalLayout();
        this.filteredOutUsers = filteredOutUsers_;
        this.add(new Text("All users (Please select to share folder to)"));
        
        lbSearchResults = new MultiSelectListBox<>();
        
        lbSearchResults.setRenderer(new ComponentRenderer<>(
                user -> {
                    String name = user.getFirstName()
                    + " " + user.getLastName() + " (" + user.getUsername() + ")";
                    Avatar avUser = new Avatar(name.replaceAll(BETWEEN_BRACKETS, ""));
                    avUser.addThemeVariants(AvatarVariant.LUMO_XSMALL);
                    int index = Math.abs(name.hashCode() % 7 + 1);
                    avUser.setColorIndex(index);
                    Text txName = new Text(name);
                    HorizontalLayout divUser = new HorizontalLayout();
                    divUser.add(avUser, txName);
                    return divUser;
                }
        ));
        lbSearchResults.getStyle().set("border", "2px solid lightgray");
        lbSearchResults.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        lbSearchResults.setWidth("100%");
        lbSearchResults.setHeight("11em");
        lbSearchResults.setId("listBoxAllUsers");
        lbSearchResults.addSelectionListener(e->{
            if (selectMultiple==false){
            Set<User> oldSelection = e.getOldSelection();
            Set<User> newSelection = e.getAddedSelection();
            lbSearchResults.deselect(oldSelection);
            if (!newSelection.isEmpty()){
                lbSearchResults.select(newSelection.iterator().next());
            }
            }
        });
        TextField tfUserQuery = new TextField();
        tfUserQuery.setClearButtonVisible(true);
        tfUserQuery.setId("textFieldUserQuery");
        tfUserQuery.setValueChangeMode(ValueChangeMode.TIMEOUT);
        tfUserQuery.setWidth("100%");
        tfUserQuery.setPlaceholder("Search user");
        tfUserQuery.setPrefixComponent(LumoIcon.SEARCH.create());
        tfUserQuery.addKeyDownListener(e -> {
            if (Key.ARROW_DOWN.equals(e.getKey())) {
                Collection<User> usersNotSelected = ((ListDataProvider<User>) lbSearchResults.getDataProvider()).getItems();
                if (!usersNotSelected.isEmpty()) {
                    //lbNotSelectedUsers.select(usersNotSelected.iterator().next());
                    UI.getCurrent().getPage().executeJs("document.getElementById('listBoxAllUsers').focus();");
                }
            }
        });
        tfUserQuery.addKeyPressListener(e -> {
            Set<User> searchedUsers = userService.searchUsers(tfUserQuery.getValue(), filteredOutUsers);
            lbSearchResults.setItems(searchedUsers);
        });
        this.add(tfUserQuery, lbSearchResults);
    }
    
    public Set<User> getSelectedItems(){
        return lbSearchResults.getSelectedItems();
    }
    
    public void filterOutSelectedItems(){
        Set<User> selectedUsers = lbSearchResults.getSelectedItems();
        ((ListDataProvider<User>)lbSearchResults.getDataProvider()).getItems().removeAll(selectedUsers);
        filteredOutUsers.addAll(selectedUsers.stream().map(u->u.getUsername()).collect(Collectors.toSet()));
        lbSearchResults.getDataProvider().refreshAll();
        lbSearchResults.clear();
        lbSearchResults.deselectAll();
    }
    
    public void addItems(Set<User> users){
        filteredOutUsers.removeAll(users.stream().map(u->u.getUsername()).collect(Collectors.toSet()));
        ((ListDataProvider<User>)lbSearchResults.getDataProvider()).getItems().addAll(users);
        lbSearchResults.getDataProvider().refreshAll();
        lbSearchResults.clear();
        lbSearchResults.deselectAll();
    }
    
//    public Set<User> getCurrentSearchResults(){
//        Set<User> users = new HashSet<>();
//        users.addAll(((ListDataProvider<User>) lbSearchResults.getDataProvider()).getItems());
//        return users;
//    }
//    
//    public void setCurrentSearchResult(Set<User> notSelectedUsersFiltered){
//        lbSearchResults.setItems(notSelectedUsersFiltered);
//        
//    }
//    
//    public void setFilteredOutUsers(Set<String> filteredOutUsers){
//        this.filteredOutUsers = filteredOutUsers;
//    }
//    
//    public void clearSelection(){
//        lbSearchResults.clear();
//    }
//    
//    public void deselectAll(){
//        lbSearchResults.deselectAll();
//    }
}
