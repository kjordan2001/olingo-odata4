/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.communication.request.cud;

import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.olingo.client.api.CommonODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.http.HttpMethod;
import org.apache.olingo.client.core.communication.request.AbstractODataBasicRequest;
import org.apache.olingo.client.core.communication.response.AbstractODataResponse;
import org.apache.olingo.client.core.uri.URIUtils;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.ResWrap;
import org.apache.olingo.commons.api.domain.CommonODataEntity;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.serialization.ODataDeserializerException;
import org.apache.olingo.commons.api.serialization.ODataSerializerException;

/**
 * This class implements an OData update request.
 *
 * @param <E> concrete ODataEntity implementation
 */
public class ODataEntityUpdateRequestImpl<E extends CommonODataEntity>
        extends AbstractODataBasicRequest<ODataEntityUpdateResponse<E>>
        implements ODataEntityUpdateRequest<E> {

  /**
   * Changes to be applied.
   */
  private final E changes;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method request method.
   * @param uri URI of the entity to be updated.
   * @param changes changes to be applied.
   */
  public ODataEntityUpdateRequestImpl(final CommonODataClient<?> odataClient,
          final HttpMethod method, final URI uri, final E changes) {

    super(odataClient, method, uri);
    this.changes = changes;
  }

  @Override
  public ODataFormat getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  protected InputStream getPayload() {
    try {
      return odataClient.getWriter().writeEntity(changes, ODataFormat.fromString(getContentType()));
    } catch (final ODataSerializerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public ODataEntityUpdateResponse<E> execute() {
    final InputStream input = getPayload();
    ((HttpEntityEnclosingRequestBase) request).setEntity(URIUtils.buildInputStreamEntity(odataClient, input));

    try {
      return new ODataEntityUpdateResponseImpl(odataClient, httpClient, doExecute());
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Response class about an ODataEntityUpdateRequest.
   */
  private class ODataEntityUpdateResponseImpl extends AbstractODataResponse implements ODataEntityUpdateResponse<E> {

    /**
     * Changes.
     */
    private E entity = null;

    private ODataEntityUpdateResponseImpl(final CommonODataClient<?> odataClient, final HttpClient httpClient,
            final HttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getBody() {
      if (entity == null) {
        try {
          final ResWrap<Entity> resource = odataClient.getDeserializer(ODataFormat.fromString(getAccept())).
                  toEntity(getRawResponse());

          entity = (E) odataClient.getBinder().getODataEntity(resource);
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return entity;
    }
  }
}
