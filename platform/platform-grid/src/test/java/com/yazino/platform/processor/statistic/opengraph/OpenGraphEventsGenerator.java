package com.yazino.platform.processor.statistic.opengraph;

import com.yazino.platform.opengraph.OpenGraphAction;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class OpenGraphEventsGenerator {

    public static void main(String[] args) {

        String domain = "breakmycasino.com";
        String userId = "8411753";

        if (args != null && args.length > 0) {
            if (args[0] != null) {
                domain = args[0];
            }
            if (args[1] != null) {
                userId = args[1];
            }
        } else {

            System.out.println("Using default domain: " + domain + " . Domain can be overridden with command line parameter.");
            System.out.println("Using default user: " + userId + " . UserId can be overridden with command line parameter.");
            System.out.println("Usage: java com.yazino.platform.processor.statistic.opengraph.OpenGraphEventsGenerator [domain] [userId]");
        }
        new OpenGraphEventsGenerator(domain, userId);
    }


    /*
     * to use, set the
     * Player Id,
     * fb authentication codes for each facebook game,
     * facebook application name (this is diff for each env)
     */
    public OpenGraphEventsGenerator(final String domain, final String userId) {

        generateCurlsToCreateStats(domain, userId);
        System.out.println();
        System.out.println();

        //for the accessToken, goto "http://mem-prd-worker1:7900/strata.server.lobby.worker.opengraph/accesstokens.do?playerId=643364&gameType=SLOTS"
        //                           http://mem-prd-worker1:7900/web-opengraph-worker/accesstokens.do?playerId=8411753&gameType=HIGH_STAKES
        // if it's not there, repeat for worker2,
        //(this could EASILY be implemented here)

        HashMap<String, String> tokens = newHashMap();
        //add games here if needed

//        tokens.put("SLOTS", "SLOTSID");//<-- put your auth keys here!
//        tokens.put("BLACKJACK", "BJID");

//        tokens.put("SLOTS","AAAAAFQUN9ooBACH2AIRgGAoLy7rwLLiCc19DnoM17H4i1BE3eJLVvP8nuUXYtlZCHsYqTO1xtznGW8SVlQJxPi4KMNVHtZA9zRb4KNJwZDZD");
//        tokens.put("BLACKJACK","AAAAAGfZAUvksBAAZCMZCnpS6wp2UpJZAeKpAN3ZAbYhrrc5n7Ar0HfVuZCPDH9P5wQRreUG1JIBfI93mQwEYcmuG1PaZAfBoowqEwB0lA2JOwZDZD");
        tokens.put("BRZ", "AAADUNlJ0pH8BAM3szODD6WQgixbfF1EahQH3kTlIrMNlthDuPVKdnMxX3Mdr9TLV9VcabHgRTjgYr0Ly6pgAjLZC8iZCYzzftC2IjB7GchREDoKZCvA");

        //yes this is hackety but this isn't prod code so....
        final HashMap<String, String> gameNames = newHashMap();
//        gameNames.put("BLACKJACK", "absoluteblackjack");//prod names
//        gameNames.put("SLOTS", "yazinoslotswheeldeal");
        gameNames.put("BRZ", "mmchighstakes");
//        gameNames.put("BLACKJACK", "mmcblackjack");//bmc names
//        gameNames.put("SLOTS", "mmcslots");


        generateStraightIntoFB(domain, userId, tokens, gameNames);
    }


    private void generateCurlsToCreateStats(final String domain, final String userId) {
        DefaultStatisticToActionTransformer transformer = new DefaultStatisticToActionTransformer();
        Map<String, OpenGraphAction> map = transformer.getMap();

        System.out.println("copy the following into a bash script and run (too big for cmd-line):");
        System.out.println("#!/bin/sh");

        final ArrayList<String> objectIds = newArrayList(map.keySet());

        Collections.sort(objectIds);

        for (String objectId : objectIds) {
            String game = objectId.split("_")[0];
            System.out.println(String.format(
                    "curl 'http://%s/games-testweb/playerStats?playerId=%s&gameType=%s&statisticEvents=%s'",
                    domain,
                    userId,
                    game.equals("BRZ") ? "HIGH_STAKES" : game,
                    objectId));
        }

    }

    private void generateStraightIntoFB(final String domain,
                                        final String userId,
                                        final HashMap<String, String> tokens,
                                        final HashMap<String, String> gameNames) {

        System.out.println("copy the following into a bash script and run (too big for cmd-line):");
        System.out.println("#!/bin/sh");

        for (String gameType : tokens.keySet()) {
            String accessTokenForPlayer = tokens.get(gameType);


            DefaultStatisticToActionTransformer transformer = new DefaultStatisticToActionTransformer();
            Map<String, OpenGraphAction> map = transformer.getMap();


            final ArrayList<String> actionIds = newArrayList(map.keySet());

            Collections.sort(actionIds);
            final HashSet<String> done = newHashSet();
            for (String actionId : actionIds) {
                final OpenGraphAction openGraphAction = map.get(actionId);
                String game = actionId.split("_")[0];
                final String objectId = openGraphAction.getObject().getId();

                if (game.equals(gameType) && !done.contains(objectId)) {

                    done.add(objectId);

                    System.out.println(String.format(
                            String.format(
                                    "curl -F 'access_token=%s' -F '%s=http://www.%s/fbog/%s/%s'  'https://graph.facebook.com/me/%s:%s'",
                                    accessTokenForPlayer,
                                    openGraphAction.getObject().getType(),
                                    domain,
                                    openGraphAction.getObject().getType(),
                                    objectId,
                                    gameNames.get(game),
                                    openGraphAction.getName()

                            ),
                            domain,
                            userId,
                            game,
                            actionId));
                    System.out.println("echo");
                }
            }
        }

        //ANNOYINGLY, this doesn't handle levels.....
        //you need something like curl -F 'access_token=AAADUNlJ0pH8BAG4IOco8H5vRd9XHz8M8kplGa6rr2XRFueSdDHvQU5WGfV9dwRodxT5rHQEWXfLY7FsD6Mzesd5wVniYm4CrlX0Ygfandv7vX25S' -F 'level=http://www.yazino.com/fbog/level/hs_level_2'  'https://graph.facebook.com/me/yazinohighstakes:gain'

    }

}
