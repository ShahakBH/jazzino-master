package com.yazino.web.parature.service;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * @see "http://s5.parature.com/UserGuide/Content/API/paratureapispecification.html"
 */
@Service
public class ParatureSupportUserService {
    private static final Logger LOG = LoggerFactory.getLogger(ParatureSupportUserService.class);

    private static final int SUCCESS = 201;
    private static final String USER_EXISTS_RESPONSE_CODE
            = "One or more fields are invalid; Invalid Field Validation Message :"
            + " An existing Customer record has been found";
    private static final String CUSTOMER_CREATE_XML = "<Customer> "
            + "<Email>%s</Email>"
            + "<First_Name>%s</First_Name> "
            + "<Last_Name>%s</Last_Name> \n"
            + "<User_Name>%s</User_Name>\n"
            + "<Password> test1234 </Password>\n"
            + "<Password_Confirm> test1234 </Password_Confirm>\n"
            + "<Custom_Field id=\"49\">%s</Custom_Field>\n"
            + "<Sla><Sla id=\"2\"/></Sla>\n"
            + "<Status><Status id=\"2\" /></Status>\n"
            + "</Customer>";

    private final HttpClient httpClient;
    private final String paratureURL;
    private final String token;

    @Autowired
    public ParatureSupportUserService(final HttpClient httpClient,
                                      @Value("${parature.service.url}") final String paratureUrl,
                                      @Value("${parature.service.token}") final String token) {
        notNull(httpClient, "httpClient may not be null");
        notNull(paratureUrl, "paratureUrl may not be null");
        notNull(token, "token may not be null");

        this.httpClient = httpClient;
        this.paratureURL = paratureUrl;
        this.token = token;
    }

    public boolean hasUserRegistered(final BigDecimal playerId) throws IOException {
        notNull(playerId, "playerId may not be null");
        LOG.debug("Testing if player ID {} is registered with Parature", playerId);

        InputStream responseStream = null;
        try {
            final HttpGet query = new HttpGet(String.format("%s?User_Name=%s&_token_=%s", paratureURL, playerId.toPlainString(), token));
            final HttpResponse queryResponse = httpClient.execute(query);
            if (queryResponse.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
                LOG.warn("Unable to lookup player {} for Parature, received response {}, content {}",
                        playerId, queryResponse.getStatusLine().getStatusCode(), responseAsString(queryResponse));
                return false;
            }
            responseStream = queryResponse.getEntity().getContent();

            final Document doc = parse(responseStream);

            final String total = doc.getDocumentElement().getAttribute("total");
            return Integer.parseInt(total) > 0;

        } catch (Exception e) {
            LOG.error("Customer lookup failed for player {}", playerId, e);
            return false;

        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }


    public void createSupportUser(final BigDecimal playerId,
                                  final PlayerProfile playerProfile)
            throws IOException, SupportUserServiceException {
        LOG.debug("Creating user: {} using parature url: {}", playerProfile, paratureURL);

        final HttpPost post = new HttpPost(paratureURL + "?_token_=" + URLEncoder.encode(token, "utf-8"));

        String firstNameField = playerProfile.getFirstName();
        if (isBlank(firstNameField)) {
            firstNameField = playerProfile.getDisplayName();
        }

        final String xml = String.format(CUSTOMER_CREATE_XML, playerProfile.getEmailAddress(), firstNameField,
                playerProfile.getLastName(), playerId, playerProfile.getDisplayName());

        post.setEntity(new StringEntity(xml));
        final HttpResponse createResponse = httpClient.execute(post);

        if (createResponse.getStatusLine().getStatusCode() != SUCCESS) {
            LOG.info("Problem creating account for {} using paratureURL {}", playerProfile, paratureURL);
            handleError(playerId, createResponse);
        } else {
            EntityUtils.consume(createResponse.getEntity());
        }
    }

    private String responseAsString(final HttpResponse response) throws IOException {
        if (response.getEntity() != null) {
            return EntityUtils.toString(response.getEntity());
        }
        return null;
    }

    private void handleError(final BigDecimal playerId,
                             final HttpResponse response) throws SupportUserServiceException {

        InputStream responseStream = null;
        try {
            responseStream = response.getEntity().getContent();
            final Document doc = parse(responseStream);

            final NodeList errors = doc.getElementsByTagName("Error");

            if (errors.getLength() == 1 && errors.item(0).getAttributes().getNamedItem("message").getNodeValue().equals(USER_EXISTS_RESPONSE_CODE)) {
                LOG.info("Register user with parature failed for playerId[{}] but response was already exists, so updated supportuserDAO", playerId);

            } else {
                LOG.error("Register user with parature failed for playerId[{}] response : {}", playerId, dumpXml(doc));
                throw new SupportUserServiceException(dumpXml(doc));
            }

        } catch (Exception e) {
            throw new SupportUserServiceException("Failed to parse response from Parature", e);

        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    private Document parse(final InputStream responseStream)
            throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final InputSource is = new InputSource(responseStream);

        return db.parse(is);
    }

    private String dumpXml(final Document document) throws IOException {
        try {
            final DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
            final LSSerializer writer = impl.createLSSerializer();
            writer.getDomConfig().setParameter("format-pretty-print", true);
            return writer.writeToString(document);

        } catch (Exception e) {
            LOG.error("Failed to dump XML", e);
            return "(dump failed)";
        }
    }
}
