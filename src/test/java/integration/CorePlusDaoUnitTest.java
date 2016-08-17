package integration;

import core.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Test of the Manager class.
 *
 * @author Jiri Mauritz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CorePlusDaoUnitTest {

    @Inject
    private DataSource dataSource;

    @Inject
    @Spy
    private ManagerImpl manager;

    private static final String[] FILE_NAMES = new String[]{"sen1.json", "sen2.json", "sen3.json"};
    private static final String FEED1 = "https://perun.cesnet.cz/scim-notification/storage-fi.ics.muni.cz/mailman";
    private static final String FEED2 = "https://perun.cesnet.cz/scim-notification/storage-fss.ics.muni.cz/openvpn";
    private static final String SBSC1_ID = "id";
    private static final String SBSC2_ID = "id2";

    private List<String> sens;

    @Before
    public void setUp() throws Exception {
        // load sen objects from files
        sens = new ArrayList<String>();
        for (String fileName : FILE_NAMES) {
            List<String> jsonLines = Files.readAllLines(Paths.get(ClassLoader.getSystemResource(fileName).toURI()));
            sens.add(String.join("\n", jsonLines));
        }

        // set up mocks
        MockitoAnnotations.initMocks(this);

        // load db tables
        Resource create = new ClassPathResource("sql/createTablesDerby.sql");
        ScriptUtils.executeSqlScript(dataSource.getConnection(), create);
    }

    @After
    public void tearDown() throws Exception {
        Resource drop = new ClassPathResource("sql/dropTablesDerby.sql");
        ScriptUtils.executeSqlScript(dataSource.getConnection(), drop);
    }

    @Test
    public void webCallbackEmpty() throws Exception {
        manager.newMessage(sens.get(0));

        // verify
        ObjectMapper mapper = new ObjectMapper();
        ScimEventNotification sen = mapper.readValue(sens.get(0), ScimEventNotification.class);
        verify(manager).webCallbackSend(eq(new HashSet<Subscriber>()), eq(sen));
    }

    @Test
    public void webCallbackOneMsg() throws Exception {
        Subscriber sbcs1 = new Subscriber(SBSC1_ID);
        sbcs1.addSubscription(new Subscription(FEED1, SubscriptionModeEnum.webCallback, FEED1));
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.webCallback, FEED1);
        manager.newMessage(sens.get(0));

        // verify
        ObjectMapper mapper = new ObjectMapper();
        ScimEventNotification sen = mapper.readValue(sens.get(0), ScimEventNotification.class);
        verify(manager).webCallbackSend(new HashSet<Subscriber>(Arrays.asList(sbcs1)), sen);
    }

    @Test
    public void webCallbackComplicated() throws Exception {
        Subscriber sbcs1 = new Subscriber(SBSC1_ID);
        sbcs1.addSubscription(new Subscription(FEED1, SubscriptionModeEnum.webCallback, FEED1));
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.webCallback, FEED1);
        Subscriber sbcs2 = new Subscriber(SBSC2_ID);
        sbcs2.addSubscription(new Subscription(FEED2, SubscriptionModeEnum.webCallback, FEED2));
        manager.newSubscription(SBSC2_ID, FEED2, SubscriptionModeEnum.webCallback, FEED2);
        manager.newMessage(sens.get(0));

        // construct sens
        ObjectMapper mapper = new ObjectMapper();
        ScimEventNotification sen1 = mapper.readValue(sens.get(0), ScimEventNotification.class);
        ScimEventNotification sen2 = mapper.readValue(sens.get(1), ScimEventNotification.class);

        // verify
        verify(manager).webCallbackSend(new HashSet<Subscriber>(Arrays.asList(sbcs1)), sen1);

        // new message
        manager.newMessage(sens.get(1));

        // verify
        verify(manager).webCallbackSend(new HashSet<Subscriber>(Arrays.asList(sbcs1, sbcs2)), sen2);
    }

    @Test
    public void pollSimple() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newMessage(sens.get(0));
        Set<ScimEventNotification> toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1);
    }

    @Test
    public void pollTwoSubscriptions() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC1_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newMessage(sens.get(0));
        manager.newMessage(sens.get(1));
        Set<ScimEventNotification> toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1, FEED2);
    }

    @Test
    public void pollTwoSubscribers() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC2_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newMessage(sens.get(0));

        // check first message
        Set<ScimEventNotification> toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1);

        manager.newMessage(sens.get(1));

        // check two messages were returned to SBSC2
        toSend = manager.poll(SBSC2_ID);
        checkSens(toSend, FEED1, FEED1);

        // check second msg was returned to SBSC1
        toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1);

        // check no more msgs are returned
        toSend = manager.poll(SBSC2_ID);
        checkSens(toSend);
    }

    @Test
    public void pollComplicated() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC1_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newSubscription(SBSC2_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC2_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newMessage(sens.get(0));
        manager.newMessage(sens.get(1));

        // first subscriber
        Set<ScimEventNotification> toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1, FEED2);

        manager.newMessage(sens.get(2));

        // check three messages were returned to SBSC2
        toSend = manager.poll(SBSC2_ID);
        checkSens(toSend, FEED1, FEED1, FEED2);

        // check third msg was returned to SBSC1
        toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED2);

        // check no more msgs are returned
        toSend = manager.poll(SBSC1_ID);
        checkSens(toSend);
        toSend = manager.poll(SBSC2_ID);
        checkSens(toSend);
    }

    @Test
    public void removeSubscription() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC1_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newMessage(sens.get(0));
        manager.newMessage(sens.get(2));

        // remove subscription
        assertTrue(manager.removeSubscription(SBSC1_ID, FEED2));

        // check only msg for feed1 is returned
        Set<ScimEventNotification> toSend = manager.poll(SBSC1_ID);
        checkSens(toSend, FEED1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeSubscriber() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC1_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newSubscription(SBSC2_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        manager.newSubscription(SBSC2_ID, FEED2, SubscriptionModeEnum.poll, FEED2);
        manager.newMessage(sens.get(0));
        manager.newMessage(sens.get(1));

        // remove
        manager.removeSubscriber(SBSC1_ID);

        // second subscriber
        Set<ScimEventNotification> toSend = manager.poll(SBSC2_ID);
        checkSens(toSend, FEED1, FEED2);

        // poll from unsubscribed subscriber
        manager.poll(SBSC1_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidJson() throws Exception {
        manager.newMessage("invalid json (}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validJsonButNotSen() throws Exception {
        manager.newMessage("{\"valid json\"}");
    }

    @Test(expected = NullPointerException.class)
    public void addSubscriptionNullFeedUri() throws Exception {
        manager.newSubscription(SBSC1_ID, null, SubscriptionModeEnum.poll, FEED1);
    }

    @Test(expected = NullPointerException.class)
    public void addSubscriptionNullMode() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, null, FEED1);
    }

    @Test(expected = NullPointerException.class)
    public void addSubscriptionNullEventUri() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, null);
    }

    @Test(expected = NullPointerException.class)
    public void addSubscriptionNullSubscriber() throws Exception {
        manager.newSubscription(null, FEED1, SubscriptionModeEnum.poll, FEED1);
    }

    @Test(expected = NullPointerException.class)
    public void removeSubscriptionNullSubscriber() throws Exception {
        manager.removeSubscription(null, FEED1);
    }

    @Test(expected = NullPointerException.class)
    public void removeSubscriptionNullFeed() throws Exception {
        manager.removeSubscription(SBSC1_ID, null);
    }

    @Test
    public void removeSubscriptionForNonExistingSubscriber() throws Exception {
        assertFalse(manager.removeSubscription(SBSC1_ID, FEED1));
    }

    @Test
    public void removeNonExistingSubscription() throws Exception {
        manager.newSubscription(SBSC1_ID, FEED1, SubscriptionModeEnum.poll, FEED1);
        assertFalse(manager.removeSubscription(SBSC1_ID, FEED2));
    }

    @Test(expected = NullPointerException.class)
    public void removeNullSubscriber() throws Exception {
        manager.removeSubscriber(null);
    }

    @Test
    public void removeNonExistingSubscriber() throws Exception {
        assertFalse(manager.removeSubscriber(SBSC1_ID));
    }

    private void checkSens(Set<ScimEventNotification> sens, String ... feedUrisArray) {
        assertEquals(feedUrisArray.length, sens.size());
        Set<String> feedUris = new HashSet<String>(Arrays.asList(feedUrisArray));
        Iterator<ScimEventNotification> iter = sens.iterator();
        Set<String> returnedFeedUris = new HashSet<String>();
        while (iter.hasNext()) {
            returnedFeedUris.addAll(iter.next().getFeedUris());
        }
        assertTrue(returnedFeedUris.containsAll(feedUris));
    }
}