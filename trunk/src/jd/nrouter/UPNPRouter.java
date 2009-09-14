//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nrouter;

import java.net.InetAddress;
import java.util.Iterator;

import jd.controlling.JDLogger;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.control.ActionRequest;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

public class UPNPRouter extends Router {
    private SSDPPacket ssdpPacket;
    private int timeout = 10000;
    private static final String ROUTER_DEVICE = "urn:schemas-upnp-org:device:InternetGatewayDevice:1";
    private static final String WAN_DEVICE = "urn:schemas-upnp-org:device:WANDevice:1";
    private static final String WANCON_DEVICE = "urn:schemas-upnp-org:device:WANConnectionDevice:1";
    private static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:WANIPConnection:1";

    private ControlPoint controlPoint = null;
    protected Device device;
    private Device wanConnectionDevice;

    /**
     * 
     * @param address
     *            the routers IP
     */
    public UPNPRouter(InetAddress address) {
        super(address);

        discover();
    }
public String toString(){
    return device.getFriendlyName()+" @ "+device.getInterfaceAddress();
}
    public String getRefreshIPRequest() {
        Device wcd = getWanConnectionDevice();
        Action action = wcd.getAction("ForceTermination"); 
        ArgumentList actionInputArgList = action.getInputArgumentList();
        ActionRequest ctrlReq = new ActionRequest();
        ctrlReq.setRequest(action, actionInputArgList);
        return ctrlReq.toString();
    }

    public boolean refreshIP() {
        Device wcd = getWanConnectionDevice();
        Action action = wcd.getAction("ForceTermination");
        if (action.postControlAction()) {
            ArgumentList outArgList = action.getOutputArgumentList();
            int nOutArgs = outArgList.size();
            for (int n = 0; n < nOutArgs; n++) {
                Argument outArg = outArgList.getArgument(n);
                System.out.println(outArg.getName() + ": " + outArg.getValue());

            }
            return true;
        } else {
            return super.refreshIP();
        }
    }

    public synchronized void discover() {
        if (controlPoint != null) return;

        controlPoint = new ControlPoint();

        final Threader th = new Threader();

        controlPoint.addSearchResponseListener(new SearchResponseListener() {

            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
                InetAddress ia = ssdpPacket.getRemoteInetAddress();
                JDLogger.getLogger().info("Received foreign package: " + ssdpPacket);
                if (ia.getHostAddress().equals(getAddress().getHostAddress())) {
                    UPNPRouter.this.ssdpPacket = ssdpPacket;
                    controlPoint.stop();

                    for (Iterator iter = controlPoint.getDeviceList().iterator(); iter.hasNext();) {
                        Device current = (Device) iter.next();
                        System.out.println("Device: " + current.getFriendlyName() + current.getDeviceType());
                        if (!current.getDeviceType().equals(ROUTER_DEVICE)) continue;

                        device = current;
                        break;
                    }
                    th.interrupt();
                }
            }
        });

        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                }
                controlPoint.stop();
                th.interrupt();

            }
        });
        controlPoint.start();
        th.startAndWait();

    }

    public boolean isUPNPDevice() {
        discover();
        return device != null;

    }

    public String getExternalIPAddress() {
        Device wcd = getWanConnectionDevice();
        Action action = wcd.getAction("GetExternalIPAddress");

        if (action.postControlAction()) {
            ArgumentList outArgList = action.getOutputArgumentList();
            int nOutArgs = outArgList.size();
            for (int n = 0; n < nOutArgs; n++) {
                Argument outArg = outArgList.getArgument(n);
                return outArg.getValue();

            }
            return super.getExternalIPAddress();
        } else {
            return super.getExternalIPAddress();
        }

    }

    /**
     * Terminates the WanConnectiondevice of this upnp router
     * 
     * @return
     */
    public Device getWanConnectionDevice() {
        discover();
        if (wanConnectionDevice != null) return wanConnectionDevice;
        for (Iterator iter = device.getDeviceList().iterator(); iter.hasNext();) {
            Device current = (Device) iter.next();
            System.out.println("Device: " + current.getFriendlyName() + current.getDeviceType());
            if (!current.getDeviceType().equals(WAN_DEVICE)) continue;

            DeviceList l = current.getDeviceList();

            for (int i = 0; i < current.getDeviceList().size(); i++) {
                Device current2 = l.getDevice(i);
                System.out.println("Device: " + current2.getFriendlyName() + current2.getDeviceType());
                if (!current2.getDeviceType().equals(WANCON_DEVICE)) continue;
                wanConnectionDevice = current;
                return current;
            }
        }
        return null;
    }

}
