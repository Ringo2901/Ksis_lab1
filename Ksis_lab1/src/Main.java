import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        try {
            start();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
    public static void showPCMACs() {
        Enumeration<NetworkInterface> interfaces;
        try {
            int num = 1;
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface inter = interfaces.nextElement();
                final byte[] hardwareAddress = inter.getHardwareAddress();
                if (hardwareAddress != null) {
                    String[] hexadecimalFormat = new String[hardwareAddress.length];
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
                    }
                    System.out.print(num + "\t" + String.join("-", hexadecimalFormat)+"\t");
                    InterfaceAddress host = inter.getInterfaceAddresses().get(0);
                    System.out.print(host.getAddress().toString()+"\t");
                    System.out.println(inter.getDisplayName());
                    num++;
                }
            }
            System.out.println();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static int getScanInterface() {
        Enumeration<NetworkInterface> interfaces;
        InterfaceAddress host = null;
        try {
            System.out.println("Number of interface to scan:");
            int number;
            Scanner in = new Scanner(System.in);
            number = in.nextInt();
            int num = 1;
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface inter = interfaces.nextElement();
                final byte[] hardwareAddress = inter.getHardwareAddress();
                if (hardwareAddress != null) {
                    if (num==number)
                    {
                        host = inter.getInterfaceAddresses().get(0);
                        break;
                    }
                    num++;
                }
            }
            assert host != null;
            return createIPReadingForm(host.getAddress());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static void start() throws UnknownHostException, SocketException {
        showPCMACs();
        System.out.println();
        Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        int ip = getScanInterface();

        System.out.println();

        int mask = -1;
        while (interfaceEnumeration.hasMoreElements()) {
            mask = takeMaskValue(interfaceEnumeration, mask);
        }

        showMACAddresses(ip, mask);
    }

    private static int takeMaskValue(Enumeration<NetworkInterface> interfaceEnumeration, int oldValue) throws SocketException {
        NetworkInterface current = interfaceEnumeration.nextElement();
        if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
            return oldValue;
        }
        InterfaceAddress host = current.getInterfaceAddresses().get(0);
        System.out.println(current.getDisplayName());
        return host.getNetworkPrefixLength();
    }

    private static void showMACAddresses(int ip, int mask) {
        int binaryMask = createBinaryMask(mask);
        int address = ip & binaryMask;
        int amount = ~binaryMask - 1;
        showingValidConnections(address, amount);
    }

    private static int createIPReadingForm(InetAddress localHost) {
        byte[] IP_address = localHost.getAddress();
        int ip = IP_address[0] & 255;
        ip <<= 8;
        ip += IP_address[1] & 255;
        ip <<= 8;
        ip += IP_address[2] & 255;
        ip <<= 8;
        ip += IP_address[3] & 255;
        return ip;
    }

    private static int createBinaryMask(int mask) {
        int binaryMask = 0;
        for (int i = 0; i < 31; i++) {
            if (i < mask) {
                binaryMask++;
            }
            binaryMask <<= 1;
        }
        return binaryMask;
    }

    private static void showingValidConnections(int address, int amount) {
        int timeout = 500;
        System.out.println();
        for (int i = 1; i <= amount; i++) {
            long outerAddress = address + i;
            String anotherApi = createIP(outerAddress);
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(anotherApi);
                try {
                    boolean state = inetAddress.isReachable(timeout);
                    System.out.println("IP address: " + inetAddress.getHostAddress());
                    System.out.println("Is reachable: " + state);
                    System.out.println(checkARPTable(inetAddress.getHostAddress()));
                } catch (IOException e) {
                    System.out.println("Exception in: " + anotherApi);
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                System.out.println("Exception in: " + anotherApi);
                e.printStackTrace();
            }
        }
    }

    private static String createIP(long outerAddress) {
        short[] buf = new short[4];
        short check = 255;
        buf[3] = (short) (outerAddress & check);
        buf[2] = (short) ((outerAddress >> 8) & check);
        buf[1] = (short) ((outerAddress >> 16) & check);
        buf[0] = (short) ((outerAddress >> 24) & check);
        return buf[0] + "." + buf[1] + "." + buf[2] + "." + buf[3];
    }

    private static String checkARPTable(String ip) throws IOException {
        String systemInput = takeARP(ip);
        String mac = "";
        Pattern pattern = Pattern.compile("\\s*([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
        Matcher matcher = pattern.matcher(systemInput);
        if (matcher.find()) {
            mac = mac + matcher.group().replaceAll("\\s", "");
        } else {
            System.out.println("No string found");
        }
        return mac;
    }

    private static String takeARP(String ip) throws IOException {
        String systemInput;
        Scanner s = new Scanner(Runtime.getRuntime().exec("arp -a " + ip).getInputStream()).useDelimiter("\\A");
        systemInput = s.next();
        return systemInput;
    }
}