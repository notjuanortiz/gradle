/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.cache.internal.locklistener;

import org.gradle.internal.net.InetAddressFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Set;

import static org.gradle.cache.internal.locklistener.FileLockPacketType.LOCK_RELEASE_CONFIRMATION;
import static org.gradle.cache.internal.locklistener.FileLockPacketType.UNLOCK_REQUEST;
import static org.gradle.cache.internal.locklistener.FileLockPacketType.UNLOCK_REQUEST_CONFIRMATION;
import static org.gradle.internal.UncheckedException.throwAsUncheckedException;

public class FileLockCommunicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileLockCommunicator.class);
    private static final String SOCKET_OPERATION_NOT_PERMITTED_ERROR_MESSAGE = "Operation not permitted";

    private final DatagramSocket socket;
    private final InetAddressFactory addressFactory;
    private volatile boolean stopped;

    public FileLockCommunicator(InetAddressFactory addressFactory) {
        this.addressFactory = addressFactory;
        try {
            socket = new DatagramSocket(0, addressFactory.getLocalBindingAddress());
        } catch (SocketException e) {
            throw throwAsUncheckedException(e);
        }
    }

    public boolean pingOwner(int ownerPort, long lockId, String displayName) {
        try {
            byte[] bytesToSend = FileLockPacketPayload.encode(lockId, UNLOCK_REQUEST);
            // Ping the owner via all available local addresses
            for (InetAddress address : addressFactory.getCommunicationAddresses()) {
                try {
                    socket.send(new DatagramPacket(bytesToSend, bytesToSend.length, address, ownerPort));
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().startsWith(SOCKET_OPERATION_NOT_PERMITTED_ERROR_MESSAGE)) {
                        LOGGER.debug("Failed attempt to ping owner of lock for {} (lock id: {}, port: {}, address: {})", displayName, lockId, ownerPort, address);
                        return false;
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to ping owner of lock for %s (lock id: %s, port: %s)", displayName, lockId, ownerPort), e);
        }
        return true;
    }

    public DatagramPacket receive() throws GracefullyStoppedException {
        try {
            byte[] bytes = new byte[FileLockPacketPayload.MAX_BYTES];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            socket.receive(packet);
            return packet;
        } catch (IOException e) {
            if (!stopped) {
                throw new RuntimeException(e);
            }
            throw new GracefullyStoppedException();
        }
    }

    public FileLockPacketPayload decode(DatagramPacket receivedPacket) {
        try {
            return FileLockPacketPayload.decode(receivedPacket.getData(), receivedPacket.getLength());
        } catch (IOException e) {
            if (!stopped) {
                throw new RuntimeException(e);
            }
            throw new GracefullyStoppedException();
        }
    }

    public void confirmUnlockRequest(SocketAddress address, long lockId) {
        try {
            byte[] bytes = FileLockPacketPayload.encode(lockId, UNLOCK_REQUEST_CONFIRMATION);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            packet.setSocketAddress(address);
            socket.send(packet);
        } catch (IOException e) {
            if (!stopped) {
                throw new RuntimeException(e);
            }
            throw new GracefullyStoppedException();
        }
    }

    public void confirmLockRelease(Set<SocketAddress> addresses, long lockId) {
        byte[] bytes = FileLockPacketPayload.encode(lockId, LOCK_RELEASE_CONFIRMATION);
        for (SocketAddress address : addresses) {
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            packet.setSocketAddress(address);
            LOGGER.debug("Confirming lock release to Gradle process at port {} for lock with id {}.", packet.getPort(), lockId);
            try {
                socket.send(packet);
            } catch (IOException e) {
                if (!stopped) {
                    LOGGER.debug("Failed to confirm lock release to Gradle process at port {} for lock with id {}.", packet.getPort(), lockId);
                }
            }
        }
    }

    public void stop() {
        stopped = true;
        socket.close();
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}
