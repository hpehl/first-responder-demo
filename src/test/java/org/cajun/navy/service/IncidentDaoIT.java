/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cajun.navy.service;

import org.cajun.navy.model.incident.IncidentDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
public class IncidentDaoIT {

    @Deployment
    public static WebArchive deployment() throws IllegalArgumentException {
        return new DefaultDeployment()
                .withPersistence()
                .withImportedData()
                .getArchive()
//                .addClasses(Resources.class, IncidentDao.class, IncidentDaoImpl.class)
                .addPackages(true,"org.cajun.navy")
                ;
    }

    @Inject
    private IncidentDao incidentDao;


    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void sanity() {
        assert incidentDao != null;
    }
}
