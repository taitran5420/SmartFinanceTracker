package com.seap.smartfinancetracker.notification.controller;

import com.seap.smartfinancetracker.notification.entity.Notification;
import com.seap.smartfinancetracker.notification.enums.NotificationType;
import com.seap.smartfinancetracker.notification.repository.NotificationRepository;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserPrincipalMapper userPrincipalMapper;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        // Create Test User
        testUser = Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create();
        testUser = userRepository.save(testUser);

        // Create JWT Test Token
        UserPrincipal userPrincipal = userPrincipalMapper.toUserPrincipal(testUser);
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }
    //</editor-fold>

    //<editor-fold desc="GET /notifications/subscribe">
    @Test
    @DisplayName("Should successfully establish an SSE connection")
    void shouldSubscribeToSseSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/notifications/subscribe")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    @DisplayName("Should fail to establish SSE connection without authentication")
    void shouldFailToSubscribe_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/notifications/subscribe"))
                .andExpect(status().isForbidden());
    }
    //</editor-fold>

    //<editor-fold desc="GET /notifications/unread">
    @Test
    @DisplayName("Should fetch unread notifications and mark them as read in DB")
    void shouldGetUnreadNotifications_AndMarkAsRead() throws Exception {
        // Arrange
        Notification unreadNotif1 = Instancio.of(Notification.class)
                .ignore(field(Notification::getId))
                .set(field(Notification::getUser), testUser)
                .set(field(Notification::isRead), false)
                .set(field(Notification::getNotificationType), NotificationType.SYSTEM_UPDATE)
                .create();

        Notification unreadNotif2 = Instancio.of(Notification.class)
                .ignore(field(Notification::getId))
                .set(field(Notification::getUser), testUser)
                .set(field(Notification::isRead), false)
                .set(field(Notification::getNotificationType), NotificationType.BUDGET_WARNING)
                .create();

        Notification readNotif = Instancio.of(Notification.class)
                .ignore(field(Notification::getId))
                .set(field(Notification::getUser), testUser)
                .set(field(Notification::isRead), true)
                .set(field(Notification::getNotificationType), NotificationType.TRANSACTION_SUCCESS)
                .create();

        notificationRepository.saveAll(List.of(unreadNotif1, unreadNotif2, readNotif));

        // Act & Assert (HTTP Response)
        mockMvc.perform(get("/notifications/unread")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].isRead").value(true))
                .andExpect(jsonPath("$[1].isRead").value(true));

        // Verify Database State:
        List<Notification> allNotificationsAfterApiCall = notificationRepository.findAll();
        for (Notification notification : allNotificationsAfterApiCall) {
            assertTrue(notification.isRead(), "All notifications for the user should now be marked as read in the database.");
        }
    }
    //</editor-fold>
}