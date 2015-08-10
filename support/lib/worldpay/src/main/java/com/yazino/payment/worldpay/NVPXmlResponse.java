package com.yazino.payment.worldpay;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.dom4j.DocumentHelper.parseText;

public class NVPXmlResponse implements Serializable {
    private static final long serialVersionUID = -1020222240056576112L;

    private static final Logger LOG = LoggerFactory.getLogger(NVPXmlResponse.class);

    private final Map<String, Property> valuesByFieldName = new HashMap<>();

    public NVPXmlResponse(final String responseXml) {
        xmlToKeyValuePairs(responseXml);
    }

    public static boolean isXmlResponse(final String responseBody) {
        return responseBody.startsWith("<?xml");
    }

    public Property getProperty(final String fieldName) {
        return valuesByFieldName.get(fieldName);
    }

    private void xmlToKeyValuePairs(final String unparsedResponse) {
        LOG.debug("Parsing xml response: {}", unparsedResponse);
        try {
            Document document = parseText(unparsedResponse);
            for (Property property : parseElements(document.getRootElement().elements())) {
                valuesByFieldName.put(property.name, property);
            }
        } catch (DocumentException e) {
            LOG.error(format("Cannot parse document text. %s", unparsedResponse), e);
            throw new RuntimeException(format("Cannot parse document text. %s", unparsedResponse), e);
        }
    }

    private List<Property> parseElements(final List elements) {
        List<Property> properties = new ArrayList<>();
        final Map<String, List<Element>> groupedByName = groupByName(elements);
        for (String elementName : groupedByName.keySet()) {
            List<Element> elementsWithSameName = groupedByName.get(elementName);

            // is an element comparable with key value pair
            // i.e. element has only a value
            final Element element = elementsWithSameName.get(0);
            if (elementsWithSameName.size() == 1 && element.elements().isEmpty()) {
                KeyValueProperty property = new KeyValueProperty(elementName);
                property.value = element.getStringValue();
                properties.add(property);
            }

            // is an element comparable with nested properties
            // i.e. element has nested elements that may are
            if (elementsWithSameName.size() == 1 && !element.elements().isEmpty()) {
                NestedProperties nestedProperties = new NestedProperties(elementName);
                nestedProperties.addProperties(parseElements(element.elements()));
                properties.add(nestedProperties);
            }

            // is an element comparable with a key that returns a list of NestedProperties
            // i.e. multiple elements with the same name
            if (elementsWithSameName.size() > 1) {
                final NestedPropertiesGroup propertyCollectionGroupedByName = new NestedPropertiesGroup(elementName);
                for (Element namedElement : elementsWithSameName) {
                    final List<Property> propertiesForName = parseElements(namedElement.elements());
                    if (propertiesForName.size() == 1) {
                        propertyCollectionGroupedByName.add(propertiesForName.iterator().next());
                    } else {
                        NestedProperties nestedProperties = new NestedProperties(elementName);
                        nestedProperties.addProperties(propertiesForName);
                        propertyCollectionGroupedByName.add(nestedProperties);
                    }
                }
                properties.add(propertyCollectionGroupedByName);
            }
        }
        return properties;
    }

    private Map<String, List<Element>> groupByName(final List elements) {
        Map<String, List<Element>> groupedByName = new HashMap<>();
        for (Object elementObj : elements) {
            Element element = (Element) elementObj;
            if (!groupedByName.containsKey(element.getName())) {
                groupedByName.put(element.getName(), new ArrayList<Element>());
            }
            groupedByName.get(element.getName()).add(element);
        }
        return groupedByName;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public abstract class Property {
        private String name;

        public Property(final String name) {
            this.name = name;
        }

        public boolean hasNestedProperties() {
            return this instanceof NestedProperties;
        }

        public boolean hasPropertiesWithSameName() {
            return this instanceof NestedPropertiesGroup;
        }

        public abstract String getValue();
        public abstract Property getNestedProperty(final String name);
        public abstract List<Property> getNestedPropertyGroup();
    }

    public final class KeyValueProperty extends Property {
        private String value;

        public KeyValueProperty(final String name) {
            super(name);
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public Property getNestedProperty(final String name) {
            throw new RuntimeException("Does not have nested value");
        }

        @Override
        public List<Property> getNestedPropertyGroup() {
            throw new RuntimeException("Does not have properties grouped by name");
        }
    }

    public final class NestedProperties extends Property {
        private Map<String, Property> properties = new HashMap<>();

        public NestedProperties(final String name) {
            super(name);
        }

        public void addProperties(final List<Property> properties) {
            for (Property property : properties) {
                this.properties.put(property.name, property);
            }
        }

        @Override
        public String getValue() {
            throw new RuntimeException("Does not have value");
        }

        @Override
        public Property getNestedProperty(final String name) {
            return properties.get(name);
        }

        @Override
        public List<Property> getNestedPropertyGroup() {
            throw new RuntimeException("Does not have properties grouped by name");
        }
    }

    public final class NestedPropertiesGroup extends Property {
        private List<Property> propertyCollectionByName = new ArrayList<>();

        public NestedPropertiesGroup(final String name) {
            super(name);
        }

        public void add(final Property property) {
            propertyCollectionByName.add(property);
        }

        @Override
        public String getValue() {
            throw new RuntimeException("Does not have value");
        }

        @Override
        public Property getNestedProperty(final String name) {
            throw new RuntimeException("Does not have nested value");
        }

        @Override
        public List<Property> getNestedPropertyGroup() {
            return propertyCollectionByName;
        }
    }
}
