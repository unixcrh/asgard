/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.asgard

import com.netflix.asgard.mock.Mocks
import grails.test.ControllerUnitTestCase
import grails.test.MockUtils

class LoadBalancerControllerTests extends ControllerUnitTestCase {

    void setUp() {
        super.setUp()
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(LoadBalancerCreateCommand)
        controller.awsLoadBalancerService = Mocks.awsLoadBalancerService()
        controller.applicationService = Mocks.applicationService()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
    }

    void testShow() {
        controller.params.id = 'ntsuiboot--frontend'
        def attrs = controller.show()
        def loadBalancer = attrs['loadBalancer']
        assert 'ntsuiboot--frontend' == loadBalancer.loadBalancerName
        assert 'ntsuiboot' == attrs.app.name
        assert 'ntsuiboot-v000' == attrs['groups'][0].autoScalingGroupName
        assert 1 == attrs['instanceStates'].size()
    }

    void testShowNonExistent() {
        controller.params.name ='doesntexist'
        controller.show()
        assert '/error/missing' == controller.renderArgs.view
        assert "Load Balancer 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    void testSaveWithoutMostParams() {
        mockRequest.method = 'POST'
        def p = controller.params
        p.appName = 'helloworld'
        def cmd = new LoadBalancerCreateCommand(appName: p.appName)
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert cmd.hasErrors()
        controller.save(cmd)
        assert controller.create == controller.chainArgs.action
        assert p == controller.chainArgs.params
        assert cmd == controller.chainArgs.model['cmd']
    }

    void testSaveSuccessfully() {
        mockRequest.method = 'POST'
        def p = mockParams
        p.appName = 'helloworld'
        p.stack = 'unittest'
        p.detail ='frontend'
        p.protocol1 = 'HTTP'
        p.lbPort1 = '80'
        p.instancePort1 = '7001'
        p.target = 'HTTP:7001/healthcheck'
        p.interval = '40'
        p.timeout = '40'
        p.unhealthy = '40'
        p.healthy = '40'
        LoadBalancerCreateCommand cmd = new LoadBalancerCreateCommand(appName: p.appName, stack: p.stack,
                detail: p.detail, protocol1: p.protocol1, lbPort1: p.lbPort1 as Integer,
                instancePort1: p.instancePort1 as Integer, target: p.target, interval: p.interval as Integer,
                timeout: p.timeout as Integer, unhealthy: p.unhealthy as Integer, healthy: p.healthy as Integer)
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert !cmd.hasErrors()
        controller.save(cmd)
        assert controller.show == controller.redirectArgs.action
        assert 'helloworld-unittest-frontend' == controller.redirectArgs.params.name
        assert controller.flash.message.startsWith("Load Balancer 'helloworld-unittest-frontend' has been created.")
    }

}
