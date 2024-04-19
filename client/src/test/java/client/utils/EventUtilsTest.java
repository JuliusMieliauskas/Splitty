package client.utils;

import client.MockConfigUtils;
import commons.Event;
import commons.User;
import jakarta.ws.rs.core.GenericType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class EventUtilsTest {
    @Test
    public void testCreateEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(1L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/", MockConfigUtils.APIMethod.POST,
                    Event.class, mockEvent);

            assertEquals(mockEvent, EventUtils.createEvent(mockEvent));
            MockConfigUtils.verifyRequest("api/events/", MockConfigUtils.APIMethod.POST);
        }
    }

    @Test
    public void testJoinEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(1L);
            User mockUser = Mockito.mock(User.class);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/1/users", MockConfigUtils.APIMethod.POST);

            EventUtils.joinEvent(mockEvent, mockUser);
            MockConfigUtils.verifyRequest("api/events/1/users", MockConfigUtils.APIMethod.POST);
        }
    }

    @Test
    public void testGetEventsByUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(12L);

            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(1L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/users/12/events", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<Event>>() { }, List.of(mockEvent));

            assertEquals(List.of(mockEvent), EventUtils.getEvents(mockUser));
            MockConfigUtils.verifyRequest("api/users/12/events", MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testRemoveUser() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Event mockEvent = Mockito.mock(Event.class);
            User mockUser = Mockito.mock(User.class);
            when(mockEvent.getId()).thenReturn(1L);
            when(mockUser.getId()).thenReturn(12L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/1/users/12", MockConfigUtils.APIMethod.DELETE);

            EventUtils.removeUser(mockEvent, mockUser);
            MockConfigUtils.verifyRequest("api/events/1/users/12", MockConfigUtils.APIMethod.DELETE);
        }
    }

    @Test
    public void testGetEventByCode() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            String inviteCode = "INV123";
            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(1L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/invite-code/" + inviteCode, MockConfigUtils.APIMethod.GET, Event.class, mockEvent);

            assertEquals(mockEvent, EventUtils.getEventByCode(inviteCode));
            MockConfigUtils.verifyRequest("api/events/invite-code/" + inviteCode, MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testGetEventById() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long eventId = 1L;
            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(eventId);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/" + eventId, MockConfigUtils.APIMethod.GET, Event.class, mockEvent);

            assertEquals(mockEvent, EventUtils.getEventById(eventId));
            MockConfigUtils.verifyRequest("api/events/" + eventId, MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testUpdateEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Event mockEvent = Mockito.mock(Event.class);
            when(mockEvent.getId()).thenReturn(1L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/1", MockConfigUtils.APIMethod.PUT);

            EventUtils.updateEvent(mockEvent);
            MockConfigUtils.verifyRequest("api/events/1", MockConfigUtils.APIMethod.PUT);
        }
    }

    @Test
    public void testLeaveEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Event mockEvent = Mockito.mock(Event.class);
            User mockUser = Mockito.mock(User.class);
            when(mockEvent.getId()).thenReturn(1L);
            when(mockUser.getId()).thenReturn(12L);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/1/users/12", MockConfigUtils.APIMethod.DELETE);

            EventUtils.removeUser(mockEvent, mockUser);
            MockConfigUtils.verifyRequest("api/events/1/users/12", MockConfigUtils.APIMethod.DELETE);
        }
    }

    @Test
    public void testGetParticipants() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long eventId = 1L;
            Set<User> mockParticipants = Set.of(Mockito.mock(User.class));

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/1/users", MockConfigUtils.APIMethod.GET,
                    new GenericType<Set<User>>() { }, mockParticipants);

            assertEquals(mockParticipants, EventUtils.getParticipants(eventId));
            MockConfigUtils.verifyRequest("api/events/1/users", MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testGetUsersOfEvent() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long eventId = 1L;
            List<User> mockUsers = List.of(Mockito.mock(User.class));

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/" + eventId + "/users", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<User>>() { }, mockUsers);

            assertEquals(mockUsers, EventUtils.getUsersOfEvent(eventId));
            MockConfigUtils.verifyRequest("api/events/" + eventId + "/users", MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testGetJson() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long eventId = 1L;
            String jsonMock = "{\"id\":3}";

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/events/" + eventId + "/json", MockConfigUtils.APIMethod.GET, String.class, jsonMock);

            assertEquals(jsonMock, EventUtils.getJson(eventId));
            MockConfigUtils.verifyRequest("api/events/" + eventId + "/json", MockConfigUtils.APIMethod.GET);
        }
    }
}
