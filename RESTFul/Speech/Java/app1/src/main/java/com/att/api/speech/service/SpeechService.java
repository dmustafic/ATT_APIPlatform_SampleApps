package com.att.api.speech.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.att.api.rest.APIResponse;
import com.att.api.rest.RESTClient;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.att.api.rest.RESTConfig;
import com.att.api.speech.model.SpeechResponse;

/**
 * Class that handles communication with the speech server.
 *
 */
public class SpeechService {
    private RESTConfig cfg;

    private boolean chunked;

    /**
     * Creates a speech service object. By default, chunked is set to false.
     *
     * @param cfg
     *            the configuration to use for setting HTTP request values
     * @see RESTConfig
     */
    public SpeechService(RESTConfig cfg) {
        this.cfg = cfg;
        chunked = false;
    }

    /**
     * If the server returned a successful response, this method parses the
     * response and returns a {@link SpeechResponse} object.
     *
     * @param response
     *            the response returned by the server
     * @return the server response as a SpeechResponse object
     * @throws IOException
     *             if unable to read the passed-in response
     * @throws java.text.ParseException
     */
    private SpeechResponse parseSuccess(String response)
            throws IOException, java.text.ParseException {
        String result = response;
        JSONObject object = new JSONObject(result);
        JSONObject recognition = object.getJSONObject("Recognition");
        SpeechResponse sp = new SpeechResponse();
        sp.addAttribute("ResponseID", recognition.getString("ResponseId"));
        final String jStatus = recognition.getString("Status");

        sp.addAttribute("Status", jStatus);

        if (jStatus.equals("OK")) {
            JSONArray nBest = recognition.getJSONArray("NBest");
            final String[] names = { "Hypothesis", "LanguageId", "Confidence",
                    "Grade", "ResultText", "Words", "WordScores" };
            for (int i = 0; i < nBest.length(); ++i) {
                JSONObject nBestObject = (JSONObject) nBest.get(i);
                for (final String name : names) {
                    String value = nBestObject.getString(name);
                    if (value != null) {
                        sp.addAttribute(name, value);
                    }
                }
            }
        }

        return sp;
    }

    /**
     * Sets whether to send the request body chunked or non-chunked.
     *
     * @param chunked
     *            value to set
     */
    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    /**
     * Sends the request to the server.
     *
     * @param file
     *            file to send.
     * @param accessToken
     *            access token used for authorization
     * @param speechContext
     *            speech context
     * @param subContext
     *            speech
     * @return a response in the form of a SpeechResponse object
     * @see SpeechResponse
     */
    public SpeechResponse sendRequest(File file, String accessToken,
            String speechContext, String xArg, String subContext) throws Exception {
        RESTClient restClient = new RESTClient(this.cfg);
        restClient.addHeader("Authorization", "Bearer " + accessToken).
                addHeader("Accept", "application/json").
                addHeader("X-SpeechContext", speechContext);
        if (xArg != null && !xArg.equals("")) {
            restClient.addHeader("X-Arg", xArg);
        }
        if (subContext != null && !subContext.equals("") && speechContext.equals("Gaming")){
            restClient.addHeader("X-SpeechSubContext",subContext);
        }
        APIResponse apiResponse = restClient.httpPost(file,this.chunked);
        return parseSuccess(apiResponse.getResponseBody());
    }
}