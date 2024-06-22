/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.landing;

import com.azrul.kelichap.views.MainLayout;
import com.azrul.kelichap.views.admin.AdminView;
import com.azrul.kelichap.views.mydocuments.MyDocumentsView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 *
 * @author azrul
 */
@PageTitle("Landing Page")
@Route(value = "", layout = MainLayout.class)
@RolesAllowed({"KELICHAP_USER", "KELICHAP_ADMIN"})
public class LandingPage extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken oauth2AuthToken) {
            OidcUser oidcUser = (DefaultOidcUser) oauth2AuthToken.getPrincipal();
             if (oidcUser.getAuthorities().stream().anyMatch(sga->StringUtils.equals(sga.getAuthority(),"ROLE_KELICHAP_ADMIN"))){
                 event.forwardTo(AdminView.class);
             }else{ //only "KELICHAP_USER", "KELICHAP_ADMIN" can access LandingPage. So if noot admin, definitely this is a user
                  event.forwardTo(MyDocumentsView.class);
             }
       
        }
    }

}
