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

import org.switchyard.BaseHandler;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.HandlerException;
import org.switchyard.Message;
import org.switchyard.MessageBuilder;
import org.switchyard.message.FaultMessage;

public class SOAPProvider extends BaseHandler {

    @Override
    public void handleMessage(Exchange exchange) throws HandlerException {
        if (exchange.getPattern().equals(ExchangePattern.IN_OUT)) {
            Message message;
            String request = exchange.getMessage().getContent(String.class);
            String toWhom = "";
            int argIdx = request.indexOf("<arg0>");
            if (argIdx > 0) {
                toWhom = request.substring(argIdx + 6, request.indexOf("</arg0>"));
            }
            String response = null;
            if (toWhom.length() == 0) {
                message = MessageBuilder.newInstance(FaultMessage.class).buildMessage();
                response = "<soap:fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                            + "   <faultcode>soap:Server.AppError</faultcode>"
                            + "   <faultstring>Invalid name</faultstring>"
                            + "   <detail>"
                            + "      <message>Looks like you did not specify a name!</message>"
                            + "      <errorcode>1000</errorcode>"
                            + "   </detail>"
                            + "</soap:fault>";
            } else {
                message = MessageBuilder.newInstance().buildMessage();
                response = "<test:sayHelloResponse xmlns:test=\"http://test.ws/\">"
                             + "   <return>Hello " + toWhom + "</return>"
                             + "</test:sayHelloResponse>";
            }

            message.setContent(response);
            exchange.send(message);
        }
    }
}