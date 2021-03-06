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
package com.hotels.styx.admin.handlers;

import com.hotels.styx.StyxConfig;
import com.hotels.styx.support.api.BlockingObservables;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.server.HttpInterceptorContext;
import org.testng.annotations.Test;
import rx.Observable;

import static com.hotels.styx.api.HttpRequest.Builder.get;
import static com.hotels.styx.support.ResourcePaths.fixturesHome;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StyxConfigurationHandlerTest {
    private static final String ORIGINS_FILE = fixturesHome() + "conf/origins/origins-development.yml";

    @Test
    public void outputsConfiguration() {
        String yaml = "" +
                "proxy:\n" +
                "  connectors:\n" +
                "    http:\n" +
                "      port: 8080\n" +
                "loadBalancing:\n" +
                "  strategy: ROUND_ROBIN\n" +
                "originsFile: " + ORIGINS_FILE + "\n";

        String adminPage = BlockingObservables.responseBody(browseForCurrentConfiguration(yaml, false));

        assertThat(adminPage, is("{\"proxy\":{\"connectors\":{\"http\":{\"port\":8080}}},\"loadBalancing\":{\"strategy\":\"ROUND_ROBIN\"},\"originsFile\":\"" + ORIGINS_FILE + "\"}\n"));
    }

    @Test
    public void outputsPrettifiedConfiguration() throws Exception {
        String yaml = "" +
                "proxy:\n" +
                "  connectors:\n" +
                "    http:\n" +
                "      port: 8080\n" +
                "loadBalancing:\n" +
                "  strategy: ROUND_ROBIN\n" +
                "originsFile: " + ORIGINS_FILE + "\n";

        String adminPage = BlockingObservables.responseBody(browseForCurrentConfiguration(yaml, true));

        assertThat(adminPage, is("{\n" +
                        "  \"proxy\" : {\n" +
                        "    \"connectors\" : {\n" +
                        "      \"http\" : {\n" +
                        "        \"port\" : 8080\n" +
                        "      }\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"loadBalancing\" : {\n" +
                        "    \"strategy\" : \"ROUND_ROBIN\"\n" +
                        "  },\n" +
                        "  \"originsFile\" : \"" + ORIGINS_FILE + "\"\n" +
                        "}"));
    }

    private static Observable<HttpResponse> browseForCurrentConfiguration(String yaml, boolean pretty) {
        return configurationBrowserHandler(yaml).handle(adminRequest(pretty));
    }

    private static HttpRequest adminRequest(boolean pretty) {
        if (pretty) {
            return get("/?pretty=").build();
        } else {
            return get("/").build();
        }
    }

    private static StyxConfigurationHandler configurationBrowserHandler(String yaml) {
        return new StyxConfigurationHandler(new StyxConfig(yaml));
    }
}
