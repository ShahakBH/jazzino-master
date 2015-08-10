package strata.server.lobby.api.facebook;

/**
 * Used to distinguish different types of Facebook App To User Requests
 * in order to standardise the output of the data from a FacebookAppToUserRequest
 * All AppRequests should specify the type in the Data section
 * e.g.
 * "data": "{"actions":null,"type":"Engagement","tracking":{"ref":"trackingref"}}",
 */
public enum FacebookAppToUserRequestType {
    Engagement,
    LegacyEngagement
}
