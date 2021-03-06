package core;

/**
 * Subscriptions are made by the subscriber as a request to the Notification Hub.
 * The subscriber is connected to specified feed to receive notifications.
 * The subscriptions are usually created by accessing "/Subscriptions" endpoint.
 *
 * @author Jiri Mauritz
 */
public class Subscription {
    private Long id;
    private String feedUri;
    private SubscriptionModeEnum mode;
    private String eventUri;
    //TODO: Jwt, pollInterval and state

    public Subscription(String feedUri, SubscriptionModeEnum mode, String eventUri) {
        this.id = null;
        this.feedUri = feedUri;
        this.mode = mode;
        this.eventUri = eventUri;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFeedUri() {
        return feedUri;
    }

    public SubscriptionModeEnum getMode() {
        return mode;
    }

    public String getEventUri() {
        return eventUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription that = (Subscription) o;

        if (!feedUri.equals(that.feedUri)) return false;
        if (mode != that.mode) return false;
        return eventUri.equals(that.eventUri);

    }

    @Override
    public int hashCode() {
        int result = feedUri.hashCode();
        result = 31 * result + mode.hashCode();
        result = 31 * result + eventUri.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "feedUri='" + feedUri + '\'' +
                ", mode=" + mode +
                ", eventUri='" + eventUri + '\'' +
                '}';
    }
}
