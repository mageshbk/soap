/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.switchyard.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;

import org.apache.log4j.Logger;
import org.switchyard.BaseHandler;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.HandlerException;
import org.switchyard.Message;
import org.switchyard.ServiceDomain;
import org.switchyard.internal.ServiceDomains;
import org.switchyard.soap.util.SOAPUtil;
import org.switchyard.soap.util.WSDLUtil;

/**
 * Hanldes SOAP requests to invoke a SwitchYard service.
 */
public class InboundHandler extends BaseHandler {
    private static final Logger LOGGER = Logger.getLogger(InboundHandler.class);
    private static final long DEFAULT_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP = 100;

    private static ThreadLocal<SOAPMessage> _response = new ThreadLocal<SOAPMessage>();

    private MessageComposer _composer;
    private MessageDecomposer _decomposer;
    private ServiceDomain _domain;
    private String _wsdlLocation;
    private QName _serviceName;
    private long _waitTimeout = DEFAULT_TIMEOUT; // default of 15 seconds
    private Endpoint _endpoint;
    private String _endpointUrl;
    private String _wsName;
    private Port _port;

    /**
     * Constructor.
     * @param config the configuration settings
     */
    public InboundHandler(final HashMap<String, String> config) {
        String localService = config.get("localService");
        String context = config.get("context");
        String port = config.get("port");
        String composer = config.get("composer");
        String decomposer = config.get("decomposer");

        if (composer != null && composer.length() > 0) {
            try {
                Class<? extends MessageComposer> composerClass = Class.forName(composer).asSubclass(MessageComposer.class);
                _composer = composerClass.newInstance();
            } catch (Exception cnfe) {
                LOGGER.error("Could not instantiate composer", cnfe);
            }
        }
        if (_composer == null) {
            _composer = new DefaultMessageComposer();
        }
        if (decomposer != null && decomposer.length() > 0) {
            try {
                Class<? extends MessageDecomposer> decomposerClass = Class.forName(decomposer).asSubclass(MessageDecomposer.class);
                _decomposer = decomposerClass.newInstance();
            } catch (Exception cnfe) {
                LOGGER.error("Could not instantiate decomposer", cnfe);
            }
        }
        if (_decomposer == null) {
            _decomposer = new DefaultMessageDecomposer();
        }

        _domain = ServiceDomains.getDomain();
        _wsdlLocation = config.get("wsdlLocation");
        _serviceName = new QName(localService);
        if (port == null) {
            port = "8080";
        }
        if (context == null) {
            context = "";
        }
        _endpointUrl = "http://localhost:" + port + "/" + context;
    }

    /**
     * Start lifecycle.
     * @throws WebServicePublishException If unable to publish the endpoint
     */
    public void start() throws WebServicePublishException {
        try {
            Definition definition = WSDLUtil.readWSDL(_wsdlLocation);
            // Only first definition for now
            javax.wsdl.Service wsdlService = (javax.wsdl.Service) definition.getServices().values().iterator().next();
            String targetNamespace = definition.getTargetNamespace();
            _wsName = wsdlService.getQName().getLocalPart();
            // Only first port for now
            _port = (Port) wsdlService.getPorts().values().iterator().next();
            String portName = _port.getName();
            BaseWebService wsProvider = new BaseWebService();
            // Hook the handler
            wsProvider.setConsumer(this);

            _endpoint = Endpoint.create(wsProvider);
            List<Source> metadata = new ArrayList<Source>();
            StreamSource source = WSDLUtil.getStream(_wsdlLocation);
            metadata.add(source);
            _endpoint.setMetadata(metadata);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(Endpoint.WSDL_SERVICE, new QName(targetNamespace, _wsName));
            properties.put(Endpoint.WSDL_PORT, new QName(targetNamespace, portName));
            _endpoint.setProperties(properties);

            _endpoint.publish(_endpointUrl + _wsName);
            LOGGER.info("WebService published at " + _endpointUrl + _wsName);
        } catch (Exception e) {
            throw new WebServicePublishException(e);
        }
    }

    /**
     * Stop lifecycle.
     */
    public void stop() {
        _endpoint.stop();
        LOGGER.info("WebService " + _endpointUrl + _wsName + " stopped.");
    }

    /**
     * The handler method that handles responses from a WebService.
     * @param exchange the Exchange
     * @throws HandlerException handler exception
     */
    @Override
    public void handleMessage(final Exchange exchange) throws HandlerException {
        try {
            _response.set(_decomposer.decompose(exchange.getMessage()));
        } catch (SOAPException se) {
            // generate fault
            LOGGER.error(se);
        }
    }

    /**
     * The handler method that handles faults from a WebService.
     * @param exchange the Exchange
     */
    @Override
    public void handleFault(final Exchange exchange) {
        try {
            _response.set(_decomposer.decompose(exchange.getMessage()));
        } catch (SOAPException se) {
            // generate fault
            LOGGER.error(se);
        }
    }
        /*}
        catch (final WebServiceException wse) {
            throw wse;
        }
        catch (final Exception ex) {
            LOGGER.error(ex);
            try {
                SOAPMessage faultMsg = null;
                if (ex instanceof FaultMessageException) {
                    final FaultMessageException fme = (FaultMessageException) ex;
                    final Message faultMessage = fme.getReturnedMessage();
                    if (faultMessage != null) {
                        final Body body = faultMessage.getBody();
                        final QName faultCode = (QName)body.get(Fault.DETAIL_CODE_CONTENT);
                        final String faultDescription = (String)body.get(Fault.DETAIL_DESCRIPTION_CONTENT);
                        final String faultDetail = (String)body.get(Fault.DETAIL_DETAIL_CONTENT);

                        if (faultCode != null) {
                            faultMsg = SOAP_MESSAGE_FACTORY.createMessage();
                            final SOAPFault fault = faultMsg.getSOAPBody().addFault(faultCode, faultDescription);
                            if (faultDetail != null) {
                                try {
                                    final Document detailDoc = parseAsDom(faultDetail);
                                    final Detail detail = fault.addDetail();
                                    detail.appendChild(detailDoc.getDocumentElement());
                                }
                                catch (final Exception ex2) {
                                    LOGGER.warn("Failed to parse fault detail", ex2);
                                }
                            }
                        }
                        else {
                            final Throwable cause = fme.getCause();
                            faultMsg = (cause != null) ? generateFault(cause) : generateFault(ex);
                        }
                    }
                }

                if (faultMsg == null) {
                    faultMsg = generateFault(ex);
                }
                return faultMsg;
            }
            catch (final SOAPException soape) {
                throw new WebServiceException("Unexpected exception generating fault response", soape);
            }
        }
        finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }*/

    /**
     * The delegate method called by the Webservice implementation.
     * @param soapMessage the SOAP request
     * @return the SOAP response
     */
    public SOAPMessage invoke(final SOAPMessage soapMessage) {
        _response.remove();
        try {
            if (SOAPUtil.isMessageOneWay(_port, soapMessage)) {
                Exchange exchange = _domain.createExchange(_serviceName, ExchangePattern.IN_ONLY, this);
                Message message = _composer.compose(soapMessage);
                exchange.send(message);
            } else {
                Exchange exchange = _domain.createExchange(_serviceName, ExchangePattern.IN_OUT, this);
                Message message = _composer.compose(soapMessage);
                exchange.send(message);
                waitForResponse();
            }
        } catch (SOAPException se) {
            // generate fault
            LOGGER.error(se);
        }
        SOAPMessage response = _response.get();
        _response.remove();
        return response;
    }

    /**
     * Sleep until we get a response or timeout has reached.
     */
    private void waitForResponse() {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() < start + _waitTimeout) {
            if (_response.get() != null) {
                return;
            }
            try {
                Thread.sleep(DEFAULT_SLEEP);
            } catch (InterruptedException e) { 
                //ignore
            }
        }
    }
}
