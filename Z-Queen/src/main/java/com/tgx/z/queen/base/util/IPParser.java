/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.z.queen.base.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * @author Unknow
 */
public class IPParser
{
    /*
     * Using regex to ensure that the address is a valid one. This allows for separating by format and ensures that the operations done on a
     * format will be valid.
     */
    // 0.0.0.0-255.255.255.255
    private final static String ipv4segment         = "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])";

    // 0-65535
    private final static String portsegment         = ":(?:6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|"
                                                      + "6[0-4][0-9]{3}|[1-5][0-9]{4}|[1-9][0-9]{1,3}|[0-9])";
    private final static String ipv4address         = "(" + ipv4segment + "\\.){3,3}" + ipv4segment;
    private final static String ipv4addressWithPort = ipv4address + portsegment + "?";
    private final static String ipv6segment         = "[a-fA-F0-9]{1,4}";
    private final static String ipv6address         = "(" +
    // 1:2:3:4:5:6:7:8
                                                      "(" + ipv6segment + ":){7,7}" + ipv6segment + "|" +
                                                      // 1::, 1:2:3:4:5:6:7::
                                                      "(" + ipv6segment + ":){1,7}:|" +
                                                      // 1::8, 1:2:3:4:5:6::8,
                                                      // 1:2:3:4:5:6::8
                                                      "(" + ipv6segment + ":){1,6}:" + ipv6segment + "|" +
                                                      // 1::7:8, 1:2:3:4:5::7:8,
                                                      // 1:2:3:4:5::8
                                                      "(" + ipv6segment + ":){1,5}(:" + ipv6segment + "){1,2}|" +
                                                      // 1::6:7:8,
                                                      // 1:2:3:4::6:7:8,
                                                      // 1:2:3:4::8
                                                      "(" + ipv6segment + ":){1,4}(:" + ipv6segment + "){1,3}|" +
                                                      // 1::5:6:7:8,
                                                      // 1:2:3::5:6:7:8,
                                                      // 1:2:3::8
                                                      "(" + ipv6segment + ":){1,3}(:" + ipv6segment + "){1,4}|" +
                                                      // # 1::4:5:6:7:8,
                                                      // 1:2::4:5:6:7:8, 1:2::8
                                                      "(" + ipv6segment + ":){1,2}(:" + ipv6segment + "){1,5}|" +
                                                      // # 1::3:4:5:6:7:8,
                                                      // 1::3:4:5:6:7:8, 1::8
                                                      ipv6segment + ":((:" + ipv6segment + "){1,6})|" +
                                                      // ::2:3:4:5:6:7:8,
                                                      // ::2:3:4:5:6:7:8, ::8,
                                                      // ::
                                                      ":((:" + ipv6segment + "){1,7}|:)|" +
                                                      // fe80::7:8%eth0,
                                                      // fe80::7:8%1 (link-local
                                                      // IPv6 addresses with
                                                      // zone index)
                                                      "fe80:(:" + ipv6segment + "){0,4}%[0-9a-zA-Z]{1,}|" +
                                                      // ::255.255.255.255,
                                                      // ::ffff:255.255.255.255,
                                                      // ::ffff:0:255.255.255.255
                                                      // (IPv4-mapped IPv6
                                                      // addresses and
                                                      // IPv4-translated
                                                      // addresses)
                                                      "::(ffff(:0{1,4}){0,1}:){0,1}" + ipv4address + "|" +
                                                      // 2001:db8:3:4::192.0.2.33,
                                                      // 64:ff9b::192.0.2.33
                                                      // (IPv4-Embedded
                                                      // IPv6 Address)
                                                      "(" + ipv6segment + ":){1,4}:" + ipv4address + ")";

    private final static String ipv6addressWithPort = "\\[" + ipv6address + "\\]" + portsegment + "?";

    /**
     * Parses ipv4 and ipv6 addresses. Emits each described IP address as a hexadecimal integer representing the address, the address space,
     * and the port number specified, if any.
     * 
     * @param address
     *            the address to analyze
     */
    public static Pair<InetAddress, Integer> parse(String address) {

        // Try to match the pattern with one of the 2 types, with or without a
        // port
        if (Pattern.matches("^" + ipv4address + "$", address)) {
            try {
                return new Pair<>(Inet4Address.getByName(address), 0);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }
        else if (Pattern.matches("^" + ipv4addressWithPort + "$", address)) {
            String[] parts = address.split("\\.");
            int port = Integer.parseInt(parts[3].split(":")[1]);
            parts[3] = parts[3].split(":")[0];

            try {
                address = parts[0] + '.' + parts[1] + '.' + parts[2] + '.' + parts[3];
                return new Pair<>(Inet4Address.getByName(address), port);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        else if (Pattern.matches("^" + ipv6address + "$", address)) {

            try {
                return new Pair<>(Inet6Address.getByName(address), 0);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        else if (Pattern.matches("^" + ipv6addressWithPort + "$", address)) {
            String[] parts = address.split(":");
            address = address.replace("[", "").replace("]", "").replaceAll(portsegment + "$", "");
            try {
                return new Pair<>(Inet6Address.getByName(address), Integer.parseInt(parts[parts.length - 1]));
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        else {
            String[] parts = address.split(":");
            try {
                return new Pair<>(Inet6Address.getByName(parts[0]), Integer.parseInt(parts[parts.length - 1]));
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public final static String ipv4Join(String ip, int port) {
        return new StringBuilder(ip).append(':').append(port).toString();
    }

    public static void main(String[] args) {
        Pair<InetAddress, Integer> result = parse("localhost:5226");
        System.out.println(result.first().getHostAddress());
    }

}
