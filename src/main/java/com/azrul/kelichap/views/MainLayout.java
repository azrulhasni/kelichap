package com.azrul.kelichap.views;

import com.azrul.kelichap.autocomplete.Autocomplete;
import com.azrul.kelichap.service.NotificationService;
import com.azrul.kelichap.views.about.AboutView;
import com.azrul.kelichap.views.admin.AdminView;
import com.azrul.kelichap.views.mydocuments.MyDocumentsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vaadin.lineawesome.LineAwesomeIcon;

/**
 * The main view is a top-level placeholder for other views.
 */
@Uses(Autocomplete.class)
public class MainLayout extends AppLayout {

    private H2 viewTitle;
//    private final AccessAnnotationChecker accessChecker;
//    private final NotificationService notifService;
    private final String BETWEEN_BRACKETS = "\\((.*?)\\)";

    public MainLayout( //            @Autowired AccessAnnotationChecker accessChecker,
            //            @Autowired NotificationService notifService
            ) {

//        this.accessChecker = accessChecker;
//        this.notifService = notifService;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken token
                && token.getPrincipal() instanceof DefaultOidcUser oidcUser) {
            
            setPrimarySection(Section.DRAWER);
            addDrawerContent(oidcUser);
            addHeaderContent();

            this.setDrawerOpened(false);
        }
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        addToNavbar(true, toggle, viewTitle);

    }

    private void addDrawerContent(OidcUser oidcUser) {
       // oidcUser.
        H1 appName = new H1("Kelichap");
        Image img = new Image("images/kelichap.svg", "Kelichap EDMS");
        img.getStyle().set("width", "32px");
        img.getStyle().set("height", "32px");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(img, appName);

        Scroller scroller = new Scroller(createNavigation(oidcUser));
        addToDrawer(header, scroller, createFooter(oidcUser));
    }

    private SideNav createNavigation(OidcUser oidcUser) {
        SideNav nav = new SideNav();
        if (oidcUser.getAuthorities().stream().anyMatch(
                sga->StringUtils.equals(sga.getAuthority(),"ROLE_KELICHAP_USER"))){
            nav.addItem(new SideNavItem(
                    "My Documents", MyDocumentsView.class, LineAwesomeIcon.LIST_SOLID.create()));
        }else if (oidcUser.getAuthorities().stream().anyMatch(
                sga->StringUtils.equals(sga.getAuthority(),"ROLE_KELICHAP_ADMIN"))){
            nav.addItem(new SideNavItem(
                    "Admin", AdminView.class, LineAwesomeIcon.USER_COG_SOLID.create()));
        }
        nav.addItem(new SideNavItem("About", AboutView.class, LineAwesomeIcon.FILE.create()));

        return nav;
    }

    private Footer createFooter(OidcUser oidcUser) {
        Footer layout = new Footer();
        if (oidcUser != null) {

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            MenuItem userName = userMenu.addItem("");
            Div div = new Div();

            String name = oidcUser.getGivenName() + " " + oidcUser.getFamilyName() + " (" + oidcUser.getPreferredUsername() + ")";
            Avatar avUser = new Avatar(name.replaceAll(BETWEEN_BRACKETS, ""));
            int index = Math.abs(name.hashCode() % 7 + 1);
            avUser.setColorIndex(index);
            avUser.addThemeVariants(AvatarVariant.LUMO_XSMALL);
            HorizontalLayout userPane = new HorizontalLayout();
            Div divName = new Div(name);
            divName.getStyle().set("display", "flex");
            divName.getStyle().set("align-items", "center");

            userPane.add(avUser, divName);
            div.add(userPane);
            div.add(new Icon("lumo", "dropdown"));
            div.getElement().getStyle().set("display", "flex");
            div.getElement().getStyle().set("align-items", "center");
            div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
            userName.add(div);
            userName.getSubMenu().addItem("Sign out", e -> {
                UI.getCurrent().getPage().setLocation("/logout");
            });

            layout.add(userMenu);
        } else {
            Button login = new Button("Sign in",
                    event -> UI.getCurrent().getPage().setLocation("/feed"));
            layout.add(login);
        }

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

}
