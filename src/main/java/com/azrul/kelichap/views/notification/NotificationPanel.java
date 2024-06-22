
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.views.notification;

import com.azrul.kelichap.domain.Notification;
import com.azrul.kelichap.domain.NotificationStatus;
import com.azrul.kelichap.domain.NotificationType;
import com.azrul.kelichap.service.NotificationService;
import com.azrul.kelichap.views.mydocuments.MyDocumentsView;
import com.azrul.kelichap.views.common.PageNav;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

/**
 *
 * @author azrul
 */
public class NotificationPanel extends VerticalLayout {

    public String username;
    public PageNav pageNav;
    public MyDocumentsView myDocsView;
    public NotificationService notifService;
    public final Integer COUNT_PER_PAGE = 3;
    public final Integer MSG_LENGTH = 50;
    public final ContextMenu notifMenu;

    public NotificationPanel(
            String username,
            NotificationService notifService,
            ContextMenu notifMenu,
            Map<NotificationType, String> introText,
            Map<NotificationType, String> actionButtonText,
            Consumer<Notification> consGoThere
    ) {
        this.username = username;
        this.notifService = notifService;
        //this.pageNav = new PageNav();
        this.notifMenu = notifMenu;
        //this.myDocsView = myDocsView;

        Integer notifCount = this.notifService.countForUser(username);
        Grid<Notification> gridNotifs = new Grid<>();
        DataProvider dataProvider = buildNotifDataProvider(username, notifCount);
        gridNotifs.setDataProvider(dataProvider);

        this.pageNav = new PageNav();
        pageNav.init(dataProvider, notifCount, COUNT_PER_PAGE,"id", false);

        gridNotifs.addComponentColumn(n -> {
            return new NotificationCard(
                    n,
                    n.getItem() != null,
                    dataProvider,
                    notifService,
                    introText.get(n.getType()),
                    actionButtonText.get(n.getType()),
                    consGoThere,
                    n2 -> {
                        this.notifService.delete(n2);
                    },
                    n2 -> {
                        this.notifService.mark(n2, NotificationStatus.READ);
                    },
                    n2 -> {
                        this.notifService.mark(n2, NotificationStatus.NEW);
                    });
        });
        gridNotifs.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
        gridNotifs.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        this.add(pageNav);
        this.add(gridNotifs);
    }

    private DataProvider buildNotifDataProvider(String username, Integer notifCount) {
        //build data provider
        var dp = new AbstractBackEndDataProvider<Notification, Void>() {
            @Override
            protected Stream<Notification> fetchFromBackEnd(Query<Notification, Void> query) {
                //QuerySortOrder so = query.getSortOrders().isEmpty() ? null : query.getSortOrders().get(0);
                query.getPage();

                Page<Notification> notifs = notifService.getForUser(
                        username,
                        pageNav.getPage() - 1,
                        pageNav.getMaxCountPerPage(),
                        //                        "id",//so == null ? "id" : so.getSorted(),
                        Sort.by(Sort.Direction.DESC, "id"));
                return notifs.stream();
            }

            @Override
            protected int sizeInBackEnd(Query<Notification, Void> query) {

                return pageNav.getDataCountPerPage();
            }

            @Override
            public String getId(Notification item) {

                return item.getId().toString();
            }

        };
        return dp;
    }

    class NotificationCard extends VerticalLayout {

        public NotificationCard(
                Notification notif,
                Boolean itemFound,
                DataProvider dataProvider,
                NotificationService notifService,
                String introText,
                String actionButtonText,
                Consumer<Notification> consGoThere,
                Consumer<Notification> consDismiss,
                Consumer<Notification> consMarkAsRead,
                Consumer<Notification> consMarkAsUnread
        ) {
            HorizontalLayout nameLine = new HorizontalLayout();
            HorizontalLayout dateLine = new HorizontalLayout();
            HorizontalLayout messageLine = new HorizontalLayout();

            NativeLabel lblName = new NativeLabel(notif.getFromFullName());
            lblName.getStyle().set("fontWeight", "bold");
            this.getStyle().set("padding", "3px");
            //this.getStyle().set("border","1px solid red");

            Span badgeNew = new Span("New");
            badgeNew.getElement().getThemeList().add("badge pill");
            if (notif.getStatus().equals(NotificationStatus.NEW)) {
                badgeNew.setVisible(true);
            } else {
                badgeNew.setVisible(false);
            }

            NativeLabel lblMsgId = notif.getMessageId() != null ? new NativeLabel("[#" + notif.getMessageId() + "] ") : new NativeLabel("");
            lblMsgId.setId("lblMsgId");
            nameLine.add(lblMsgId, new Text(" "), lblName, new Text(" " + introText + ":"));
            dateLine.add(badgeNew, new Text(notif.getDateTime().format(DateTimeFormatter.ofPattern("dd-MMM-yyy hh:mm:ss"))));
            if (notif.getMessage() != null) {
                if (notif.getMessage().length() < MSG_LENGTH) {
                    messageLine.add(new Text(notif.getMessage()));
                } else {
                    messageLine.add(new Text(StringUtils.substring(notif.getMessage(), 0, MSG_LENGTH) + "..."));
                }
                messageLine.getStyle().set("text-wrap", "balance");
            }

            HorizontalLayout warningLine = new HorizontalLayout();
            HorizontalLayout buttonLine = new HorizontalLayout();
            if (itemFound == true) {
                Button btnGoThere = new Button(actionButtonText, e -> {
                    notifMenu.close();
                    consGoThere.accept(notif);
                    consMarkAsRead.accept(notif);
                    badgeNew.setVisible(false);
                });
                btnGoThere.setId("btnGoThere");
                btnGoThere.addThemeVariants(ButtonVariant.LUMO_SMALL);
                btnGoThere.getStyle().set("padding", "3px");
                btnGoThere.getStyle().set("margin", "0px");
                buttonLine.add(btnGoThere);
            } else {
                Icon warning = VaadinIcon.WARNING.create();
                warningLine.add(new HorizontalLayout(warning, new Text(" Cannot find this document. Perhaps it was deleted")));
            }
            Button btnDismiss = new Button("Dismiss", e -> {
                //notifMenu.close();
                consDismiss.accept(notif);
                pageNav.refreshPageNav(notifService.countForUser(username));

                dataProvider.refreshAll();
            });
            btnDismiss.getStyle().set("padding", "3px");
            btnDismiss.getStyle().set("margin", "0px");
            btnDismiss.addThemeVariants(ButtonVariant.LUMO_SMALL);
            buttonLine.add(btnDismiss);

            Button btnMarkUnread = new Button("Mark as unread");
            btnMarkUnread.addThemeVariants(ButtonVariant.LUMO_SMALL);
            btnMarkUnread.getStyle().set("padding", "3px");
            btnMarkUnread.getStyle().set("margin", "0px");
            buttonLine.add(btnMarkUnread);

            Button btnMarkRead = new Button("Mark as read");
            btnMarkRead.addThemeVariants(ButtonVariant.LUMO_SMALL);
            btnMarkRead.getStyle().set("padding", "3px");
            btnMarkRead.getStyle().set("margin", "0px");
            buttonLine.add(btnMarkRead);

            btnMarkUnread.addClickListener(e -> {
                consMarkAsUnread.accept(notif);
                badgeNew.setVisible(true);
                btnMarkUnread.setVisible(false);
                btnMarkRead.setVisible(true);
            });

            btnMarkRead.addClickListener(e -> {
                consMarkAsRead.accept(notif);
                badgeNew.setVisible(false);
                btnMarkUnread.setVisible(true);
                btnMarkRead.setVisible(false);
            });

            if (notif.getStatus() == NotificationStatus.READ) {
                btnMarkUnread.setVisible(true);
                btnMarkRead.setVisible(false);
            } else {
                btnMarkUnread.setVisible(false);
                btnMarkRead.setVisible(true);
            }

            buttonLine.add(btnDismiss);
            this.add(nameLine, dateLine, messageLine, warningLine, buttonLine);

        }
    }
}
