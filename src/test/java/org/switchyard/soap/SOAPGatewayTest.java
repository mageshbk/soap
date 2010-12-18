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

import java.io.File;
import java.util.HashMap;
import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.Message;
import org.switchyard.MessageBuilder;
import org.switchyard.MockHandler;
import org.switchyard.ServiceDomain;
import org.switchyard.internal.ServiceDomains;
import org.switchyard.soap.util.SOAPUtil;
import org.switchyard.soap.util.XMLHelper;

import org.w3c.dom.Element;

public class SOAPGatewayTest {
    private static final QName PUBLISH_AS_WS_SERVICE = new QName("publish-as-ws");
    private static final QName WS_CONSUMER_SERVICE = new QName("webservice-consumer");

    private static ServiceDomain _domain;
    private static SOAPGateway _soapInbound;
    private static SOAPGateway _soapOutbound;

    @BeforeClass
    public static void setUp() throws Exception {
        // Provide a switchyard service
        _domain = ServiceDomains.getDomain();
        SOAPProvider provider = new SOAPProvider();
        _domain.registerService(PUBLISH_AS_WS_SERVICE, provider);

        String port = System.getProperty("org.switchyard.saop.webservice.port");

        // Service exposed as WS
        _soapInbound = new SOAPGateway();
        HashMap config = new HashMap();
        config.put("publishAsWS", "true");
        config.put("wsdlLocation", "target/test-classes/HelloWebService.wsdl");
        config.put("localService", PUBLISH_AS_WS_SERVICE.getLocalPart());
        config.put("port", port);
        _soapInbound.init(config);
        _soapInbound.start();

        // A WS Consumer as Service
        _soapOutbound = new SOAPGateway();
        config = new HashMap();
        config.put("remoteWSDL", "http://localhost:" + port + "/HelloWebService?wsdl");
        config.put("serviceName", WS_CONSUMER_SERVICE.getLocalPart());
        _soapOutbound.init(config);
        _soapOutbound.start();
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        _soapOutbound.stop();
        _soapInbound.stop();
        _soapInbound.destroy();
        _soapOutbound.destroy();
    }

    @Test
    public void invokeOneWay() throws Exception {
        Element input = SOAPUtil.parseAsDom("<test:helloWS xmlns:test=\"http://test.ws/\">"
                     + "   <arg0>Hello</arg0>"
                     + "</test:helloWS>").getDocumentElement();

        // Invoke the WS via our WS Consumer service
        MockHandler consumer = new MockHandler();
        Exchange exchange = _domain.createExchange(WS_CONSUMER_SERVICE, ExchangePattern.IN_ONLY, consumer);
        Message message = MessageBuilder.newInstance().buildMessage();
        message.setContent(input);
        exchange.send(message);
    }

    @Test
    public void invokeRequestResponse() throws Exception {
        Element input = SOAPUtil.parseAsDom("<test:sayHello xmlns:test=\"http://test.ws/\">"
                     + "   <arg0>Jimbo</arg0>"
                     + "</test:sayHello>").getDocumentElement();

        Element output = SOAPUtil.parseAsDom("<test:sayHelloResponse xmlns:test=\"http://test.ws/\">"
                     + "   <return>Hello Jimbo</return>"
                     + "</test:sayHelloResponse>").getDocumentElement();

        // Invoke the WS via our WS Consumer service
        MockHandler consumer = new MockHandler();
        Exchange exchange = _domain.createExchange(WS_CONSUMER_SERVICE, ExchangePattern.IN_OUT, consumer);
        Message message = MessageBuilder.newInstance().buildMessage();
        message.setContent(input);
        exchange.send(message);
        consumer.waitForMessage();
        Element response = consumer.getMessages().peek().getMessage().getContent(Element.class);
        Assert.assertTrue("Expected \r\n" + XMLHelper.toString(output) + "\r\nbut was \r\n" + XMLHelper.toString(response), XMLHelper.compareXMLContent(output, response));
    }

    @Test
    public void invokeRequestResponseFault() throws Exception {
        Element input = SOAPUtil.parseAsDom("<test:sayHello xmlns:test=\"http://test.ws/\">"
                     + "   <arg0></arg0>"
                     + "</test:sayHello>").getDocumentElement();

        Element output = SOAPUtil.parseAsDom("<soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                        + "   <faultcode>soap:Server.AppError</faultcode>"
                        + "   <faultstring>Invalid name</faultstring>"
                        + "   <detail>"
                        + "      <message>Looks like you did not specify a name!</message>"
                        + "      <errorcode>1000</errorcode>"
                        + "   </detail>"
                        + "</soap:Fault>").getDocumentElement();

        // Invoke the WS via our WS Consumer service
        MockHandler consumer = new MockHandler();
        Exchange exchange = _domain.createExchange(WS_CONSUMER_SERVICE, ExchangePattern.IN_OUT, consumer);
        Message message = MessageBuilder.newInstance().buildMessage();
        message.setContent(input);
        exchange.send(message);
        consumer.waitForMessage();
        Element response = consumer.getMessages().peek().getMessage().getContent(Element.class);
        Assert.assertTrue("Expected \r\n" + XMLHelper.toString(output) + "\r\nbut was \r\n" + XMLHelper.toString(response), XMLHelper.compareXMLContent(output, response));
    }
}