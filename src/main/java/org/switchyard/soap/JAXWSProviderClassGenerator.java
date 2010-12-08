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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.annotation.Annotation;
//import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;

/**
 * Generates a JAXWS Provider implementation based on service parameters.
 */
public class JAXWSProviderClassGenerator {
    private final ClassPool _pool;
    private final CtClass _superClass;

    /**
     * Constructor.
     * @throws WebServicePublishException If the WebService Class could not be created
     */
    public JAXWSProviderClassGenerator()
            throws WebServicePublishException {
        _pool = new ClassPool();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            _pool.appendClassPath(new LoaderClassPath(tccl));
        }
        _pool.appendClassPath(new LoaderClassPath(JAXWSProviderClassGenerator.class.getClassLoader()));
        try {
            _superClass = _pool.get(BaseWebService.class.getName());
        } catch (final NotFoundException nfe) {
            throw new WebServicePublishException("Failed to obtain superclasses", nfe);
        }
    }

    /**
     * Create a Class that can be used as a WebService Provider.
     * @param serviceName the WebService's name
     * @param portName the WebService's port name
     * @param targetNamespace the WebService's namespace
     * @return the generated Class
     * @throws WebServicePublishException If the WebService Class could not be created
     */
    public Class generate(final String serviceName, final String portName, final String targetNamespace)
            throws WebServicePublishException {
        String className = "org.switchyard.soap.ws." + serviceName + "Impl";
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Class providerClass = tccl.loadClass(className);
            return providerClass;
        } catch (final ClassNotFoundException cnfe) {
             // ignore
        }
        try {
            _pool.get(className);
            throw new WebServicePublishException("Duplicate class within context: " + className);
        } catch (final NotFoundException nfe) {
             // ignore
        }
        
        final CtClass seiClass = _pool.makeClass(className);

        try {
            seiClass.setSuperclass(_superClass);

            ConstPool constantPool = seiClass.getClassFile().getConstPool();

            final String superClassName = seiClass.getSuperclass().getName().replace('.', '/');
            final String interfaceName = Provider.class.getName().replace('.', '/');
            final String typeName = SOAPMessage.class.getName().replace('.', '/');
            final String signature = 'L' + superClassName + ';' + 'L' + interfaceName + "<L" + typeName + ";>;";
            final SignatureAttribute signatureAttribute = new SignatureAttribute(constantPool, signature);
            seiClass.getClassFile().addAttribute(signatureAttribute);

            AnnotationsAttribute attribute = new AnnotationsAttribute(
                    constantPool, AnnotationsAttribute.visibleTag);
            Annotation annotation = new Annotation(
                    "javax.xml.ws.WebServiceProvider", constantPool);
            /*StringMemberValue strValue1 = new StringMemberValue(constantPool);
            strValue1.setValue(wsdlLocation);
            annotation.addMemberValue("wsdlLocation", strValue1);*/
            StringMemberValue strValue2 = new StringMemberValue(constantPool);
            strValue2.setValue(serviceName);
            annotation.addMemberValue("serviceName", strValue2);
            StringMemberValue strValue3 = new StringMemberValue(constantPool);
            strValue3.setValue(portName);
            annotation.addMemberValue("portName", strValue3);

            StringMemberValue strValue4 = new StringMemberValue(constantPool);
            strValue4.setValue(targetNamespace);
            annotation.addMemberValue("targetNamespace", strValue4);

            attribute.addAnnotation(annotation);

            Annotation annotation2 = new Annotation("javax.xml.ws.ServiceMode",
                    constantPool);
            EnumMemberValue enumValue = new EnumMemberValue(constantPool);
            enumValue.setType("javax.xml.ws.Service$Mode");
            enumValue.setValue("MESSAGE");
            annotation2.addMemberValue("value", enumValue);
            attribute.addAnnotation(annotation2);

            /*if (epInfo.isAddressing() && JBossDeployerUtil.getWSImpl().equals(JBossDeployerUtil.WSIMPL_CXF))
            {
                Annotation annotation3 = new Annotation("javax.xml.ws.soap.Addressing", constantPool);
                BooleanMemberValue boolEnabled = new BooleanMemberValue(constantPool);
                boolEnabled.setValue(true);
                BooleanMemberValue boolRequired = new BooleanMemberValue(constantPool);
                boolRequired.setValue(true);
                annotation3.addMemberValue("enabled", boolEnabled);
                annotation3.addMemberValue("required", boolEnabled);
                attribute.addAnnotation(annotation3);
            }

            if (includeHandlers)
            {
                final Annotation handlerChainAnnotation = new Annotation("javax.jws.HandlerChain", constantPool);
                final StringMemberValue handlerValue = new StringMemberValue(constantPool);
                handlerValue.setValue("esb-jaxws-handlers.xml");
                handlerChainAnnotation.addMemberValue("file", handlerValue);
                attribute.addAnnotation(handlerChainAnnotation);
            }*/
            
            seiClass.getClassFile().addAttribute(attribute);

            final String constructorStr = "super();";
            CtConstructor defaultConstructor = new CtConstructor(null, seiClass);
            defaultConstructor.setBody(constructorStr);
            seiClass.addConstructor(defaultConstructor);
            //seiClass.writeFile();

            return seiClass.toClass();
        } catch (Exception e) {
            throw new WebServicePublishException(
                    "Failed to generate jaxws dispatch class for service",
                    e);
        }
    }

    private String getParamValue(final String value) {
        if (value == null) {
            return "null";
        } else {
            return '"' + value + '"';
        }
    }
}
