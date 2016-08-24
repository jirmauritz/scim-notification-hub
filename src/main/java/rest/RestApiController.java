package rest;


import core.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Rest controller of the manager class and all application.
 *
 * @author Jiri Mauritz
 */
@RestController
public class RestApiController {

    @Inject
    private Manager manager;

    private SecureRandom random = new SecureRandom();

    public static final String DOMAIN = "https://perun-dev.meta.zcu.cz/scim-notification-hub";
    public static final String WEB_CALLBACK = "urn:ietf:params:scimnotify:api:messages:2.0:webCallback";
    public static final String POLL = "urn:ietf:params:scimnotify:api:messages:2.0:poll";


    @RequestMapping(value = "/Subscriptions/{sbscId}", method = GET)
    public ResponseEntity<Subscription> getSubscription(@PathVariable("sbscId") String sbscId) {
        Subscriber subscriber = manager.getSubscriberByIdentifier(sbscId);
        Subscription subscription = subscriber.getSubscriptions().iterator().next();

        return new ResponseEntity<>(subscription, HttpStatus.OK);
    }

    /**
     * POST /Subscription
     * Create a new subscription.
     * The body of the request must follow the schema 'urn:ietf:params:scim:schemas:notify:2.0:Subscription'.
     *
     * @param body of the request containing the subscription
     * @return status 201 or 400 if the subscription json is not valid
     */
    @RequestMapping(value = "/Subscriptions", method = POST)
    public ResponseEntity<String> createSubscription(@RequestBody String body) {
        ObjectMapper mapper = new ObjectMapper();
        String sbscId;
        try {
            Map<String, Object> json = (Map<String, Object>) mapper.readValue(body, Map.class);
            String feedUri = (String) json.get("feedUri");
            String modeString = (String) json.get("mode");
            SubscriptionModeEnum mode;
            if (POLL.equals(modeString)) {
                mode = SubscriptionModeEnum.poll;
            } else if (WEB_CALLBACK.equals(modeString)) {
                mode = SubscriptionModeEnum.webCallback;
            } else {
                throw new IOException("Wrong subscription mode.");
            }
            String eventUri = (String) json.get("eventUri");

            // generate subscription id
            sbscId = nextSubscriptionId();

            manager.newSubscription(sbscId, feedUri, mode, eventUri);
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        String response = "Location: \n" + DOMAIN + "/Subscriptions/" + sbscId;
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * DELETE /Subscription/{identifier}
     * Remvove the subscription, which means remove also the subscriber.
     *
     * @param sbscId subscription / subscriber identifier
     * @return status 200 or 404 if not found
     */
    @RequestMapping(value = "/Subscriptions/{sbscId}", method = DELETE)
    public ResponseEntity<String> deleteSubscription(@PathVariable("sbscId") String sbscId) {
        try {
            boolean deleted = manager.removeSubscriber(sbscId);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * POST /Events
     * Creates a new scim event notification.
     *
     * @param senJson scim event notification according to the schema 'urn:ietf:params:scim:schemas:notify:2.0:Event'
     * @return status 204
     */
    @RequestMapping(value = "/Events", method = POST)
    public ResponseEntity<?> createSubscriber(@RequestBody String senJson) {
        try {
            manager.newMessage(senJson);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * GET /Poll/{identifier}
     * Perform poll of the messages for the specified subscription.
     *
     * @param sbscId subscription / subscriber identifier
     * @return status 200
     */
    @RequestMapping(value = "/Poll/{sbscId}", method = GET)
    public ResponseEntity<Set<ScimEventNotification>> poll(@PathVariable("sbscId") String sbscId) {
        Set<ScimEventNotification> msgs;
        try {
            msgs = manager.poll(sbscId);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // TODO: add exception message to the response
        }
        return new ResponseEntity<>(msgs, HttpStatus.OK);
    }

    private String nextSubscriptionId() {
        String identifier = new BigInteger(130, random).toString(25);
        Set<String> alreadyCreatedIdentifiers = manager.getSubscriberIdentifiers();
        while (alreadyCreatedIdentifiers.contains(identifier)) {
            identifier = new BigInteger(130, random).toString(25);
        }
        return identifier;
    }
}
