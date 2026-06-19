package com.seap.smartfinancetracker.notification.repository;

import com.seap.smartfinancetracker.notification.entity.Notification;
import com.seap.smartfinancetracker.user.entity.User;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;
    //</editor-fold>

    //<editor-fold desc="Test findByUserIdAndIsReadFalseOrderByCreatedAtDesc">
    @Test
    @DisplayName("Should return only unread notifications for a specific user, ordered by creation date descending")
    void findByUserIdAndIsReadFalse_ShouldReturnUnreadAndOrdered() throws InterruptedException {
        // Arrange: Setup users
        User targetUser = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        User otherUser = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());

        Notification n1 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), targetUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        n1 = entityManager.persistAndFlush(n1);

        Thread.sleep(10);

        Notification n2 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), targetUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        n2 = entityManager.persistAndFlush(n2);

        Notification n3 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), targetUser)
                .set(Select.field(Notification::isRead), true)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        entityManager.persistAndFlush(n3);

        Notification n4 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), otherUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        entityManager.persistAndFlush(n4);

        // Act
        List<Notification> result = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(targetUser.getId());

        // Assert
        assertEquals(2, result.size(), "Should only fetch exactly 2 unread notifications belonging to the target user");
        assertEquals(n2.getId(), result.get(0).getId(), "The newest notification must be at the top of the list (Descending order)");
        assertEquals(n1.getId(), result.get(1).getId(), "The oldest notification must be at the bottom");
    }
    //</editor-fold>

    //<editor-fold desc="Test markAllAsReadByUserId">
    @Test
    @DisplayName("Should perform a bulk JPQL update to mark all unread notifications as read for a given user")
    void markAllAsReadByUserId_ShouldUpdateIsReadFlagSuccessfully() {
        // Arrange: Setup users
        User targetUser = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        User otherUser = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());

        Notification n1 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), targetUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        n1 = entityManager.persistAndFlush(n1);

        Notification n2 = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), targetUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        n2 = entityManager.persistAndFlush(n2);

        Notification otherUserNotif = Instancio.of(Notification.class)
                .set(Select.field(Notification::getUser), otherUser)
                .set(Select.field(Notification::isRead), false)
                .ignore(Select.field(Notification::getId))
                .ignore(Select.field(Notification::getCreatedAt))
                .ignore(Select.field(Notification::getUpdatedAt))
                .create();
        otherUserNotif = entityManager.persistAndFlush(otherUserNotif);

        // Act: Execute the modifying bulk query
        notificationRepository.markAllAsReadByUserId(targetUser.getId());

        entityManager.clear();

        // Assert: Retrieve fresh records from DB
        Notification updatedN1 = entityManager.find(Notification.class, n1.getId());
        Notification updatedN2 = entityManager.find(Notification.class, n2.getId());
        Notification nonUpdatedNotif = entityManager.find(Notification.class, otherUserNotif.getId());

        assert updatedN1 != null;
        assert updatedN2 != null;
        assert nonUpdatedNotif != null;
        assertTrue(updatedN1.isRead(), "Target user's first notification should now be marked as read");
        assertTrue(updatedN2.isRead(), "Target user's second notification should now be marked as read");
        assertFalse(nonUpdatedNotif.isRead(), "Other user's notification must NOT be affected by the bulk update");
    }
    //</editor-fold>
}