/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.routing.handlers

import com.hotels.styx.api.Id.id
import com.hotels.styx.api.{HttpClient, HttpRequest, HttpResponse, Id}
import com.hotels.styx.client.applications.BackendService
import com.hotels.styx.infrastructure.configuration.yaml.YamlConfig
import com.hotels.styx.proxy.BackendServiceClientFactory
import com.hotels.styx.routing.config.RoutingConfigDefinition
import io.netty.handler.codec.http.HttpResponseStatus.OK
import org.scalatest.{FunSpec, ShouldMatchers}
import rx.Observable

import scala.collection.JavaConverters._

class ProxyToBackendSpec extends FunSpec with ShouldMatchers {

  private val config = configBlock(
    """
      |config:
      |    name: ProxyToBackend
      |    type: ProxyToBackend
      |    config:
      |      backend:
      |        id: "ba"
      |        connectionPool:
      |          maxConnectionsPerHost: 45
      |          maxPendingConnectionsPerHost: 15
      |        responseTimeoutMillis: 60000
      |        origins:
      |        - { id: "ba1", host: "localhost:9094" }
      |
      |""".stripMargin)

  it("builds ProxyToBackend handler") {
    val handler = new ProxyToBackend.ConfigFactory(clientFactory()).build(List().asJava, null, config)

    val response = handler.handle(HttpRequest.Builder.get("/foo")
      .build(), null).toBlocking.first()
    response.status should be (OK)
  }

  it("throws for missing mandatory 'backend' attribute") {
    val config = configBlock(
      """
        |config:
        |    name: myProxy
        |    type: ProxyToBackend
        |    config:
        |      na: na
        |""".stripMargin)

    val e = intercept[IllegalArgumentException] {
      val handler = new ProxyToBackend.ConfigFactory(clientFactory())
        .build(List("config", "config").asJava, null, config)
    }

    e.getMessage should be("Routing object definition of type 'ProxyToBackend', attribute='config.config', is missing a mandatory 'backend' attribute.")
  }

  it("throws for a missing mandatory backend.origins attribute") {
    val config = configBlock(
      """
        |config:
        |    name: ProxyToBackend
        |    type: ProxyToBackend
        |    config:
        |      backend:
        |        id: "ba"
        |        connectionPool:
        |          maxConnectionsPerHost: 45
        |          maxPendingConnectionsPerHost: 15
        |        responseTimeoutMillis: 60000
        |""".stripMargin)

    val e = intercept[IllegalArgumentException] {
      val handler = new ProxyToBackend.ConfigFactory(clientFactory())
        .build(List("config", "config").asJava, null, config)
    }

    e.getMessage should be("Routing object definition of type 'ProxyToBackend', attribute='config.config.backend', is missing a mandatory 'origins' attribute.")
  }

  private def configBlock(text: String) = new YamlConfig(text).get("config", classOf[RoutingConfigDefinition]).get()

  private def clientFactory() = new BackendServiceClientFactory() {
    override def createClient(backendService: BackendService): HttpClient = new HttpClient {
      override def sendRequest(request: HttpRequest): Observable[HttpResponse] = {
        backendService.id() should be (id("ba"))
        backendService.connectionPoolConfig().maxConnectionsPerHost() should be (45)
        backendService.connectionPoolConfig().maxPendingConnectionsPerHost() should be (15)
        backendService.responseTimeoutMillis() should be (60000)
        backendService.origins().asScala.head.id() should be(id("ba1"))
        backendService.origins().asScala.head.host().getPort should be(9094)
        Observable
          .just(HttpResponse.Builder
            .response(OK)
            .addHeader("X-Backend-Service", backendService.id())
            .build()
          )
      }
    }
  }

}
