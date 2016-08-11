package dao;

import core.Feed;
import core.Subscriber;
import core.Subscription;

import java.util.Set;

/**
 * Created by xmauritz on 8/9/16.
 */
public interface SubscriptionDao {

    /**
     * Create new subscription to the specified feed and assign it to the specified subscriber.
     *
     * @param subscription to be created
     * @param subscriber   owner of the subscription
     * @param feed         to which the subscription belongs
     */
    public void create(Subscription subscription, Subscriber subscriber, Feed feed);

    /**
     * Remove a new subscription.
     *
     * @param subscriberIdentifier who owns the subscription
     * @param feedUri              of the subscription
     */
    public void remove(String subscriberIdentifier, String feedUri);
}