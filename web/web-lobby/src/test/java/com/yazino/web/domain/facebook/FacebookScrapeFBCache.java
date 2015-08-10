package com.yazino.web.domain.facebook;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;

public class FacebookScrapeFBCache {

    private static final int MAXIMUM_LEVEL_ACROSS_ALL_GAMES = 25;

    public static void main(String[] args) {
        final String[] domains;
        if (args != null && args.length > 0 && args[0] != null) {
            domains = new String[] { args[0] };
        } else {
//            domain = "breakmycasino.com";
//            domains = new String[] { "yazino.com", "www.yazino.com" }; // Not clear whether subdomain is required so running both to be sure...
            domains = new String[]{"env-proxy.london.yazino.com/jrae-centos"}; //this is how to reset local env
            System.out.println( "Using default domains: " + domains + " . Domain can be overridden with command line parameter." );
        }

        new FacebookScrapeFBCache(domains);
    }

    @SuppressWarnings({"unchecked"})
    private FacebookScrapeFBCache(String[] domains) {
        final ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(
                "classpath:/META-INF/spring/lobby-spring.xml");
        FacebookOGResources resources = (FacebookOGResources) classPathXmlApplicationContext.getBean("facebookOGResources");
        final Map<String, FacebookOGResource> resourceMap = (Map<String, FacebookOGResource>) ReflectionTestUtils.getField(resources, "resourceMap");

        System.out.println("copy the following into a bash script and run (too big for cmd-line):");
        HashSet<String> games= newHashSet();
        System.out.println("#!/bin/sh");
        for (String domain : domains) {
            for (String objectId : resourceMap.keySet()) {

                games.add(objectId.split("_")[0]);

                System.out.println("curl -X POST -F \"id=http://" + domain + "/fbog/achievement/" + objectId + "\"  -F \"scrape=true\"  \"https://graph.facebook.com\"");
                System.out.println("echo ");
                System.out.println("echo ");
            }


            //level generation
            for (String game : games) {
                for (int i = 1; i < MAXIMUM_LEVEL_ACROSS_ALL_GAMES; i++) {

                     System.out.println("curl -X POST -F \"id=http://" + domain + "/fbog/level/" + game +"_level_" + i + "\"  -F \"scrape=true\"  \"https://graph.facebook.com\"");
                     System.out.println("echo ");
                     System.out.println("echo ");
                }
            }
        }
        System.exit(0);
    }
}
