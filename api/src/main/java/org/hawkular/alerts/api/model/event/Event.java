/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.api.model.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.trigger.Trigger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * An Alert is an Event.  For the most part an Event can be thought of as an Alert without life-cycle. Alerts are
 * always generated by a Trigger. Events may be generated by a Trigger or may be created directly via the API.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@ApiModel(description = "An Alert is an Event. + \n" +
        " + \n" +
        "For the most part an Event can be thought of as an Alert without life-cycle. + \n" +
        " + \n" +
        "Alerts are always generated by a Trigger. + \n" +
        " + \n" +
        "Events may be generated by a Trigger or may be created directly via the API. + \n")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        // set default impl for events injected via REST API
        defaultImpl = Event.class)
@JsonSubTypes({
        @Type(name = "EVENT", value = Event.class),
        @Type(name = "ALERT", value = Alert.class) })
public class Event implements Comparable<Event>, Serializable {

    private static final long serialVersionUID = 1L;

    // This field will be controlled vi fasterxml. We need to make this an explicit field because we ship the
    // serialized json to clients via rest. Since we serialize via fasterxml the field will be added to the json.
    // Using "visible=false" in JsonTypeInfo only works if we control the deserialization.
    @ApiModelProperty(value = "Indicate if this object is an EVENT or an ALERT. This is a read-only field controlled " +
            "by the system for serialization purposes.",
            position = 0,
            allowableValues = "EVENT, ALERT")
    @JsonInclude
    protected String eventType;

    @ApiModelProperty(value = "Tenant id owner of this event.",
            position = 1,
            allowableValues = "Tenant is overwritten from Hawkular-Tenant HTTP header parameter request")
    @JsonInclude
    protected String tenantId;

    @ApiModelProperty(value = "Unique identifier for this event.",
            position = 2,
            required = true)
    @JsonInclude
    protected String id;

    @ApiModelProperty(value = "Creation time for this event.",
            position = 3,
            required = true,
            allowableValues = "Timestamp in milliseconds.")
    @JsonInclude
    protected long ctime;

    // Optional dataSource of Event, used by EventCondition to evaluate events
    @ApiModelProperty(value = "Optional dataSource for Event. Used for <<Trigger>> in <<EventCondition>> " +
            "to evaluate events with triplet [tenantId, source, dataId] as unique identifier.",
            position = 4,
            allowableValues = "Timestamp in milliseconds.")
    @JsonInclude
    String dataSource;

    // Optional dataId of Event, used by EventCondition to evaluate events
    @ApiModelProperty(value = "Data identifier used for Events condition evaluation. Events must supply a valid dataId " +
            "to be considered for <<EventCondition>> evaluation. + \n" +
            "DataIds in an events context should incorporate the source of the event (for uniqueness). + \n" +
            "Events generated from a <<Trigger>> will have dataId set to the triggerId, therefore allowing chaining " +
            "with other <<EventCondition>>. ",
            position = 5,
            required = true)
    @JsonInclude(Include.NON_EMPTY)
    private String dataId;

    // category of Event, suitable for display, recommended to be, but not limited to, an EventCategory.name.
    @ApiModelProperty(value = "Category of Event. Suitable for display. + \n" +
            "Alerts will use ALERT category. + \n" +
            "Events generated from <<Trigger>> will use Trigger.eventCategory or TRIGGER category as default.",
            position = 6,
            required = true,
            allowableValues = "Any category defined by the user.",
            example = "ALERT or TRIGGER")
    @JsonInclude
    private String category;

    // A description of the event, suitable for display
    @ApiModelProperty(value = "Description of the event. Suitable for display. + \n" +
            "Events generated from <<Trigger>> will use Trigger.eventText or Trigger.description/Trigger.name as " +
            "default.",
            position = 7,
            required = true,
            allowableValues = "Any description defined by the user.")
    @JsonInclude
    private String text;

    @ApiModelProperty(value = "Properties defined by the user for this event. + \n " +
            "Events generated from <<Trigger>> will use Trigger.context. + \n" +
            "Context cannot be used as part of Event conditions expressions or criteria in finder methods.",
            position = 8)
    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> context;

    @ApiModelProperty(value = "Tags defined by the user for this event. + \n " +
            "Events generated from <<Trigger>> will use Trigger.tags. + \n" +
            "Tags can be used as part of Event conditions expressions and criteria in finder methods. + \n" +
            "Tag value cannot be null.",
            position = 9)
    @JsonInclude(Include.NON_EMPTY)
    protected Map<String, String> tags;

    // Null for API-generated Events. Otherwise the Trigger that created the event (@ctime)
    @ApiModelProperty(value = "Trigger that created the event. + \n " +
            "Null for API-generated Events.",
            position = 10)
    @JsonInclude(Include.NON_EMPTY)
    private Trigger trigger;

    // Null for API-generated Events
    // Otherwise the Dampening defined when the event was created (@ctime), null for default dampening.
    @ApiModelProperty(value = "Dampening defined when the event was created. + \n " +
            "Null for API-generated Events.",
            position = 11)
    @JsonInclude(Include.NON_EMPTY)
    @Thin
    private Dampening dampening;

    // Null for API-generated Events. Otherwise the Eval Sets that fired the trigger
    @ApiModelProperty(value = "The Eval Sets that fired the trigger. + \n " +
            "Null for API-generated Events.",
            position = 12)
    @JsonInclude(Include.NON_EMPTY)
    @Thin
    private List<Set<ConditionEval>> evalSets;

    public Event() {
        // for json assembly
        this(null, (String) null, null, null);
    }

    public Event(String tenantId, String id, String category, String text) {
        this(tenantId, id, System.currentTimeMillis(), null, null, category, text, null, null);
    }

    public Event(String tenantId, String id, String dataId, String category, String text) {
        this(tenantId, id, System.currentTimeMillis(), null, dataId, category, text, null, null);
    }

    public Event(String tenantId, String id, String dataSource, String dataId, String category, String text) {
        this(tenantId, id, System.currentTimeMillis(), dataSource, dataId, category, text, null, null);
    }

    public Event(String tenantId, String id, String category, String text, Map<String, String> context) {
        this(tenantId, id, System.currentTimeMillis(), null, null, category, text, context, null);
    }

    public Event(String tenantId, String id, String dataId, String category, String text,
            Map<String, String> context) {
        this(tenantId, id, System.currentTimeMillis(), null, dataId, category, text, context, null);
    }

    public Event(String tenantId, String id, String dataSource, String dataId, String category, String text,
            Map<String, String> context) {
        this(tenantId, id, System.currentTimeMillis(), dataSource, dataId, category, text, context, null);
    }

    public Event(String tenantId, String id, long ctime, String dataId, String category,
            String text) {
        this(tenantId, id, ctime, null, dataId, category, text, null, null);
    }

    public Event(String tenantId, String id, long ctime, String dataSource, String dataId, String category,
            String text) {
        this(tenantId, id, ctime, dataSource, dataId, category, text, null, null);
    }

    public Event(String tenantId, String id, long ctime, String category, String text, Map<String, String> context) {
        this(tenantId, id, ctime, null, null, category, text, context, null);
    }

    public Event(String tenantId, String id, long ctime, String dataSource, String dataId, String category,
            String text, Map<String, String> context) {
        this(tenantId, id, ctime, dataSource, dataId, category, text, context, null);
    }

    public Event(String tenantId, String id, long ctime, String dataId, String category,
            String text, Map<String, String> context, Map<String, String> tags) {
        this(tenantId, id, ctime, null, dataId, category, text, context, tags);
    }

    public Event(String tenantId, String id, long ctime, String dataSource, String dataId, String category,
            String text, Map<String, String> context, Map<String, String> tags) {
        this.tenantId = tenantId;
        this.id = id;
        setCtime(ctime);
        setDataSource(dataSource);
        this.dataId = dataId;
        this.category = category;
        this.text = text;
        this.context = context;
        this.tags = tags;
        this.eventType = EventType.EVENT.name();
    }

    /**
     * Convenience constructor for an Alerting Event.
     * @param alert the Non-Thin Alert, it must be fully defined.
     */
    public Event(Alert alert) {
        this(alert.getTenantId(), alert.getTrigger(), alert.getDampening(), alert.getEvalSets());
        this.eventType = alert.getEventType();
    }

    // TODO [lponce] validate if this constructor is enough
    public Event(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must be not null");
        }
        this.tenantId = event.getTenantId();
        this.trigger = event.getTrigger();
        this.dampening = event.getDampening() != null ? new Dampening(event.getDampening()) : null;
        // evalSets are maintained as reference as it is a mostly read-only field
        this.evalSets = event.getEvalSets();
        this.eventType = event.getEventType();
        this.ctime = event.getCtime();
        this.id = event.getId();
        this.dataSource = event.getDataSource();
        this.dataId = event.getDataId();
        this.context = new HashMap<>(event.getContext());
        this.category = event.getCategory();
        this.text = event.getText();
        this.tags = new HashMap<>(event.getTags());
    }

    public Event(String tenantId, Trigger trigger, Dampening dampening, List<Set<ConditionEval>> evalSets) {
        this.tenantId = tenantId;
        this.trigger = trigger;
        this.dampening = dampening;
        this.evalSets = evalSets;
        this.eventType = EventType.EVENT.name();
        this.ctime = System.currentTimeMillis();

        this.id = trigger.getId() + "-" + this.ctime + "-" + UUID.randomUUID();
        this.dataSource = trigger.getSource();
        this.dataId = trigger.getId();
        this.context = trigger.getContext();
        if (!isEmpty(trigger.getEventCategory())) {
            this.category = trigger.getEventCategory();
        } else {
            this.category = (EventType.ALERT == trigger.getEventType()) ?
                    EventCategory.ALERT.name() : EventCategory.TRIGGER.name();
        }
        if (!isEmpty(trigger.getEventText())) {
            this.text = trigger.getEventText();
        } else {
            this.text = isEmpty(trigger.getDescription()) ? trigger.getName() : trigger.getDescription();
        }
        this.tags = trigger.getTags();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = (ctime <= 0) ? System.currentTimeMillis() : ctime;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = isEmpty(dataSource) ? Data.SOURCE_NONE : dataSource;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, String> getTags() {
        if (null == tags) {
            tags = new HashMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void addTag(String name, String value) {
        if (null == name || null == value) {
            throw new IllegalArgumentException("Tag must have non-null name and value");
        }
        getTags().put(name, value);
    }

    public void removeTag(String name) {
        if (null == name) {
            throw new IllegalArgumentException("Tag must have non-null name");
        }
        getTags().remove(name);
    }

    public Map<String, String> getContext() {
        if (null == context) {
            context = new HashMap<>();
        }
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public void addContext(String name, String value) {
        if (null == name || null == value) {
            throw new IllegalArgumentException("Propety must have non-null name and value");
        }
        getContext().put(name, value);
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Dampening getDampening() {
        return dampening;
    }

    public void setDampening(Dampening dampening) {
        this.dampening = dampening;
    }

    public List<Set<ConditionEval>> getEvalSets() {
        return evalSets;
    }

    public void setEvalSets(List<Set<ConditionEval>> evalSets) {
        this.evalSets = evalSets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Event [tenantId=" + tenantId + ", id=" + id + ", ctime=" + ctime + ", category=" + category
                + ", dataId=" + dataId + ", dataSource=" + dataSource + ", text=" + text + ", context=" + context + ", " +
                "tags=" + tags + ", trigger=" + trigger + "]";
    }

    /* (non-Javadoc)
     * Natural Ordering provided: dataId asc, tenantId asc, dataSource asc, Timestamp asc, id asc. This is important
     * to ensure that the engine naturally processes events for the same dataId is ascending time order.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Event o) {
        /*
            Comparison only should be used on events with proper dataId defined.
         */
        if (this.dataId == null) {
            return this.id.compareTo(o.id);
        }

        int c = this.dataId.compareTo(o.dataId);
        if (0 != c)
            return c;

        c = this.tenantId.compareTo(o.tenantId);
        if (0 != c)
            return c;

        c = this.dataSource.compareTo(o.dataSource);
        if (0 != c)
            return c;

        c = Long.compare(this.ctime, o.ctime);
        if (0 != c) {
            return c;
        }

        return this.id.compareTo(o.id);
    }

    /**
     * @return true if the two Event objects represent the same logical Event (just different values and/or times)
     */
    public boolean same(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (dataId == null) {
            if (other.dataId != null)
                return false;
        } else if (!dataId.equals(other.dataId))
            return false;
        if (dataSource == null) {
            if (other.dataSource != null)
                return false;
        } else if (!dataSource.equals(other.dataSource))
            return false;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        return true;
    }

    private static boolean isEmpty(String s) {
        return null == s || s.trim().isEmpty();
    }

}