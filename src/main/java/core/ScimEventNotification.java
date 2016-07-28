package core;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xmauritz on 7/26/16.
 */
public class ScimEventNotification {
    private static final List<String> SCHEMAS = new ArrayList<String>() {{ add("urn:ietf:params:scim:schemas:notify:2.0:Event"); }};
    private List<String> feedUris;
    private String publisherUri;
    private List<String> resourceUris;
    private ScimEventTypeEnum type;
    private List<String> attributes;
    private JSONObject values;

    // Constructor
    public ScimEventNotification() {
        feedUris = new ArrayList<String>();
        resourceUris = new ArrayList<String>();
        attributes = new ArrayList<String>();
        values = new JSONObject();
    }

    public List<String> getFeedUris() {
        return Collections.unmodifiableList(feedUris);
    }

    public void addToFeedUris(String feedUri) {
        this.feedUris.add(feedUri);
    }

    public String getPublisherUri() {
        return publisherUri;
    }

    public void setPublisherUri(String publisherUri) {
        this.publisherUri = publisherUri;
    }

    public List<String> getResourceUris() {
        return Collections.unmodifiableList(resourceUris);
    }

    public void addToResourceUris(String resourceUri) {
        this.resourceUris.add(resourceUri);
    }

    public ScimEventTypeEnum getType() {
        return type;
    }

    public void setType(ScimEventTypeEnum type) {
        this.type = type;
    }

    public List<String> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void addToAttributes(String attribute) {
        this.attributes.add(attribute);
    }

    public JSONObject getValues() {
        return values;
    }

    public void setValues(JSONObject values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScimEventNotification that = (ScimEventNotification) o;

        if (feedUris != null ? !feedUris.equals(that.feedUris) : that.feedUris != null) return false;
        if (publisherUri != null ? !publisherUri.equals(that.publisherUri) : that.publisherUri != null) return false;
        if (resourceUris != null ? !resourceUris.equals(that.resourceUris) : that.resourceUris != null) return false;
        if (type != that.type) return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        return values != null ? values.equals(that.values) : that.values == null;

    }

    @Override
    public int hashCode() {
        int result = feedUris != null ? feedUris.hashCode() : 0;
        result = 31 * result + (publisherUri != null ? publisherUri.hashCode() : 0);
        result = 31 * result + (resourceUris != null ? resourceUris.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (values != null ? values.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScimEventNotification{" +
                "SCHEMAS=" + SCHEMAS +
                ", feedUris=" + feedUris +
                ", publisherUri='" + publisherUri + '\'' +
                ", resourceUris=" + resourceUris +
                ", type=" + type +
                ", attributes=" + attributes +
                ", values=" + values +
                '}';
    }

    public String toJson() {
        JSONObject sen = new JSONObject();

        sen.put("schemas", SCHEMAS);
        sen.put("feedUris", feedUris);
        sen.put("publisherUri", publisherUri);
        sen.put("resourceUri", resourceUris);
        sen.put("type", type.name());
        sen.put("attributes", attributes);
        sen.put("values", values);

        return sen.toString();
    }
}