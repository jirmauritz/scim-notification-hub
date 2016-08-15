package dao;

import core.Feed;
import core.ScimEventNotification;
import core.Subscriber;

import java.util.Map;
import java.util.Set;

/**
 * Created by xmauritz on 8/8/16.
 */
public interface SubscriberDao {

    /**
     * Update the subscriber's attributes according to the data storage.
     *
     * @param subscriber subscriber to be updated
     */
    public void update(Map<String, Subscriber> subscribers);

    /**
     * Create a new subscriber.
     *
     * @param subscriber to be created
     */
    public void create(Subscriber subscriber);

    /**
     * Remove a new subscriber.
     *
     * @param subscriber to be removed
     */
    public void remove(Subscriber subscriber);

    /**
     * Retrieve all poll subscribers and their last seen message for the specified feed.
     *
     * @param feed where the subscribers are subscribed
     * @return poll subscribers and their last seen message for feed
     */
    public Map<Subscriber, ScimEventNotification> getPollSubscribers(Feed feed);

    /**
     * Retrieve all webCallback subscribers for the specified feed.
     *
     * @param feed where the subscribers are subscribed
     * @return all webCallback subscribers for feed
     */
    public Set<Subscriber> getWebCallbackSubscribers(Feed feed);
}
