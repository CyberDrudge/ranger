/*
 * Copyright 2015 Flipkart Internet Pvt. Ltd.
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
package io.appform.ranger.drove.server.bundle.config;

import io.appform.ranger.client.RangerClientConstants;
import io.appform.ranger.drove.config.DroveConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RangerDroveConfiguration {
    public static final String DEFAULT_ENVIRONMENT_TAG_NAME = "environment";
    public static final String DEFAULT_REGION_TAG_NAME = "region";

    @NotEmpty
    @NotNull
    String namespace;
    @NotEmpty
    @NotNull
    List<DroveConfig> droveConfigs;

    String environmentTagName;
    String regionTagName;

    @Min(RangerClientConstants.MINIMUM_REFRESH_TIME)
    int nodeRefreshTimeMs = RangerClientConstants.MINIMUM_REFRESH_TIME;
}
