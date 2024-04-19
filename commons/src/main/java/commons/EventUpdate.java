package commons;

public class EventUpdate {
    public enum Action {
        ADDED_USER, REMOVED_USER, UPDATED_USER,
        ADDED_EXPENSE, REMOVED_EXPENSE, REMOVED_ALL_EXPENSES, UPDATED_EXPENSE,
        UPDATED_EVENT, DELETED_EVENT
    }
    private Action action;
    private Long id;

    @SuppressWarnings("unused")
    public EventUpdate() {
        // for object mappers
    }

    /**
     * Create a new EventUpdate for the websocket to return
     * @param action The action that was performed in the update
     * @param id The id of the updated object
     */
    public EventUpdate(Action action, Long id) {
        if (action == Action.UPDATED_EVENT || action == Action.DELETED_EVENT || action == Action.REMOVED_ALL_EXPENSES) {
            throw new RuntimeException("Invalid event update");
        }
        this.action = action;
        this.id = id;
    }

    /**
     * Create a new EventUpdate for the websocket to return
     * @param action The action that was performed in the update
     */
    public EventUpdate(Action action) {
        if (!(action == Action.UPDATED_EVENT || action == Action.DELETED_EVENT || action == Action.REMOVED_ALL_EXPENSES)) {
            throw new RuntimeException("Invalid event update");
        }
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public Long getObjectId() {
        return id;
    }

    public void setObjectId(Long id) {
        this.id = id;
    }
}
