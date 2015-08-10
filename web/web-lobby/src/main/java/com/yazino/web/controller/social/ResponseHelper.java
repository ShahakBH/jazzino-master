package com.yazino.web.controller.social;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ResponseHelper {

    public enum Provider {
        FACEBOOK,
        BUDDIES,
        EMAIL_ADDRESS,
        GMAIL
    }

    private ResponseHelper() {
        // utility class
    }

    public static ResponseBuilder setupResponse(final Provider[] supportedProviders, final Provider currentProvider) {
        final ResponseBuilder builder = new ResponseBuilder();

        if (Provider.EMAIL_ADDRESS.equals(currentProvider)) {
            builder.withPageClass("email");
        } else {
            builder.withPageClass(currentProvider.toString().toLowerCase());
            builder.withPersonSelector();
        }
        return builder.withProviders(ResponseHelper.getProviders(supportedProviders, currentProvider));
    }

    private static String toClassString(final ArrayList<String> classes) {
        final StringBuilder pageClassesStringBuilder = new StringBuilder();
        for (final String aClass : classes) {
            if (pageClassesStringBuilder.length() > 0) {
                pageClassesStringBuilder.append(" ");
            }
            pageClassesStringBuilder.append(aClass);
        }
        return pageClassesStringBuilder.toString();
    }

    private static ProviderVO getProvider(Provider provider, boolean isSelected) {
        if (Provider.EMAIL_ADDRESS.equals(provider)) {
            return new ProviderVO("emailAddress", "email", "Email Directly", isSelected, false);
        } else if (Provider.BUDDIES.equals(provider)) {
            return new ProviderVO("buddies", "buddies", "Yazino Buddies", isSelected, false);
        } else if (Provider.GMAIL.equals(provider)) {
            return new ProviderVO("gmail", "gmail", "GMail Contacts", isSelected, true);
        } else {
            String providerCode = provider.toString().toLowerCase();
            return new ProviderVO(providerCode, isSelected);
        }
    }

    private static ArrayList<ProviderVO> getProviders(Provider[] allProviders, Provider selectedProvider) {
        ArrayList<ProviderVO> providers = new ArrayList<>();
        for (final Provider allProvider : allProviders) {
            providers.add(getProvider(allProvider, allProvider.equals(selectedProvider)));
        }
        return providers;
    }

    public static class ResponseBuilder {

        private static final String PAGE_TYPE = "pageType";
        private String viewName = "partials/social-flow/layout";
        private final ArrayList<String> pageClasses = new ArrayList<>();
        private boolean showPersonSelector = false;
        private ArrayList<ProviderVO> provider;
        private Map<String, Object> customModelValues = new HashMap<>();
        private boolean isSentVariant = false;

        public ResponseBuilder() {
            this.withModelAttribute("pageHeaderType", "preSend");
        }

        public ResponseBuilder withPageClass(String newClass) {
            pageClasses.add(newClass);
            return this;
        }

        public ResponseBuilder withPageType(String newType) {
            return this.withModelAttribute(PAGE_TYPE, newType);
        }

        public ResponseBuilder withPersonSelector() {
            showPersonSelector = true;
            return this;
        }

        public ResponseBuilder withSentVariation() {
            isSentVariant = true;
            return this
                    .withPageClass("sent")
                    .withModelAttribute("pageHeaderType", "postSend");
        }

        public ResponseBuilder withProviders(ArrayList<ProviderVO> providers) {
            provider = providers;
            return this;
        }

        public ResponseBuilder withModelAttribute(String key, Object value) {
            customModelValues.put(key, value);
            return this;
        }

        public ModelAndView toModelAndView() {
            ModelMap model = new ModelMap();
            if (!isSentVariant) {
                this.withPageClass("start");
            }
            if (customModelValues.containsKey(PAGE_TYPE)) {
                this.withPageClass((String) customModelValues.get(PAGE_TYPE));
            }
            if (pageClasses.size() > 0) {
                model.addAttribute("pageClasses", toClassString(pageClasses));
            }
            if (showPersonSelector) {
                model.addAttribute("showPersonSelector", true);
            }
            if (provider != null) {
                model.addAttribute("providers", provider);
            }
            model.addAllAttributes(customModelValues);
            return new ModelAndView(viewName, model);
        }

    }

    public static class ProviderVO {
        private String className;
        private String urlName;
        private String altText;
        private boolean isSelected;
        private boolean isImage;

        public ProviderVO(final String className, final String urlPart, final String altText, final boolean isSelected, final boolean isImage) {
            this.className = className;
            this.urlName = urlPart;
            this.altText = altText;
            this.isSelected = isSelected;
            this.isImage = isImage;
        }

        public ProviderVO(String name, boolean isSelected) {
            this(name, name, name, isSelected, true);
        }

        public String getClassName() {
            return className;
        }

        public String getUrlName() {
            return urlName;
        }

        public String getAltText() {
            return altText;
        }

        public boolean getIsSelected() {
            return isSelected;
        }

        public boolean getIsImage() {
            return isImage;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            ProviderVO rhs = (ProviderVO) obj;
            return new EqualsBuilder()
                    .append(this.className, rhs.className)
                    .append(this.urlName, rhs.urlName)
                    .append(this.altText, rhs.altText)
                    .append(this.isSelected, rhs.isSelected)
                    .append(this.isImage, rhs.isImage)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(className)
                    .append(urlName)
                    .append(altText)
                    .append(isSelected)
                    .append(isImage)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("className", className)
                    .append("urlName", urlName)
                    .append("altText", altText)
                    .append("isSelected", isSelected)
                    .append("isImage", isImage)
                    .toString();
        }
    }

}
