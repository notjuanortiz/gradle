/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.net;

import java.net.InetAddress;
import java.util.List;

public interface InetAddressFactory {
    String getHostname();

    /**
     * Determines if the IP address can be used for communication with this machine
     */
    boolean isCommunicationAddress(InetAddress address);

    /**
     * Locates the possible IP addresses which can be used to communicate with this machine.
     *
     * Loopback addresses are preferred.
     */
    List<InetAddress> getCommunicationAddresses();

    InetAddress getLocalBindingAddress();
}
