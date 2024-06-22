/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.common;

import com.azrul.kelichap.domain.User;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author azrul
 */
public class UserField extends TextField {

    private final String BETWEEN_BRACKETS = "\\((.*?)\\)";
    private User user;
    private Boolean addProfileButton;
    private Button btnShowProfile = new Button();
    private Registration clickListener = null;
    private Avatar avUser = new Avatar();

    public UserField(User user, Boolean addProfileButton) {
        this.addProfileButton = addProfileButton;
        this.setSuffixComponent(btnShowProfile);
        this.setPrefixComponent(avUser);
        btnShowProfile.setIcon(VaadinIcon.USER_CARD.create());
        btnShowProfile.addThemeVariants(ButtonVariant.LUMO_SMALL);
        setUser(user);

    }

    public UserField(User user) {
        this.addProfileButton = false;
        setUser(user);

    }

    public User getUser() {
        return user;
    }

    public void reset() {
        setUser(null);
    }

    public final void setUser(User user_) {

        this.user = user_;
        if (user == null) {
            this.setValue("--");
            btnShowProfile.setEnabled(false);
            avUser.setVisible(false);
        } else {

            String userDisplayName = user.getUserDispalyName();
            avUser.setVisible(true);
            this.setValue(userDisplayName);
            avUser.setName(userDisplayName.replaceAll(BETWEEN_BRACKETS, ""));
            avUser.addThemeVariants(AvatarVariant.LUMO_XSMALL);
            int index = Math.abs(userDisplayName.hashCode() % 7 + 1);
            avUser.setColorIndex(index);

            if (addProfileButton) {
                UserProfileDialog up = new UserProfileDialog(user);
                btnShowProfile.setEnabled(true);
                if (clickListener!=null){
                    clickListener.remove();
                }
                clickListener = btnShowProfile.addClickListener(e -> up.open());
            }
        }

        this.setReadOnly(true);
    }

}
