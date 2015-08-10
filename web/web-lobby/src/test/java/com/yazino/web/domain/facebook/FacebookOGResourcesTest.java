package com.yazino.web.domain.facebook;


import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class FacebookOGResourcesTest {

    private Map<String, FacebookOGResource> resources = newHashMap();
    private FacebookOGResources facebookOGResources;

    FacebookOGResource facebookOGResource;

    @Before
    public void setUp(){
        facebookOGResource = new FacebookOGResource();
        facebookOGResource.setArticle("a");
        facebookOGResource.setDescription("blah");
        facebookOGResource.setTitle("blah");

        resources.put("blah",facebookOGResource );
        facebookOGResources = new FacebookOGResources(resources);
    }

    @Test
    public void getTitleShouldReturnSimpleTitle(){

        assertThat(facebookOGResources.getTitle("blah"), is(equalTo("blah")));
    }

    @Test
    public void getTitleShouldReturnTitleWithNoArticle(){
        assertThat(facebookOGResources.getTitle("blah"), is(equalTo("blah")));
    }

    @Test
    public void getTitleShouldReturnArticle(){

        assertThat(facebookOGResources.getArticle("blah"), is(equalTo("a")));
    }

    @Test
    public void nullobjectIdShouldNotBlowUp(){
     assertThat(facebookOGResources.getArticle("blahblah"), is(equalTo("")));
     assertThat(facebookOGResources.getTitle("blahblah"), is(equalTo("")));
     assertThat(facebookOGResources.getDescription("blahblah"), is(equalTo("")));
    }

}
