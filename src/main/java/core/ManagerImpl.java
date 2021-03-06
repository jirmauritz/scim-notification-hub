package core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.FeedDao;
import dao.SubscriberDao;
import dao.SubscriptionDao;
import rest.RestClientController;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manager controls the notification hub and keeps all the feeds.
 *
 * @author Jiri Mauritz
 */
@Named
@Singleton
public class ManagerImpl implements Manager {

    @Inject
    private SubscriberDao subscriberDao;

    @Inject
    private FeedDao feedDao;

    @Inject
    private SubscriptionDao subscriptionDao;

    // feed uri mapped on the feed object
    private Map<String, Feed> feeds = new HashMap<String, Feed>();

    // subscriber identificator mapped on the subscriber object
    private Map<String, Subscriber> subscribers = new HashMap<String, Subscriber>();


    @Override
    public void newMessage(String json) {
        if (json == null) throw new IllegalArgumentException("Json cannot be null.");
        // String -> JSON
        ObjectMapper mapper = new ObjectMapper();
        ScimEventNotification sen;
        try {
            sen = mapper.readValue(json, ScimEventNotification.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON.", e);
        }
        // update feeds
        feedDao.updateIdentifiers(feeds);
        // classify the sen into right feeds
        Set<String> urisToBeNotified = new HashSet<>();
        for (String feedUri : sen.getFeedUris()) {
            Feed feed = feeds.get(feedUri);
            if (feed == null) {
                // create new feed
                feed = new Feed(feedUri);
                feeds.put(feedUri, feed);
                feedDao.create(feed);
            } else {
                feedDao.update(feed);
            }
            Set<Subscriber> subscribersToBeNotified = new HashSet<>();
            subscribersToBeNotified.addAll(feed.newMsg(sen));
            for (Subscriber subscriber : subscribersToBeNotified) {
                for (Subscription subscription : subscriber.getSubscriptions()) {
                    if (subscription.getFeedUri().equals(feed.getUri())) {
                        urisToBeNotified.add(subscription.getEventUri());
                    }
                }
            }
            feedDao.storeState(feed);
        }
        webCallbackSend(urisToBeNotified, sen);
    }

    @Override
    public void newSubscription(String subscriberId, String feedUri, SubscriptionModeEnum mode, String eventUri) {
        if (subscriberId == null) throw new NullPointerException("SubscriberId cannot be null.");
        if (feedUri == null) throw new NullPointerException("FeedUri cannot be null.");
        if (mode == null) throw new NullPointerException("Mode cannot be null.");
        if (eventUri == null) throw new NullPointerException("EventUri cannot be null.");

        // update subscribers
        subscriberDao.update(subscribers);

        // get subscriber
        Subscriber subscriber = subscribers.get(subscriberId);
        if (subscriber == null) {
            // create new subscriber
            subscriber = new Subscriber(subscriberId);
            subscribers.put(subscriberId, subscriber);
            subscriberDao.create(subscriber);
        }

        // create subscription
        Subscription subscription = new Subscription(feedUri, mode, eventUri);
        subscriber.addSubscription(subscription);

        // add to feed
        feedDao.updateIdentifiers(feeds);
        Feed feed = feeds.get(feedUri);
        if (feed == null) {
            // create new feed
            feed = new Feed(feedUri);
            feeds.put(feedUri, feed);
            feedDao.create(feed);
        } else {
            feedDao.update(feed);
        }
        feed.addSubscriber(subscriber);
        feedDao.storeState(feed);
    }

    @Override
    public boolean removeSubscription(String subscriberIdentifier, String feedUri) {
        if (subscriberIdentifier == null) throw new NullPointerException("SubscriberId cannot be null.");
        if (feedUri == null) throw new NullPointerException("FeedUri cannot be null.");
        subscriberDao.update(subscribers);
        if (!subscribers.containsKey(subscriberIdentifier)) return false;
        Subscriber subscriber = subscribers.get(subscriberIdentifier);
        if (subscriber.removeSubscription(feedUri)) {

            // update feed
            feedDao.updateIdentifiers(feeds);
            Feed feed = feeds.get(feedUri);
            feedDao.update(feed);

            // remove subscriber from the feed
            feed.removeSubscriber(subscriber);
            subscriptionDao.remove(subscriberIdentifier, feedUri);

            // remove the subscriber if he has no subscriptions
            if (subscriber.getSubscriptions().isEmpty()) {
                subscribers.remove(subscriber.getIdentifier());
                subscriberDao.remove(subscriber);
            }

            // remove the feed if nobody is subscribed
            if (feed.getSubscribers().isEmpty()) {
                feeds.remove(feed.getUri());
                feedDao.remove(feed);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeSubscriber(String subscriberId) {
        if (subscriberId == null) throw new NullPointerException("SubscriberId cannot be null.");
        subscriberDao.update(subscribers);
        feedDao.updateIdentifiers(feeds);
        if (!subscribers.containsKey(subscriberId)) return false;
        Subscriber subscriber = subscribers.get(subscriberId);
        for (Subscription subscription : subscriber.getSubscriptions()) {

            // update feed
            Feed feed = feeds.get(subscription.getFeedUri());
            feedDao.update(feed);

            // remove the subscriber form the feed
            feed.removeSubscriber(subscriber);

            // remove the feed if nobody is subscribed
            if (feed.getSubscribers().isEmpty()) {
                feeds.remove(feed.getUri());
                feedDao.remove(feed);
            } else {
                feedDao.storeState(feed);
            }
        }
        subscribers.remove(subscriber.getIdentifier());
        subscriberDao.remove(subscriber);
        return true;
    }

    @Override
    public Set<ScimEventNotification> poll(String subscriberIdentifier) {
        if (subscriberIdentifier == null) throw new IllegalArgumentException("Subscriber's identifier cannot be null.");
        subscriberDao.update(subscribers);
        feedDao.updateIdentifiers(feeds);
        if (!subscribers.containsKey(subscriberIdentifier)) {
            throw new IllegalArgumentException("Subscriber with identifier " + subscriberIdentifier + " does not exists.");
        }

        // get subscriber
        Subscriber subscriber = subscribers.get(subscriberIdentifier);
        Set<ScimEventNotification> msgsToSend = new HashSet<>();
        // retrieve all msgs for each feed
        for (Subscription subscription : subscriber.getSubscriptions()) {
            if (subscription.getMode().equals(SubscriptionModeEnum.poll)) {
                Feed feed = feeds.get(subscription.getFeedUri());
                feedDao.update(feed);
                msgsToSend.addAll(feed.poll(subscriber));
                feedDao.storeState(feed);
            }
        }
        return msgsToSend;
    }

    /**
     * Call REST layer to inform the subscribers about the scim event.
     *
     * @param eventUris to be informed about scim event
     * @param sen       which defines the scim event
     */
    public void webCallbackSend(Set<String> eventUris, ScimEventNotification sen) {
        RestClientController.webCallback(eventUris, sen);
    }

    @Override
    public Set<String> getSubscriberIdentifiers() {
        return subscribers.keySet();
    }

    @Override
    public Subscriber getSubscriberByIdentifier(String identifier) {
        return subscribers.get(identifier);
    }
}
