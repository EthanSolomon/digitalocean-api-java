/*
 * The MIT License
 * 
 * Copyright (c) 2010-2014 Jeevanandam M. (myjeeva.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.myjeeva.digitalocean.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myjeeva.digitalocean.DigitalOcean;
import com.myjeeva.digitalocean.common.ActionType;
import com.myjeeva.digitalocean.common.ApiAction;
import com.myjeeva.digitalocean.common.Constants;
import com.myjeeva.digitalocean.common.RequestMethod;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.pojo.Action;
import com.myjeeva.digitalocean.pojo.Actions;
import com.myjeeva.digitalocean.pojo.Backups;
import com.myjeeva.digitalocean.pojo.Domain;
import com.myjeeva.digitalocean.pojo.DomainRecord;
import com.myjeeva.digitalocean.pojo.DomainRecords;
import com.myjeeva.digitalocean.pojo.Domains;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.DropletAction;
import com.myjeeva.digitalocean.pojo.Droplets;
import com.myjeeva.digitalocean.pojo.Image;
import com.myjeeva.digitalocean.pojo.ImageAction;
import com.myjeeva.digitalocean.pojo.Images;
import com.myjeeva.digitalocean.pojo.Kernels;
import com.myjeeva.digitalocean.pojo.Key;
import com.myjeeva.digitalocean.pojo.Keys;
import com.myjeeva.digitalocean.pojo.Regions;
import com.myjeeva.digitalocean.pojo.Sizes;
import com.myjeeva.digitalocean.pojo.Snapshots;
import com.myjeeva.digitalocean.serializer.DropletSerializer;

/**
 * DigitalOcean API client wrapper methods Implementation
 * 
 * @author Jeevanandam M. (jeeva@myjeeva.com)
 */
public class DigitalOceanClient implements DigitalOcean, Constants {

  private final Logger LOG = LoggerFactory.getLogger(DigitalOceanClient.class);

  /**
   * Http client
   */
  protected HttpClient httpClient;

  /**
   * OAuth Authorization Token for Accessing DigitalOcean API
   */
  protected String authToken;

  /**
   * DigitalOcean API version. defaults to v2 from constructor
   */
  protected String apiVersion;

  /**
   * DigitalOcean API Host is <code>api.digitalocean.com</code>
   */
  protected String apiHost = "api.digitalocean.com";

  /**
   * Gson Parser instance for deserialize
   */
  private Gson deserialize;

  /**
   * Gson Parser instance for serialize
   */
  private Gson serialize;

  /**
   * JSON Parser instance
   */
  private JsonParser jsonParser;

  public DigitalOceanClient(String authToken) {
    this("v2", authToken);
  }

  /**
   * DigitalOcean Client Constructor
   * 
   * @param apiVersion a {@link String} object
   * @param authToken a {@link String} object
   */
  public DigitalOceanClient(String apiVersion, String authToken) {

    if (!"v2".equalsIgnoreCase(apiVersion)) {
      throw new IllegalArgumentException("Only API version 2 is supported.");
    }

    this.apiVersion = apiVersion;
    this.authToken = authToken;
    initialize();
  }

  /**
   * @return the httpClient
   */
  public HttpClient getHttpClient() {
    return httpClient;
  }

  /**
   * @param httpClient the httpClient to set
   */
  public void setHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * @return the authToken
   */
  public String getAuthToken() {
    return authToken;
  }

  /**
   * @param authToken the authToken to set
   */
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /**
   * @return the apiVersion
   */
  public String getApiVersion() {
    return apiVersion;
  }

  /**
   * @param apiVersion the apiVersion to set
   */
  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  // =======================================
  // Droplet access/manipulation methods
  // =======================================

  @Override
  public Droplets getAvailableDroplets(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validatePageNo(pageNo);

    return (Droplets) invokeAction(new ApiRequest(ApiAction.AVAILABLE_DROPLETS, pageNo));
  }

  @Override
  public Kernels getAvailableKernels(Integer dropletId, Integer pageNo)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletIdAndPageNo(dropletId, pageNo);

    Object[] params = {dropletId};
    return (Kernels) invokeAction(new ApiRequest(ApiAction.AVAILABLE_DROPLETS_KERNELS, params,
        pageNo));
  }

  @Override
  public Snapshots getAvailableSnapshots(Integer dropletId, Integer pageNo)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletIdAndPageNo(dropletId, pageNo);

    Object[] params = {dropletId};
    return (Snapshots) invokeAction(new ApiRequest(ApiAction.GET_DROPLET_SNAPSHOTS, params, pageNo));
  }

  @Override
  public Backups getAvailableBackups(Integer dropletId, Integer pageNo)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletIdAndPageNo(dropletId, pageNo);

    Object[] params = {dropletId};
    return (Backups) invokeAction(new ApiRequest(ApiAction.GET_DROPLET_BACKUPS, params, pageNo));
  }

  @Override
  public Droplet getDropletInfo(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Droplet) invokeAction(new ApiRequest(ApiAction.GET_DROPLET_INFO, params));
  }

  @Override
  public Droplet createDroplet(Droplet droplet) throws DigitalOceanException,
      RequestUnsuccessfulException {
    if (null == droplet
        || null == droplet.getName()
        || null == droplet.getRegion()
        || null == droplet.getSize()
        || (null == droplet.getImage() || (null == droplet.getImage().getId() && null == droplet
            .getImage().getSlug()))) {
      throw new IllegalArgumentException(
          "Missing required parameters [Name, Region Slug, Size Slug, Image Id/Slug] for create droplet.");
    }

    return (Droplet) invokeAction(new ApiRequest(ApiAction.CREATE_DROPLET, droplet));
  }

  @Override
  public Boolean deleteDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_DROPLET, params));
  }

  // Droplet action methods

  @Override
  public Action rebootDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.REBOOT_DROPLET, new DropletAction(
        ActionType.REBOOT), params));
  }

  @Override
  public Action powerCycleDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.POWER_CYCLE_DROPLET, new DropletAction(
        ActionType.POWER_CYCLE), params));
  }

  @Override
  public Action shutdownDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.SHUTDOWN_DROPLET, new DropletAction(
        ActionType.SHUTDOWN), params));
  }

  @Override
  public Action powerOffDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.POWER_OFF_DROPLET, new DropletAction(
        ActionType.POWER_OFF), params));
  }

  @Override
  public Action powerOnDroplet(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.POWER_ON_DROPLET, new DropletAction(
        ActionType.POWER_ON), params));
  }

  @Override
  public Action resetDropletPassword(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.RESET_DROPLET_PASSWORD,
        new DropletAction(ActionType.POWER_CYCLE), params));
  }

  @Override
  public Action resizeDroplet(Integer dropletId, String size) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.RESIZE);
    action.setSize(size);
    return (Action) invokeAction(new ApiRequest(ApiAction.RESIZE_DROPLET, action, params));
  }

  @Override
  public Action takeDropletSnapshot(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.SNAPSHOT_DROPLET, new DropletAction(
        ActionType.SNAPSHOT), params));
  }

  @Override
  public Action takeDropletSnapshot(Integer dropletId, String snapshotName)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.SNAPSHOT);
    action.setName(snapshotName);
    return (Action) invokeAction(new ApiRequest(ApiAction.SNAPSHOT_DROPLET, action, params));
  }

  @Override
  public Action restoreDroplet(Integer dropletId, Integer imageId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.RESTORE);
    action.setImage(imageId);
    return (Action) invokeAction(new ApiRequest(ApiAction.RESTORE_DROPLET, action, params));
  }

  @Override
  public Action rebuildDroplet(Integer dropletId, Integer imageId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.REBUILD);
    action.setImage(imageId);
    return (Action) invokeAction(new ApiRequest(ApiAction.REBUILD_DROPLET, action, params));
  }

  @Override
  public Action disableDropletBackups(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.DISABLE_DROPLET_BACKUPS,
        new DropletAction(ActionType.DISABLE_BACKUPS), params));
  }

  @Override
  public Action renameDroplet(Integer dropletId, String name) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.RENAME);
    action.setName(name);
    return (Action) invokeAction(new ApiRequest(ApiAction.RENAME_DROPLET, action, params));
  }

  @Override
  public Action changeDropletKernel(Integer dropletId, Integer kernelId)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    DropletAction action = new DropletAction(ActionType.CHANGE_KERNEL);
    action.setKernel(kernelId);
    return (Action) invokeAction(new ApiRequest(ApiAction.CHANGE_DROPLET_KERNEL, action, params));
  }

  @Override
  public Action enableDropletIpv6(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.ENABLE_DROPLET_IPV6, new DropletAction(
        ActionType.ENABLE_IPV6), params));
  }

  @Override
  public Action enableDropletPrivateNetworking(Integer dropletId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validateDropletId(dropletId);

    Object[] params = {dropletId};
    return (Action) invokeAction(new ApiRequest(ApiAction.ENABLE_DROPLET_PRIVATE_NETWORKING,
        new DropletAction(ActionType.ENABLE_PRIVATE_NETWORKING), params));
  }


  // ==============================================
  // Actions manipulation/access methods
  // ==============================================

  @Override
  public Actions getAvailableActions(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validatePageNo(pageNo);
    return (Actions) invokeAction(new ApiRequest(ApiAction.AVAILABLE_ACTIONS, pageNo));
  }

  @Override
  public Action getActionInfo(Integer actionId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(actionId, "Missing required parameter - actionId");

    Object[] params = {actionId};
    return (Action) invokeAction(new ApiRequest(ApiAction.GET_ACTION_INFO, params));
  }

  @Override
  public Actions getAvailableDropletActions(Integer dropletId, Integer pageNo)
      throws DigitalOceanException, RequestUnsuccessfulException {
    validateDropletIdAndPageNo(dropletId, pageNo);

    Object[] params = {dropletId};
    return (Actions) invokeAction(new ApiRequest(ApiAction.GET_DROPLET_ACTIONS, params, pageNo));
  }

  @Override
  public Actions getAvailableImageActions(Integer imageId, Integer pageNo)
      throws DigitalOceanException, RequestUnsuccessfulException {
    checkNullAndThrowError(imageId, "Missing required parameter - imageId.");
    validatePageNo(pageNo);

    Object[] params = {imageId};
    return (Actions) invokeAction(new ApiRequest(ApiAction.GET_IMAGE_ACTIONS, params, pageNo));
  }


  // =======================================
  // Images access/manipulation methods
  // =======================================

  @Override
  public Images getAvailableImages(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validatePageNo(pageNo);
    return (Images) invokeAction(new ApiRequest(ApiAction.AVAILABLE_IMAGES, pageNo));
  }

  @Override
  public Image getImageInfo(Integer imageId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(imageId, "Missing required parameter - imageId.");

    Object[] params = {imageId};
    return (Image) invokeAction(new ApiRequest(ApiAction.GET_IMAGE_INFO, params));
  }

  @Override
  public Image getImageInfo(String slug) throws DigitalOceanException, RequestUnsuccessfulException {
    checkEmptyAndThrowError(slug, "Missing required parameter - slug.");

    Object[] params = {slug};
    return (Image) invokeAction(new ApiRequest(ApiAction.GET_IMAGE_INFO, params));
  }

  @Override
  public Image updateImage(Image image) throws DigitalOceanException, RequestUnsuccessfulException {
    if (null == image || null == image.getName()) {
      throw new IllegalArgumentException("Missing required parameter - image object.");
    }

    Object[] params = {image.getId()};
    return (Image) invokeAction(new ApiRequest(ApiAction.UPDATE_IMAGE_INFO, image, params));
  }

  @Override
  public Boolean deleteImage(Integer imageId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(imageId, "Missing required parameter - imageId.");

    Object[] params = {imageId};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_IMAGE, params));
  }

  @Override
  public Action transferImage(Integer imageId, String regionSlug) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(imageId, "Missing required parameter - imageId.");
    checkEmptyAndThrowError(regionSlug, "Missing required parameter - regionSlug.");

    Object[] params = {imageId};
    return (Action) invokeAction(new ApiRequest(ApiAction.TRANSFER_IMAGE, new ImageAction(
        ActionType.TRANSFER, regionSlug), params));
  }



  // =======================================
  // Regions methods
  // =======================================

  @Override
  public Regions getAvailableRegions(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validatePageNo(pageNo);
    return (Regions) invokeAction(new ApiRequest(ApiAction.AVAILABLE_REGIONS, pageNo));
  }


  // =======================================
  // Sizes methods
  // =======================================

  @Override
  public Sizes getAvailableSizes(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    validatePageNo(pageNo);
    return (Sizes) invokeAction(new ApiRequest(ApiAction.AVAILABLE_SIZES, pageNo));
  }


  // =======================================
  // Domain methods
  // =======================================

  @Override
  public Domains getAvailableDomains(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    return (Domains) invokeAction(new ApiRequest(ApiAction.AVAILABLE_DOMAINS, pageNo));
  }

  @Override
  public Domain getDomainInfo(String domainName) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");

    Object[] params = {domainName};
    return (Domain) invokeAction(new ApiRequest(ApiAction.GET_DOMAIN_INFO, params));
  }

  @Override
  public Domain createDomain(Domain domain) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(domain.getName(), "Missing required parameter - domainName.");
    checkEmptyAndThrowError(domain.getIpAddress(), "Missing required parameter - ipAddress.");

    return (Domain) invokeAction(new ApiRequest(ApiAction.CREATE_DOMAIN, domain));
  }

  @Override
  public Boolean deleteDomain(String domainName) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");

    Object[] params = {domainName};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_DOMAIN, params));
  }

  @Override
  public DomainRecords getDomainRecords(String domainName) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");

    Object[] params = {domainName};
    return (DomainRecords) invokeAction(new ApiRequest(ApiAction.GET_DOMAIN_RECORDS, params));
  }

  @Override
  public DomainRecord getDomainRecordInfo(String domainName, Integer recordId)
      throws DigitalOceanException, RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");
    checkNullAndThrowError(recordId, "Missing required parameter - recordId.");

    Object[] params = {domainName, recordId};
    return (DomainRecord) invokeAction(new ApiRequest(ApiAction.GET_DOMAIN_RECORD_INFO, params));
  }

  @Override
  public DomainRecord createDomainRecord(String domainName, DomainRecord domainRecord)
      throws DigitalOceanException, RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");
    if (null == domainRecord) {
      throw new IllegalArgumentException("Missing required parameter - domainRecord");
    }

    Object[] params = {domainName};
    return (DomainRecord) invokeAction(new ApiRequest(ApiAction.CREATE_DOMAIN_RECORD, domainRecord,
        params));
  }

  @Override
  public DomainRecord updateDomainRecord(String domainName, Integer recordId, String name)
      throws DigitalOceanException, RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");
    checkNullAndThrowError(recordId, "Missing required parameter - recordId.");
    checkEmptyAndThrowError(name, "Missing required parameter - name.");

    Object[] params = {domainName, recordId};
    return (DomainRecord) invokeAction(new ApiRequest(ApiAction.UPDATE_DOMAIN_RECORD,
        new DomainRecord(name), params));
  }

  @Override
  public Boolean deleteDomainRecord(String domainName, Integer recordId)
      throws DigitalOceanException, RequestUnsuccessfulException {
    checkEmptyAndThrowError(domainName, "Missing required parameter - domainName.");
    checkNullAndThrowError(recordId, "Missing required parameter - recordId.");

    Object[] params = {domainName, recordId};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_DOMAIN_RECORD, params));
  }

  @Override
  public Keys getAvailableKeys(Integer pageNo) throws DigitalOceanException,
      RequestUnsuccessfulException {
    return (Keys) invokeAction(new ApiRequest(ApiAction.AVAILABLE_KEYS, pageNo));
  }

  @Override
  public Key getKeyInfo(Integer sshKeyId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(sshKeyId, "Missing required parameter - sshKeyId.");

    Object[] params = {sshKeyId};
    return (Key) invokeAction(new ApiRequest(ApiAction.GET_KEY_INFO, params));
  }

  @Override
  public Key getKeyInfo(String fingerprint) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(fingerprint, "Missing required parameter - fingerprint.");

    Object[] params = {fingerprint};
    return (Key) invokeAction(new ApiRequest(ApiAction.GET_KEY_INFO, params));
  }

  @Override
  public Key createKey(Key newKey) throws DigitalOceanException, RequestUnsuccessfulException {
    if (null == newKey) {
      throw new IllegalArgumentException("Missing required parameter - newKey");
    }
    checkEmptyAndThrowError(newKey.getName(), "Missing required parameter - name.");
    checkEmptyAndThrowError(newKey.getPublicKey(), "Missing required parameter - publicKey.");

    return (Key) invokeAction(new ApiRequest(ApiAction.CREATE_KEY, newKey));
  }

  @Override
  public Key updateKey(Integer sshKeyId, String newSshKeyName) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(sshKeyId, "Missing required parameter - sshKeyId.");
    checkEmptyAndThrowError(newSshKeyName, "Missing required parameter - newSshKeyName.");

    Object[] params = {sshKeyId};
    return (Key) invokeAction(new ApiRequest(ApiAction.UPDATE_KEY, new Key(newSshKeyName), params));
  }

  @Override
  public Key updateKey(String fingerprint, String newSshKeyName) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(fingerprint, "Missing required parameter - fingerprint.");
    checkEmptyAndThrowError(newSshKeyName, "Missing required parameter - newSshKeyName.");

    Object[] params = {fingerprint};
    return (Key) invokeAction(new ApiRequest(ApiAction.UPDATE_KEY, new Key(newSshKeyName), params));
  }

  @Override
  public Boolean deleteKey(Integer sshKeyId) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkNullAndThrowError(sshKeyId, "Missing required parameter - sshKeyId.");

    Object[] params = {sshKeyId};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_KEY, params));
  }

  @Override
  public Boolean deleteKey(String fingerprint) throws DigitalOceanException,
      RequestUnsuccessfulException {
    checkEmptyAndThrowError(fingerprint, "Missing required parameter - fingerprint.");

    Object[] params = {fingerprint};
    return (Boolean) invokeAction(new ApiRequest(ApiAction.DELETE_KEY, params));
  }


  private Object invokeAction(ApiRequest request) throws DigitalOceanException,
      RequestUnsuccessfulException {
    ApiResponse response = performAction(request);
    return response.getData();
  }

  protected ApiResponse performAction(ApiRequest request) throws DigitalOceanException,
      RequestUnsuccessfulException {

    URI uri = createUri(request);
    String response = null;

    if (RequestMethod.GET == request.getMethod()) {
      response = doGet(uri);
    } else if (RequestMethod.POST == request.getMethod()) {
      response = doPost(uri, getRequestData(request));
    } else if (RequestMethod.PUT == request.getMethod()) {
      response = doPut(uri, getRequestData(request));
    } else if (RequestMethod.DELETE == request.getMethod()) {
      response = doDelete(uri);
    }

    ApiResponse apiResponse = new ApiResponse(request.getApiAction(), true);

    if ("true".equals(response) || "false".equals(response)) {
      apiResponse.setData(Boolean.valueOf(response));
    } else {
      if (request.getElementName().endsWith("s")) {
        apiResponse.setData(deserialize.fromJson(response, request.getClazz()));
      } else {
        JsonElement element =
            jsonParser.parse(response).getAsJsonObject().get(request.getElementName());
        apiResponse.setData(deserialize.fromJson(element, request.getClazz()));
      }
    }

    return apiResponse;
  }

  private String doGet(URI uri) throws DigitalOceanException, RequestUnsuccessfulException {
    HttpGet get = new HttpGet(uri);
    get.setHeaders(getRequestHeaders());
    return execute(get);
  }

  private String doPost(URI uri, StringEntity entity) throws DigitalOceanException,
      RequestUnsuccessfulException {
    HttpPost post = new HttpPost(uri);
    post.setHeaders(getRequestHeaders());

    if (null != entity) {
      post.setEntity(entity);
    }

    return execute(post);
  }

  private String doPut(URI uri, StringEntity entity) throws DigitalOceanException,
      RequestUnsuccessfulException {
    HttpPut put = new HttpPut(uri);
    put.setHeaders(getRequestHeaders());

    if (null != entity) {
      put.setEntity(entity);
    }

    return execute(put);
  }

  private String doDelete(URI uri) throws DigitalOceanException, RequestUnsuccessfulException {
    HttpDelete delete = new HttpDelete(uri);
    delete.setHeaders(getRequestHeaders());
    delete.setHeader("Content-Type", FORM_URLENCODED_CONTENT_TYPE);
    return execute(delete);
  }

  private String execute(HttpRequestBase request) throws DigitalOceanException,
      RequestUnsuccessfulException {
    String response = "";
    try {
      HttpResponse httpResponse = httpClient.execute(request);

      int statusCode = httpResponse.getStatusLine().getStatusCode();
      if (HttpStatus.SC_OK == statusCode || HttpStatus.SC_CREATED == statusCode
          || HttpStatus.SC_ACCEPTED == statusCode) {
        response = httpResponseToString(httpResponse);
      } else {
        response = evaluateResponse(httpResponse);
      }

      LOG.debug("HTTP Response: " + response);
    } catch (ClientProtocolException cpe) {
      throw new RequestUnsuccessfulException(cpe.getMessage(), cpe);
    } catch (IOException ioe) {
      throw new RequestUnsuccessfulException(ioe.getMessage(), ioe);
    } finally {
      request.releaseConnection();
    }

    return response;
  }

  private String evaluateResponse(HttpResponse httpResponse) throws DigitalOceanException {
    int statusCode = httpResponse.getStatusLine().getStatusCode();

    if (HttpStatus.SC_NO_CONTENT == statusCode) {
      return "true";
    }

    if (statusCode >= 400 && statusCode < 510) {
      String jsonStr = httpResponseToString(httpResponse);
      LOG.debug("JSON Response: " + jsonStr);

      JsonObject jsonObj = jsonParser.parse(jsonStr).getAsJsonObject();
      String id = jsonObj.get("id").getAsString();
      String errorMsg =
          String.format("\nHTTP Status Code: %s\nError Id: %s\nError Message: %s", statusCode, id,
              jsonObj.get("message").getAsString());
      LOG.debug(errorMsg);
      throw new DigitalOceanException(errorMsg, id, statusCode);
    }

    return null;
  }

  private String httpResponseToString(HttpResponse httpResponse) {
    String response = "";
    if (null != httpResponse.getEntity()) {
      try {
        response = EntityUtils.toString(httpResponse.getEntity(), UTF_8);
      } catch (ParseException pe) {
        LOG.error(pe.getMessage(), pe);
      } catch (IOException ioe) {
        LOG.error(ioe.getMessage(), ioe);
      }
    }
    return response;
  }

  private URI createUri(ApiRequest request) {
    URIBuilder ub = new URIBuilder();
    ub.setScheme(HTTPS_SCHEME);
    ub.setHost(apiHost);
    ub.setPath(createPath(request));

    if (null != request.getPageNo()) {
      ub.setParameter(PARAM_PAGE_NO, request.getPageNo().toString());
    }

    URI uri = null;
    try {
      uri = ub.build();
    } catch (URISyntaxException use) {
      LOG.error(use.getMessage(), use);
    }

    return uri;
  }

  private Header[] getRequestHeaders() {
    Header[] headers =
        {new BasicHeader("X-User-Agent", "DigitalOcean API Client by myjeeva.com"),
            new BasicHeader("Content-Type", JSON_CONTENT_TYPE),
            new BasicHeader("Authorization", "Bearer " + authToken)};
    return headers;
  }

  private String createPath(ApiRequest request) {
    String path = URL_PATH_SEPARATOR + apiVersion + request.getApiAction().getPath();
    return (null == request.getParams() ? path : String.format(path, request.getParams()));
  }

  private StringEntity getRequestData(ApiRequest request) {
    StringEntity data = null;

    if (null != request.getData()) {
      String inputData = serialize.toJson(request.getData());
      data = new StringEntity(inputData, ContentType.create(JSON_CONTENT_TYPE));
    }

    return data;
  }

  // =======================================
  // Validation methods
  // =======================================

  private void validateDropletIdAndPageNo(Integer dropletId, Integer pageNo) {
    validateDropletId(dropletId);
    validatePageNo(pageNo);
  }

  private void validateDropletId(Integer dropletId) {
    checkNullAndThrowError(dropletId, "Missing required parameter - dropletId.");
  }

  private void validatePageNo(Integer pageNo) {
    checkNullAndThrowError(pageNo, "Missing required parameter - pageNo.");
  }

  private void checkNullAndThrowError(Integer integer, String msg) {
    if (null == integer) {
      LOG.error(msg);
      throw new IllegalArgumentException(msg);
    }
  }

  private void checkEmptyAndThrowError(String str, String msg) {
    if (StringUtils.isEmpty(str)) {
      LOG.error(msg);
      throw new IllegalArgumentException(msg);
    }
  }

  private void initialize() {
    this.deserialize = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

    this.serialize =
        new GsonBuilder().setDateFormat(DATE_FORMAT)
            .registerTypeAdapter(Droplet.class, new DropletSerializer())
            .excludeFieldsWithoutExposeAnnotation().create();

    this.jsonParser = new JsonParser();

    this.httpClient = new DefaultHttpClient(new PoolingClientConnectionManager());
  }
}
