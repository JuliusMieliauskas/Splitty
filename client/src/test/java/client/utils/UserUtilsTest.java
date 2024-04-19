package client.utils;

import client.MockConfigUtils;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.core.GenericType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class UserUtilsTest {
    @Test
    public void testGetAllUsers() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/users", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<User>>() { }, List.of(mockUser));

            assertEquals(1, UserUtils.getAllUsers().size());
            assertEquals(12L, UserUtils.getAllUsers().getFirst().getId());
        }
    }

    @Test
    public void testCreateOrGetEmptyUserName() {
        assertThrows(FailedRequestException.class, () -> {
            UserUtils.createOrGet("");
        });
    }

    /**
     * Assumes EventUtils.getUsersOfEvent is working
     */
    @Test
    public void testCreateOrGetAlreadyInEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class);
             MockedStatic<EventUtils> eventUtils = Mockito.mockStatic(EventUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            eventUtils.when(() -> EventUtils.getUsersOfEvent(1L)).thenReturn(List.of(mockUser));

            assertThrows(FailedRequestException.class, () -> {
                UserUtils.createOrGet("coolname");
            });
        }
    }

    /**
     * Assumes createUser is working
     */
    @Test
    public void testCreateOrGetUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class);
             MockedStatic<EventUtils> eventUtils = Mockito.mockStatic(EventUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            User mockRes = Mockito.mock(User.class);
            eventUtils.when(() -> EventUtils.getUsersOfEvent(1L)).thenReturn(List.of(mockUser));
            MockConfigUtils.mockEndPoint("api/users", MockConfigUtils.APIMethod.POST, User.class, mockRes);

            assertEquals(mockRes, UserUtils.createOrGet("epicname"));

            MockConfigUtils.verifyRequest("api/users", MockConfigUtils.APIMethod.POST);
        }
    }

    @Test
    public void testRenameUserEmptyUsername() {
        assertThrows(FailedRequestException.class, () -> {
            UserUtils.renameUser(1L, "");
        });
    }

    @Test
    public void testRenameUserUserNameAlreadyUsed() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class);
             MockedStatic<EventUtils> eventUtils = Mockito.mockStatic(EventUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            User mockUser2 = Mockito.mock(User.class);
            when(mockUser2.getId()).thenReturn(13L);
            when(mockUser2.getUsername()).thenReturn("epicname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            eventUtils.when(() -> EventUtils.getUsersOfEvent(1L)).thenReturn(List.of(mockUser));

            assertThrows(FailedRequestException.class, () -> {
                UserUtils.renameUser(13L, "coolname");
            });
        }
    }

    /**
     * Assumes getUserById is working
     * Assumes updateUser is working
     */
    @Test
    public void testRenameUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class);
             MockedStatic<EventUtils> eventUtils = Mockito.mockStatic(EventUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            User mockUser2 = Mockito.mock(User.class);
            when(mockUser2.getId()).thenReturn(13L);
            when(mockUser2.getUsername()).thenReturn("epicname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();
            MockConfigUtils.mockEndPoint("api/users/13", MockConfigUtils.APIMethod.GET, mockUser2, User.class, mockUser2);
            MockConfigUtils.mockEndPoint("api/users/13", MockConfigUtils.APIMethod.PUT, mockUser2, User.class, mockUser2);

            eventUtils.when(() -> EventUtils.getUsersOfEvent(1L)).thenReturn(List.of(mockUser));

            UserUtils.renameUser(13L, "epicnewname");

            MockConfigUtils.verifyRequest("api/users/13", MockConfigUtils.APIMethod.GET);
            verify(mockUser2).setUsername("epicnewname");
            MockConfigUtils.verifyRequest("api/users/13", MockConfigUtils.APIMethod.PUT, mockUser2);
        }
    }

    @Test
    public void testGetUserById() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users/12", MockConfigUtils.APIMethod.GET, User.class, mockUser);

            assertEquals(mockUser, UserUtils.getUserById(12));

            MockConfigUtils.verifyRequest("api/users/12", MockConfigUtils.APIMethod.GET);

        }
    }

    @Test
    public void testGetUserByIdFailed() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users/12", MockConfigUtils.APIMethod.GET, User.class, mockUser);
            MockConfigUtils.mockFailedEndPoint("api/users/13", MockConfigUtils.APIMethod.GET, "No user with id 13");

            assertThrows(FailedRequestException.class, () -> {
                UserUtils.getUserById(13);
            });

            MockConfigUtils.verifyRequest("api/users/13", MockConfigUtils.APIMethod.GET);

        }
    }

    @Test
    public void testGetUserByUserName() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/events/1/username/coolname", MockConfigUtils.APIMethod.GET, User.class, mockUser);

            assertEquals(mockUser, UserUtils.getUserByName("coolname"));

            MockConfigUtils.verifyRequest("api/events/1/username/coolname", MockConfigUtils.APIMethod.GET);

        }
    }

    @Test
    public void testGetUserByUserNameFailed() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/events/1/username/coolname", MockConfigUtils.APIMethod.GET, User.class, mockUser);
            MockConfigUtils.mockFailedEndPoint("api/events/1/username/epicname",
                    MockConfigUtils.APIMethod.GET, "No user with name epicname");

            assertThrows(FailedRequestException.class, () -> {
                UserUtils.getUserByName("epicname");
            });

            MockConfigUtils.verifyRequest("api/events/1/username/epicname", MockConfigUtils.APIMethod.GET);

        }
    }

    @Test
    public void testCreateUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users", MockConfigUtils.APIMethod.POST, mockUser, User.class, mockUser);

            assertEquals(mockUser, UserUtils.createUser(mockUser));

            MockConfigUtils.verifyRequest("api/users", MockConfigUtils.APIMethod.POST, mockUser);

        }
    }

    @Test
    public void testDeleteUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users/12", MockConfigUtils.APIMethod.DELETE);

            UserUtils.deleteUser(mockUser);

            MockConfigUtils.verifyRequest("api/users/12", MockConfigUtils.APIMethod.DELETE);

        }
    }

    @Test
    public void testGetUserExpenses() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);
            when(mockUser.getUsername()).thenReturn("coolname");

            UserExpense mockUserExpense = Mockito.mock(UserExpense.class);
            when(mockUserExpense.getId()).thenReturn(14L);
            when(mockUserExpense.getDebtor()).thenReturn(mockUser);

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users/12/expenses", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<UserExpense>>() { }, List.of(mockUserExpense));

            assertEquals(List.of(mockUserExpense), UserUtils.getUserExpenses(mockUser));

            MockConfigUtils.verifyRequest("api/users/12/expenses", MockConfigUtils.APIMethod.GET);

        }
    }

    @Test
    public void testUpdateUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);

            User updatedMockUser = Mockito.mock(User.class);
            when(updatedMockUser.getId()).thenReturn(12L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockCurrentEvent();

            MockConfigUtils.mockEndPoint("api/users/12", MockConfigUtils.APIMethod.PUT,
                    mockUser, User.class, updatedMockUser);

            assertEquals(updatedMockUser, UserUtils.updateUser(12L, mockUser));

            MockConfigUtils.verifyRequest("api/users/12", MockConfigUtils.APIMethod.PUT, mockUser);
        }
    }

    @Test
    public void testVerifyAdminPassword() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            MockConfigUtils.init(config);

            MockConfigUtils.mockEndPoint("api/admin", MockConfigUtils.APIMethod.POST,
                    "pword", Boolean.class, Boolean.FALSE);

            assertEquals(false, UserUtils.verifyAdminPassword("pword"));

            MockConfigUtils.verifyRequest("api/admin", MockConfigUtils.APIMethod.POST, "pword");
        }
    }
}
