package com.chameleonvision.network;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxNetworking extends SysNetworking {
    private static final String PATH = "/etc/dhcpcd.conf";

    @Override
    public boolean setDHCP() {
        File dhcpConf = new File(PATH);
        if (dhcpConf.exists()) {
            try {
                List<String> lines = FileUtils.readLines(dhcpConf, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith("interface " + networkInterface.name)) {
                        lines.remove(i);
                        for (int j = i; j < lines.size(); j++) {
                            String subInterface = lines.get(j);
                            if (subInterface.contains("static ip_address") || subInterface.contains("static routers")) {
                                lines.remove(j);
                                j--;
                            }
                            if (subInterface.contains("interface")) {
                                break;
                            }
                        }
                        FileUtils.writeLines(dhcpConf, lines);
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        } else {
            System.err.println("dhcpcd5 is not installed cant set ip");
            return false;
        }
        return true;
    }

    @Override
    public boolean setHostname(String newHostname) {
        String[] setHostnameArgs = {"set-hostname", newHostname};
        try {
            var setHostnameRetCode = shell.execute("hostnamectl", setHostnameArgs);
            return setHostnameRetCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean setStatic(String ipAddress, String netmask, String gateway) {
        setDHCP(); // clean up old static interface
        File dhcpConf = new File(PATH);
        try {
            List<String> lines = FileUtils.readLines(dhcpConf, StandardCharsets.UTF_8);
            lines.add("interface " + networkInterface.name);
            InetAddress iNetMask = InetAddress.getByName(netmask);
            int prefix = NetmaskToCIDR.convertNetmaskToCIDR(iNetMask);
            lines.add("static ip_address " + ipAddress + "/" + prefix);
            lines.add("static routers " + gateway);
            FileUtils.writeLines(dhcpConf, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException {
        List<java.net.NetworkInterface> netInterfaces;
        try {
            netInterfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return null;
        }
        List<java.net.NetworkInterface> goodInterfaces = new ArrayList<>();

        for (var netInterface : netInterfaces) {
            if (netInterface.getDisplayName().contains("lo")) continue;
            if (!netInterface.isUp()) continue;
            goodInterfaces.add(netInterface);
        }
        return goodInterfaces;

    }
}
