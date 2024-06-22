/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.common;

import com.azrul.kelichap.domain.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 *
 * @author azrul
 */
public class UserProfile extends FormLayout {
    public UserProfile(User user){
        //FormLayout this = new FormLayout();
        this.getStyle().set("width","100%");
         UserField username = new UserField(user);
         username.setReadOnly(true);
         username.getStyle().set("width", "100%");
         TextField email = new TextField();
         email.setValue(user.getEmail());
         email.setReadOnly(true);
         email.getStyle().set("width", "100%");
         TextField firstName = new TextField();
         firstName.setReadOnly(true);
         firstName.getStyle().set("width", "100%");
         firstName.setValue(user.getFirstName());
         TextField lastName = new TextField();
         lastName.setReadOnly(true);
         lastName.getStyle().set("width", "100%");
         lastName.setValue(user.getLastName());
         this.addFormItem(username,"Username");
         this.addFormItem(firstName,"First name");
         this.addFormItem(lastName,"Last name");
         this.addFormItem(email,"Email");
         this.setColspan(username, 2);
         this.setColspan(firstName, 2);
         this.setColspan(lastName, 2);
         this.setColspan(email, 2);
         this.setResponsiveSteps(new ResponsiveStep("0", 1));
         
    }
}
